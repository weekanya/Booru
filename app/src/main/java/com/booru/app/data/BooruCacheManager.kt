package com.booru.app.data

import android.content.Context
import android.util.Log
import com.booru.app.RemoteMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object BooruCacheManager {

    const val MAX_MEDIA_CACHE_BYTES = 100L * 1024L * 1024L
    private const val TAG = "BooruCacheManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getFavoritesMediaDir(context: Context): File {
        return File(context.filesDir, "favorites_media").apply { mkdirs() }
    }

    private fun urlToHash(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(url.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getFavoriteFileForUrl(context: Context, url: String): File? {
        if (url.isBlank()) return null
        val hash = urlToHash(url)
        val ext = url.substringAfterLast(".").substringBefore("?").ifBlank { "jpg" }
        val file = File(getFavoritesMediaDir(context), "$hash.$ext")
        return if (file.exists() && file.length() > 0) file else null
    }

    suspend fun saveFavoriteMedia(context: Context, media: RemoteMedia) = withContext(Dispatchers.IO) {
        val urlsToCache = if (media.isVideo) {
            listOf(media.preview, media.sample)
        } else {
            listOf(media.preview, media.sample, media.url)
        }.filter { it.isNotBlank() }.distinct()

        for (url in urlsToCache) {
            val hash = urlToHash(url)
            val ext = url.substringAfterLast(".").substringBefore("?").ifBlank { "jpg" }
            val targetFile = File(getFavoritesMediaDir(context), "$hash.$ext")
            if (targetFile.exists() && targetFile.length() > 0) continue

            val tempFile = File(getFavoritesMediaDir(context), "$hash.$ext.tmp")
            try {
                val referer = when {
                    url.contains("gelbooru.com") -> "https://gelbooru.com/"
                    url.contains("rule34.xxx") -> "https://rule34.xxx/"
                    url.contains("realbooru.com") -> "https://realbooru.com/"
                    url.contains("xbooru.com") -> "https://xbooru.com/"
                    url.contains("tbib.org") -> "https://tbib.org/"
                    url.contains("safebooru.org") -> "https://safebooru.org/"
                    url.contains("yande.re") -> "https://yande.re/"
                    url.contains("konachan") -> "https://konachan.net/"
                    else -> "https://gelbooru.com/"
                }

                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Referer", referer)
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful && resp.body != null) {
                        val body = resp.body!!
                        val contentLength = body.contentLength()
                        if (contentLength > MAX_MEDIA_CACHE_BYTES) {
                            throw IOException("Media Content-Length $contentLength exceeds limit of $MAX_MEDIA_CACHE_BYTES")
                        }
                        val input = body.byteStream()
                        tempFile.outputStream().use { out ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                totalBytes += bytesRead
                                if (totalBytes > MAX_MEDIA_CACHE_BYTES) {
                                    throw IOException("Media exceeds max download size limit")
                                }
                                out.write(buffer, 0, bytesRead)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0) {
                            tempFile.renameTo(targetFile)
                        }
                    }
                }
            } catch (c: CancellationException) {
                if (tempFile.exists()) tempFile.delete()
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache media from $url", e)
            } finally {
                if (tempFile.exists() && !targetFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    suspend fun removeFavoriteMedia(
        context: Context,
        media: RemoteMedia,
        remainingFavorites: List<RemoteMedia> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val remainingUrls = remainingFavorites.flatMap {
            listOf(it.preview, it.sample, it.url)
        }.filter { it.isNotBlank() }.toSet()

        val urlsToRemove = listOf(
            media.preview,
            media.sample,
            media.url
        ).filter { it.isNotBlank() && it !in remainingUrls }

        for (url in urlsToRemove) {
            val file = getFavoriteFileForUrl(context, url)
            file?.delete()
        }
    }

    suspend fun pruneOrphanedFavoritesMedia(
        context: Context,
        activeFavorites: List<RemoteMedia>
    ) = withContext(Dispatchers.IO) {
        val validUrls = activeFavorites.flatMap {
            listOf(it.preview, it.sample, it.url)
        }.filter { it.isNotBlank() }.toSet()
        val validHashes = validUrls.map { urlToHash(it) }.toSet()

        val dir = getFavoritesMediaDir(context)
        val files = dir.listFiles() ?: return@withContext
        for (file in files) {
            val name = file.name
            if (name.endsWith(".tmp")) {
                file.delete()
                continue
            }
            val hash = name.substringBefore(".")
            if (hash.isNotBlank() && hash !in validHashes) {
                file.delete()
            }
        }
    }

    fun getBrowsingCacheSizeBytes(context: Context): Long {
        var size = calculateDirSize(context.cacheDir)
        try {
            context.externalCacheDir?.let {
                size += calculateDirSize(it)
            }
        } catch (_: Exception) {}
        return size
    }

    fun getFavoritesStorageSizeBytes(context: Context): Long {
        return calculateDirSize(getFavoritesMediaDir(context))
    }

    fun getCacheSizeBytes(context: Context): Long {
        return getBrowsingCacheSizeBytes(context)
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            else -> String.format(java.util.Locale.US, "%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearBrowsingCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
            coil.Coil.imageLoader(context).diskCache?.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear Coil image cache", e)
        }

        try {
            com.booru.app.BooruVideoCache.clearVideoCache(context)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear video cache", e)
        }

        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name != "image_cache" && file.name != "booru_video_cache") {
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear internal cacheDir", e)
        }

        try {
            context.externalCacheDir?.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear externalCacheDir", e)
        }
    }
}
