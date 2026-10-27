package com.booru.app.data

import kotlinx.coroutines.ensureActive
import com.booru.app.data.network.await
import android.content.Context
import android.util.Log
import com.booru.app.RemoteMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

import com.booru.app.data.network.BooruHeaders
import com.booru.app.data.network.NetworkClient

object BooruCacheManager {

    const val MAX_MEDIA_CACHE_BYTES = 100L * 1024L * 1024L
    private const val TAG = "BooruCacheManager"
    private val HEX = "0123456789abcdef".toCharArray()
    private val EXTENSION = Regex("[A-Za-z0-9]{1,5}")

    private val httpClient = NetworkClient.baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var favoritesDir: File? = null

    fun getFavoritesMediaDir(context: Context): File {
        favoritesDir?.let { if (it.exists()) return it }
        return File(context.filesDir, "favorites_media").apply { mkdirs() }.also { favoritesDir = it }
    }

    private fun urlToHash(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(url.toByteArray(Charsets.UTF_8))
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[v ushr 4]
            out[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(out)
    }

    private fun extensionOf(url: String): String {
        val ext = url.substringBefore("?").substringBefore("#").substringAfterLast("/").substringAfterLast(".", "")
        return if (EXTENSION.matches(ext)) ext else "jpg"
    }

    fun getFavoriteFileForUrl(context: Context, url: String): File? {
        if (url.isBlank()) return null
        val hash = urlToHash(url)
        val ext = extensionOf(url)
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
            coroutineContext.ensureActive()
            val hash = urlToHash(url)
            val ext = extensionOf(url)
            val targetFile = File(getFavoritesMediaDir(context), "$hash.$ext")
            if (targetFile.exists() && targetFile.length() > 0) continue

            val tempFile = File(getFavoritesMediaDir(context), "$hash.$ext.tmp")
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", BooruHeaders.BROWSER_USER_AGENT)
                    .header("Referer", BooruHeaders.refererFor(url))
                    .build()

                httpClient.newCall(req).await().use { resp ->
                    val body = resp.body
                    if (resp.isSuccessful && body != null) {
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
                                coroutineContext.ensureActive()
                                totalBytes += bytesRead
                                if (totalBytes > MAX_MEDIA_CACHE_BYTES) {
                                    throw IOException("Media exceeds max download size limit")
                                }
                                out.write(buffer, 0, bytesRead)
                            }
                        }
                        coroutineContext.ensureActive()
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

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            else -> String.format(java.util.Locale.US, "%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    private const val TRASH_PREFIX = "cache_trash_"
    private const val UPDATES_DIR = "updates"
    private val KEPT_ON_LAUNCH = setOf(UPDATES_DIR)
    private const val LAUNCH_PREFS = "booru_launch"
    private const val KEY_CLEAR_ON_LAUNCH = "clear_cache_on_launch"
    private const val STALE_UPDATE_MS = 24L * 60 * 60 * 1000

    fun isClearOnLaunchEnabled(context: Context): Boolean =
        context.getSharedPreferences(LAUNCH_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CLEAR_ON_LAUNCH, true)

    fun setClearOnLaunchEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(LAUNCH_PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_CLEAR_ON_LAUNCH, enabled).apply()
    }

    fun moveBrowsingCacheToTrash(context: Context): List<File> {
        val moved = mutableListOf<File>()
        val stamp = System.nanoTime()
        try {
            context.cacheDir.listFiles()?.forEachIndexed { index, file ->
                if (file.name.startsWith(TRASH_PREFIX)) {
                    moved.add(file)
                } else if (file.name !in KEPT_ON_LAUNCH) {
                    val target = File(context.cacheDir, "$TRASH_PREFIX${stamp}_${index}_${file.name}")
                    if (file.renameTo(target)) moved.add(target) else moved.add(file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to move cache to trash", e)
        }
        return moved
    }

    fun leftoverTrash(context: Context): List<File> =
        context.cacheDir.listFiles()?.filter { it.name.startsWith(TRASH_PREFIX) }.orEmpty()

    fun deleteTrash(context: Context, entries: List<File>, clearExternal: Boolean = true) {
        for (entry in entries) {
            try {
                if (entry.isDirectory && entry.name.endsWith("booru_video_cache")) {
                    com.booru.app.BooruVideoCache.deleteCacheDirectory(context, entry)
                } else {
                    entry.deleteRecursively()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete cache entry ${entry.name}", e)
            }
        }
        if (clearExternal) {
            try {
                context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            } catch (_: Exception) {}
        }
    }

    fun pruneStaleUpdates(context: Context, currentVersion: String) {
        val files = File(context.cacheDir, UPDATES_DIR).listFiles() ?: return
        val now = System.currentTimeMillis()
        for (file in files) {
            val version = file.name.removePrefix("booru_").removeSuffix(".tmp").removeSuffix(".apk")
            val stale = file.name.endsWith(".tmp") ||
                now - file.lastModified() > STALE_UPDATE_MS ||
                !UpdateChecker.isNewerVersion(version, currentVersion)
            if (stale) runCatching { file.delete() }
        }
    }
}
