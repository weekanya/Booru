package com.booru.app.data

import android.content.Context
import com.booru.app.RemoteMedia
import com.booru.app.data.db.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object BooruCacheManager {

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
        val urlsToCache = listOf(
            media.preview,
            media.sample,
            media.url
        ).filter { it.isNotBlank() }.distinct()

        for (url in urlsToCache) {
            try {
                val hash = urlToHash(url)
                val ext = url.substringAfterLast(".").substringBefore("?").ifBlank { "jpg" }
                val targetFile = File(getFavoritesMediaDir(context), "$hash.$ext")
                if (targetFile.exists() && targetFile.length() > 0) continue

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
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", referer)
                    .build()

                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful && resp.body != null) {
                    val tempFile = File(getFavoritesMediaDir(context), "$hash.$ext.tmp")
                    tempFile.outputStream().use { out ->
                        resp.body!!.byteStream().copyTo(out)
                    }
                    tempFile.renameTo(targetFile)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun removeFavoriteMedia(context: Context, media: RemoteMedia) = withContext(Dispatchers.IO) {
        val urlsToRemove = listOf(
            media.preview,
            media.sample,
            media.url
        ).filter { it.isNotBlank() }

        for (url in urlsToRemove) {
            val file = getFavoriteFileForUrl(context, url)
            file?.delete()
        }
    }

    fun getCacheSizeBytes(context: Context): Long {
        return calculateDirSize(context.cacheDir)
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
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            else -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearBrowsingCache(context: Context, favorites: List<RemoteMedia> = emptyList()) = withContext(Dispatchers.IO) {
        // Ensure all favorite media files are safely persisted in filesDir first
        for (fav in favorites) {
            saveFavoriteMedia(context, fav)
        }

        // Clear Coil memory/disk cache and temporary cacheDir files
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
            coil.Coil.imageLoader(context).diskCache?.clear()
        } catch (_: Exception) {}

        try {
            val files = context.cacheDir.listFiles() ?: emptyArray()
            for (file in files) {
                file.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}
