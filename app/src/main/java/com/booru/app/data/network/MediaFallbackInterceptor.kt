package com.booru.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class MediaFallbackInterceptor(private val allowThumbnail: Boolean) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 404) return response

        val candidates = candidatesFor(request.url.toString(), allowThumbnail)
        if (candidates.isEmpty()) return response

        response.close()
        for (candidate in candidates) {
            try {
                val alternative = chain.proceed(request.newBuilder().url(candidate).build())
                if (alternative.isSuccessful) return alternative
                alternative.close()
            } catch (e: IOException) {
                if (chain.call().isCanceled()) throw e
            }
        }
        return response.newBuilder()
            .body(ByteArray(0).toResponseBody(null))
            .build()
    }

    companion object {
        private val FALLBACK_HOSTS = listOf("realbooru.com", "safebooru.org", "tbib.org", "gelbooru.com", "rule34.xxx")
        private val IMAGE_EXTENSIONS = listOf("jpeg", "jpg", "png", "gif", "webp")
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov")

        fun candidatesFor(url: String, allowThumbnail: Boolean): List<String> {
            if (FALLBACK_HOSTS.none { url.contains(it) }) return emptyList()
            val query = if (url.contains("?")) "?" + url.substringAfter("?") else ""
            val cleanUrl = url.substringBefore("?")
            val extension = cleanUrl.substringAfterLast("/").substringAfterLast(".", "").lowercase()
            if (extension in VIDEO_EXTENSIONS) return emptyList()

            val candidates = LinkedHashSet<String>()
            when {
                cleanUrl.contains("/samples/") -> {
                    val filename = cleanUrl.substringAfterLast("/sample_").substringBefore(".")
                    val sampleDir = cleanUrl.substringBeforeLast("/sample_")
                    val imageDir = sampleDir.replace("/samples/", "/images/")
                    IMAGE_EXTENSIONS.forEach { candidates.add("$imageDir/$filename.$it$query") }
                    if (allowThumbnail) {
                        candidates.add("${sampleDir.replace("/samples/", "/thumbnails/")}/thumbnail_$filename.jpg$query")
                    }
                }
                cleanUrl.contains("/images/") -> {
                    val baseWithoutExt = cleanUrl.substringBeforeLast(".")
                    val filename = cleanUrl.substringAfterLast("/").substringBefore(".")
                    val imageDir = cleanUrl.substringBeforeLast("/")
                    IMAGE_EXTENSIONS.forEach { candidates.add("$baseWithoutExt.$it$query") }
                    candidates.add("${imageDir.replace("/images/", "/samples/")}/sample_$filename.jpg$query")
                    if (allowThumbnail) {
                        candidates.add("${imageDir.replace("/images/", "/thumbnails/")}/thumbnail_$filename.jpg$query")
                    }
                }
                cleanUrl.contains("/thumbnails/") -> {
                    val filename = cleanUrl.substringAfterLast("/thumbnail_").substringBefore(".")
                    val thumbDir = cleanUrl.substringBeforeLast("/thumbnail_")
                    listOf("jpg", "jpeg", "png").forEach { candidates.add("$thumbDir/thumbnail_$filename.$it$query") }
                }
            }
            candidates.remove(url)
            return candidates.toList()
        }
    }
}
