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
                    val responseBody = favFile.readBytes().toResponseBody(mediaType)
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

                if (url.contains("realbooru.com") && (initialResponse.code == 404 || initialResponse.code == 302)) {
                    initialResponse.close()
                    val candidateUrls = mutableListOf<String>()

                    if (url.contains("/samples/")) {
                        val hash = url.substringAfterLast("/sample_").substringBefore(".")
                        val dir = url.substringBeforeLast("/sample_").replace("/samples/", "/images/")
                        candidateUrls.add("$dir/$hash.jpeg")
                        candidateUrls.add("$dir/$hash.jpg")
                        candidateUrls.add("$dir/$hash.png")
                        candidateUrls.add("$dir/$hash.gif")
                        val thumbDir = url.substringBeforeLast("/sample_").replace("/samples/", "/thumbnails/")
                        candidateUrls.add("$thumbDir/thumbnail_$hash.jpg")
                    } else if (url.contains("/images/")) {
                        val baseWithoutExt = url.substringBeforeLast(".")
                        candidateUrls.add("$baseWithoutExt.jpeg")
                        candidateUrls.add("$baseWithoutExt.jpg")
                        candidateUrls.add("$baseWithoutExt.png")
                        candidateUrls.add("$baseWithoutExt.gif")
                        candidateUrls.add("$baseWithoutExt.mp4")
                        candidateUrls.add("$baseWithoutExt.webm")
                        val hash = url.substringAfterLast("/").substringBefore(".")
                        val sampleDir = url.substringBeforeLast("/").replace("/images/", "/samples/")
                        candidateUrls.add("$sampleDir/sample_$hash.jpg")
                        val thumbDir = url.substringBeforeLast("/").replace("/images/", "/thumbnails/")
                        candidateUrls.add("$thumbDir/thumbnail_$hash.jpg")
                    }

                    for (candidate in candidateUrls) {
                        if (candidate == url) continue
                        val altReq = originalRequest.newBuilder()
                            .url(candidate)
                            .header("User-Agent", userAgent)
                            .header("Referer", "https://realbooru.com/")
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                        val altResp = chain.proceed(altReq)
                        if (altResp.isSuccessful) {
                            return@addInterceptor altResp
                        }
                        altResp.close()
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
                    .maxSizeBytes(500L * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
