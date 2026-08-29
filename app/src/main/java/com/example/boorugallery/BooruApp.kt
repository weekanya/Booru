package com.example.boorugallery

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

class BooruApplication : Application(), ImageLoaderFactory {

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_COMPLETE || level >= TRIM_MEMORY_UI_HIDDEN) {
            cleanAppCache()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val url = originalRequest.url.toString()

                val referer = when {
                    url.contains("gelbooru.com") -> "https://gelbooru.com/"
                    url.contains("rule34.xxx") -> "https://rule34.xxx/"
                    url.contains("xbooru.com") -> "https://xbooru.com/"
                    url.contains("tbib.org") -> "https://tbib.org/"
                    url.contains("safebooru.org") -> "https://safebooru.org/"
                    url.contains("yande.re") -> "https://yande.re/"
                    url.contains("konachan") -> "https://konachan.net/"
                    else -> "${originalRequest.url.scheme}://${originalRequest.url.host}/"
                }

                val userAgent = when {
                    url.contains("donmai.us") -> "BooruGallery/1.0 (by weekanya)"
                    else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                }

                val newRequest = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Referer", referer)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()

                chain.proceed(newRequest)
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

    fun cleanAppCache() {
        Thread {
            runCatching {
                cacheDir.deleteRecursively()
            }
        }.start()
    }
}
