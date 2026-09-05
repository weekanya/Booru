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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.buffer
import okio.source

class BooruApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
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
                    url.contains("xbooru.com") -> "https://xbooru.com/"
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
                        url.contains("xbooru.com") ||
                        url.contains("tbib.org") ||
                        url.contains("gelbooru.com") ||
                        url.contains("rule34.xxx")

                if (isBooruDomain && (initialResponse.code == 404 || initialResponse.code == 302 || initialResponse.code == 403)) {
                    initialResponse.close()
                    val candidateUrls = LinkedHashSet<String>()
                    val query = if (url.contains("?")) "?" + url.substringAfter("?") else ""
                    val cleanUrl = url.substringBefore("?")

                    if (url.contains("xbooru.com") && !url.contains("?")) {
                        candidateUrls.add("$url?1")
                    }

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
                        if (cleanUrl.contains("xbooru.com")) {
                            candidateUrls.add("$cleanUrl?1")
                            candidateUrls.add(cleanUrl.replace("img.xbooru.com", "xbooru.com") + query)
                        }
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

                        if (cleanUrl.contains("xbooru.com")) {
                            candidateUrls.add("$cleanUrl?1")
                            candidateUrls.add(cleanUrl.replace("img.xbooru.com", "xbooru.com") + query)
                            candidateUrls.add(cleanUrl.replace("xbooru.com", "img.xbooru.com") + query)
                        }
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

    fun getCache(context: android.content.Context): androidx.media3.datasource.cache.SimpleCache {
        return simpleCache ?: synchronized(lock) {
            simpleCache ?: run {
                val cacheDir = java.io.File(context.applicationContext.cacheDir, "booru_video_cache")
                val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context.applicationContext)
                val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024)
                androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    fun clearVideoCache(context: android.content.Context) {
        synchronized(lock) {
            try {
                simpleCache?.keys?.toList()?.forEach { key ->
                    simpleCache?.removeResource(key)
                }
            } catch (_: Exception) {}
        }
    }
}
