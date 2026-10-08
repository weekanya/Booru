package com.booru.app

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
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

import com.booru.app.data.network.NetworkClient

class BooruApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val trash = BooruCacheManager.moveBrowsingCacheToTrash(this)
        CoroutineScope(Dispatchers.IO).launch {
            BooruCacheManager.deleteTrash(this@BooruApplication, trash)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = NetworkClient.baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val url = originalRequest.url.toString()

                val favFile = BooruCacheManager.getFavoriteFileForUrl(this, url)
                if (favFile != null && favFile.exists() && favFile.length() > 0) {
                    val mediaType = (favFile.name.substringAfterLast(".").let { ext ->
                        when (ext.lowercase()) {
                            "png" -> "image/png"
                            "gif" -> "image/gif"
                            "webp" -> "image/webp"
                            "mp4" -> "video/mp4"
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

                val referer = when {
                    url.contains("gelbooru.com") -> "https://gelbooru.com/"
                    url.contains("rule34.xxx") -> "https://rule34.xxx/"
                    url.contains("realbooru.com") -> "https://realbooru.com/"
                    url.contains("tbib.org") -> "https://tbib.org/"
                    url.contains("safebooru.org") -> "https://safebooru.org/"
                    url.contains("yande.re") -> "https://yande.re/"
                    url.contains("konachan") -> "https://konachan.net/"
                    else -> "${originalRequest.url.scheme}://${originalRequest.url.host}/"
                }

                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

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

                if (isBooruDomain && (initialResponse.code == 404 || initialResponse.code == 302 || initialResponse.code == 403)) {
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
                        candidateUrls.add("$baseWithoutExt.mp4$query")
                        candidateUrls.add("$baseWithoutExt.webm$query")
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
                }

                return@addInterceptor initialResponse
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object BooruVideoCache {
    @Volatile
    private var simpleCache: androidx.media3.datasource.cache.SimpleCache? = null
    private val lock = Any()
    private val activePlayers = java.util.concurrent.atomic.AtomicInteger(0)

    fun acquirePlayer() {
        activePlayers.incrementAndGet()
    }

    fun releasePlayer() {
        activePlayers.decrementAndGet()
    }

    fun hasActivePlayers(): Boolean = activePlayers.get() > 0

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
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

    fun refererFor(url: String): String = when {
        url.contains("gelbooru.com") -> "https://gelbooru.com/"
        url.contains("rule34.xxx") -> "https://rule34.xxx/"
        url.contains("realbooru.com") -> "https://realbooru.com/"
        url.contains("tbib.org") -> "https://tbib.org/"
        url.contains("safebooru.org") -> "https://safebooru.org/"
        url.contains("yande.re") -> "https://yande.re/"
        url.contains("konachan") -> "https://konachan.net/"
        else -> "https://gelbooru.com/"
    }

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

    suspend fun prefetchParallel(
        context: android.content.Context,
        url: String,
        onHeadReady: () -> Unit
    ) {
        try {
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
                val head = minOf(FIRST_CHUNK_BYTES, length)
                try {
                    cacheChunk(cache, upstream, url, 0L, head)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                }

                val chunks = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, Long>>()
                var position = head
                while (position < length) {
                    val size = minOf(CHUNK_BYTES, length - position)
                    chunks.add(position to size)
                    position += size
                }
                launch {
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
                                        val (start, size) = current.poll() ?: break
                                        val claimed = try {
                                            cacheChunk(cache, upstream, url, start, size)
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (_: Exception) {
                                            failures++
                                            true
                                        }
                                        if (!claimed) skipped.add(start to size)
                                    }
                                }
                            }
                        }
                        queue = skipped
                    }
                }
                kotlinx.coroutines.delay(40)
                onHeadReady()
            }
        } finally {
            onHeadReady()
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

    fun clearVideoCache(context: android.content.Context) {
        synchronized(lock) {
            if (activePlayers.get() > 0) {
                try {
                    simpleCache?.keys?.toList()?.forEach { key ->
                        try {
                            simpleCache?.removeResource(key)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
                return
            }
            try {
                simpleCache?.keys?.toList()?.forEach { key ->
                    try {
                        simpleCache?.removeResource(key)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            try {
                simpleCache?.release()
            } catch (_: Exception) {}
            simpleCache = null
            try {
                val videoDir = java.io.File(context.applicationContext.cacheDir, "booru_video_cache")
                if (videoDir.exists()) {
                    deleteCacheDirectory(context, videoDir)
                }
            } catch (_: Exception) {}
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
