package com.booru.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import okio.Path.Companion.toOkioPath
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

import com.booru.app.data.BooruCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.buffer
import okio.source

import com.booru.app.data.network.BooruHeaders
import com.booru.app.data.network.NetworkClient

class BooruApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        com.booru.app.data.TagCategoryCache.init(this)
        com.booru.app.data.OfflineStore.init(this)
        CoroutineScope(Dispatchers.IO).launch { com.booru.app.data.OfflineStore.load() }
        CoroutineScope(Dispatchers.IO).launch { com.booru.app.data.TagCategoryCache.ensureLoaded() }
        val clearOnLaunch = BooruCacheManager.isClearOnLaunchEnabled(this)
        val trash = if (clearOnLaunch) {
            BooruCacheManager.moveBrowsingCacheToTrash(this)
        } else {
            BooruCacheManager.leftoverTrash(this)
        }
        CoroutineScope(Dispatchers.IO).launch {
            BooruCacheManager.deleteTrash(this@BooruApplication, trash, clearExternal = clearOnLaunch)
            val currentVersion = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
            if (currentVersion != null) BooruCacheManager.pruneStaleUpdates(this@BooruApplication, currentVersion)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = NetworkClient.baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val url = originalRequest.url.toString()

                val favFile = BooruCacheManager.getFavoriteFileForUrl(this, url)
                    ?: com.booru.app.data.OfflineStore.fileForUrl(url)
                if (favFile != null && favFile.exists() && favFile.length() > 0) {
                    val mediaType = (favFile.name.substringAfterLast(".").let { ext ->
                        when (ext.lowercase()) {
                            "png" -> "image/png"
                            "gif" -> "image/gif"
                            "webp" -> "image/webp"
                            "mp4" -> "video/mp4"
                            "webm" -> "video/webm"
                            else -> "image/jpeg"
                        }
                    }).toMediaType()
                    val responseBody = favFile.source().buffer().asResponseBody(mediaType, favFile.length())
                    return@addInterceptor Response.Builder()
                        .request(originalRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK (Served from favorites storage)")
                        .body(responseBody)
                        .build()
                }

                val referer = BooruHeaders.refererFor(url)
                val userAgent = BooruHeaders.BROWSER_USER_AGENT

                val newRequest = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Referer", referer)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()

                val initialResponse = chain.proceed(newRequest)
                if (initialResponse.isSuccessful) {
                    return@addInterceptor initialResponse
                }

                val isBooruDomain = url.contains("realbooru.com") ||
                        url.contains("safebooru.org") ||
                        url.contains("tbib.org") ||
                        url.contains("gelbooru.com") ||
                        url.contains("rule34.xxx")

                if (isBooruDomain && initialResponse.code == 404) {
                    initialResponse.close()
                    val candidateUrls = LinkedHashSet<String>()
                    val query = if (url.contains("?")) "?" + url.substringAfter("?") else ""
                    val cleanUrl = url.substringBefore("?")

                    if (cleanUrl.contains("/samples/")) {
                        val filename = cleanUrl.substringAfterLast("/sample_").substringBefore(".")
                        val dir = cleanUrl.substringBeforeLast("/sample_").replace("/samples/", "/images/")
                        val thumbDir = cleanUrl.substringBeforeLast("/sample_").replace("/samples/", "/thumbnails/")

                        candidateUrls.add("$dir/$filename.jpeg$query")
                        candidateUrls.add("$dir/$filename.jpg$query")
                        candidateUrls.add("$dir/$filename.png$query")
                        candidateUrls.add("$dir/$filename.gif$query")
                        candidateUrls.add("$dir/$filename.webp$query")
                        candidateUrls.add("$thumbDir/thumbnail_$filename.jpg$query")
                    } else if (cleanUrl.contains("/images/")) {
                        val baseWithoutExt = cleanUrl.substringBeforeLast(".")
                        val filename = cleanUrl.substringAfterLast("/").substringBefore(".")
                        val sampleDir = cleanUrl.substringBeforeLast("/").replace("/images/", "/samples/")
                        val thumbDir = cleanUrl.substringBeforeLast("/").replace("/images/", "/thumbnails/")

                        candidateUrls.add("$baseWithoutExt.jpeg$query")
                        candidateUrls.add("$baseWithoutExt.jpg$query")
                        candidateUrls.add("$baseWithoutExt.png$query")
                        candidateUrls.add("$baseWithoutExt.gif$query")
                        candidateUrls.add("$baseWithoutExt.webp$query")
                        candidateUrls.add("$sampleDir/sample_$filename.jpg$query")
                        candidateUrls.add("$thumbDir/thumbnail_$filename.jpg$query")
                    } else if (cleanUrl.contains("/thumbnails/")) {
                        val filename = cleanUrl.substringAfterLast("/thumbnail_").substringBefore(".")
                        val thumbDir = cleanUrl.substringBeforeLast("/thumbnail_")
                        candidateUrls.add("$thumbDir/thumbnail_$filename.jpg$query")
                        candidateUrls.add("$thumbDir/thumbnail_$filename.jpeg$query")
                        candidateUrls.add("$thumbDir/thumbnail_$filename.png$query")
                    }

                    for (candidate in candidateUrls) {
                        if (candidate == url) continue
                        val altReq = originalRequest.newBuilder()
                            .url(candidate)
                            .header("User-Agent", userAgent)
                            .header("Referer", referer)
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                        try {
                            val altResp = chain.proceed(altReq)
                            if (altResp.isSuccessful) {
                                return@addInterceptor altResp
                            }
                            altResp.close()
                        } catch (_: Exception) {}
                    }
                    return@addInterceptor initialResponse.newBuilder()
                        .body(ByteArray(0).toResponseBody(null))
                        .build()
                }

                return@addInterceptor initialResponse
            }
            .build()

        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                add(AnimatedImageDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object BooruVideoCache {
    @Volatile
    private var simpleCache: androidx.media3.datasource.cache.SimpleCache? = null
    private val lock = Any()

    private const val USER_AGENT = BooruHeaders.BROWSER_USER_AGENT
    private const val FIRST_CHUNK_BYTES = 256L * 1024
    private const val CHUNK_BYTES = 1024L * 1024
    private const val PREFETCH_WORKERS = 4
    private const val MAX_PREFETCH_BYTES = 200L * 1024 * 1024

    private val writerExecutor: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newCachedThreadPool()

    private val prefetchClient: OkHttpClient by lazy {
        NetworkClient.baseClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun refererFor(url: String): String = BooruHeaders.refererFor(url)

    fun httpDataSourceFactory(url: String, client: OkHttpClient = playerClient): androidx.media3.datasource.okhttp.OkHttpDataSource.Factory =
        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(client)
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(mapOf("Referer" to refererFor(url), "Accept" to "*/*"))

    private val playerClient: OkHttpClient by lazy {
        NetworkClient.baseClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun playerDataSourceFactory(context: android.content.Context, url: String): androidx.media3.datasource.cache.CacheDataSource.Factory =
        androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(httpDataSourceFactory(url))
            .setCacheWriteDataSinkFactory(null)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private fun resolveContentLength(url: String): Long? = runCatching {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", refererFor(url))
            .header("Range", "bytes=0-0")
            .build()
        prefetchClient.newCall(request).execute().use { response ->
            if (response.code != 206) null
            else response.header("Content-Range")?.substringAfter('/', "")?.trim()?.toLongOrNull()
        }
    }.getOrNull()

    private suspend fun runWriter(writer: androidx.media3.datasource.cache.CacheWriter) =
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
            val future = writerExecutor.submit(Runnable {
                val result = runCatching { writer.cache() }
                if (cont.isActive) cont.resumeWith(result)
            })
            cont.invokeOnCancellation {
                writer.cancel()
                future.cancel(true)
            }
        }

    private suspend fun cacheChunk(
        cache: androidx.media3.datasource.cache.SimpleCache,
        upstream: androidx.media3.datasource.okhttp.OkHttpDataSource.Factory,
        url: String,
        start: Long,
        length: Long
    ): Boolean {
        if (cache.isCached(url, start, length)) return true
        val probe = cache.startReadWriteNonBlocking(url, start, length) ?: return false
        if (!probe.isCached) cache.releaseHoleSpan(probe)
        val dataSource = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .createDataSource()
        val spec = androidx.media3.datasource.DataSpec.Builder()
            .setUri(url)
            .setPosition(start)
            .setLength(length)
            .build()
        runWriter(androidx.media3.datasource.cache.CacheWriter(dataSource, spec, null, null))
        return true
    }

    suspend fun prefetchParallel(context: android.content.Context, url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val cache = getCache(context)
            val length = resolveContentLength(url)
            if (length == null || length <= 0L || length > MAX_PREFETCH_BYTES) return@withContext
            val mutations = androidx.media3.datasource.cache.ContentMetadataMutations()
            androidx.media3.datasource.cache.ContentMetadataMutations.setContentLength(mutations, length)
            runCatching { cache.applyContentMetadataMutations(url, mutations) }
            if (cache.isCached(url, 0L, length)) return@withContext

            val upstream = httpDataSourceFactory(url, prefetchClient)
            val chunks = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, Long>>()
            var position = 0L
            var size = FIRST_CHUNK_BYTES
            while (position < length) {
                val len = minOf(size, length - position)
                chunks.add(position to len)
                position += len
                size = CHUNK_BYTES
            }
            var queue = chunks
            repeat(2) {
                if (queue.isEmpty()) return@repeat
                val current = queue
                val skipped = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, Long>>()
                kotlinx.coroutines.coroutineScope {
                    repeat(PREFETCH_WORKERS) {
                        launch {
                            var failures = 0
                            while (failures < 2) {
                                val (start, len) = current.poll() ?: break
                                val claimed = try {
                                    cacheChunk(cache, upstream, url, start, len)
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    failures++
                                    true
                                }
                                if (!claimed) skipped.add(start to len)
                            }
                        }
                    }
                }
                queue = skipped
            }
        }
    }

    fun getCache(context: android.content.Context): androidx.media3.datasource.cache.SimpleCache {
        return simpleCache ?: synchronized(lock) {
            simpleCache ?: run {
                val cacheDir = java.io.File(context.applicationContext.cacheDir, "booru_video_cache")
                val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context.applicationContext)
                val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(512L * 1024 * 1024)
                androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    fun deleteCacheDirectory(context: android.content.Context, dir: java.io.File) {
        try {
            androidx.media3.datasource.cache.SimpleCache.delete(
                dir,
                androidx.media3.database.StandaloneDatabaseProvider(context.applicationContext)
            )
        } catch (_: Exception) {}
        if (dir.exists()) dir.deleteRecursively()
    }
}
