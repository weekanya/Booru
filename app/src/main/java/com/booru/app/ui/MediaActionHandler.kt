package com.booru.app.ui

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import coil3.toBitmap
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Scale
import com.booru.app.RemoteMedia
import com.booru.app.data.BooruCacheManager
import com.booru.app.data.ImageQuality
import com.booru.app.data.OfflineStore
import com.booru.app.data.network.BooruHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

import com.booru.app.data.network.NetworkClient

object MediaActionHandler {

    private const val MAX_DOWNLOAD_BYTES = 200L * 1024L * 1024L

    private val httpClient = NetworkClient.baseClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()

    suspend fun downloadMedia(
        context: Context,
        media: RemoteMedia,
        quality: ImageQuality
    ): Result<String> = withContext(Dispatchers.IO) {
        executeDownload(context, media, quality)
    }

    private fun sanitizeFilenamePart(value: String): String {
        return value.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40).ifBlank { "unknown" }
    }

    private fun executeDownload(
        context: Context,
        media: RemoteMedia,
        quality: ImageQuality
    ): Result<String> {
        val downloadUrl = when (quality) {
            ImageQuality.ORIGINAL -> media.url.ifBlank { media.sample.ifBlank { media.preview } }
            ImageQuality.SAMPLE   -> media.sample.ifBlank { media.url.ifBlank { media.preview } }
            ImageQuality.SAVER    -> media.preview.ifBlank { media.sample.ifBlank { media.url } }
        }
        if (downloadUrl.isBlank()) {
            return Result.failure(IOException("Empty media URL"))
        }

        localCopy(context, downloadUrl)?.let { return saveLocalFile(context, media, it) }

        val remote = try {
            saveRemote(context, media, downloadUrl)
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Result.failure(e)
        }
        if (remote.isSuccess) return remote

        val fallback = listOf(media.url, media.sample)
            .filter { it.isNotBlank() && it != downloadUrl }
            .firstNotNullOfOrNull { localCopy(context, it) }
        return if (fallback != null) saveLocalFile(context, media, fallback) else remote
    }

    private fun localCopy(context: Context, url: String): File? =
        BooruCacheManager.getFavoriteFileForUrl(context, url) ?: OfflineStore.fileForUrl(url)

    private fun saveLocalFile(context: Context, media: RemoteMedia, file: File): Result<String> = try {
        val ext = file.name.substringAfterLast('.', "").lowercase().ifBlank { "jpg" }
        file.inputStream().use { input -> writeToMediaStore(context, media, input, ext) }
    } catch (c: kotlinx.coroutines.CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun saveRemote(context: Context, media: RemoteMedia, downloadUrl: String): Result<String> {
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", BooruHeaders.BROWSER_USER_AGENT)
            .header("Referer", BooruHeaders.refererFor(downloadUrl))
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP error ${response.code}"))
            }
            val body = response.body ?: return Result.failure(IOException("Empty response body"))
            val contentLength = body.contentLength()
            if (contentLength > MAX_DOWNLOAD_BYTES) {
                return Result.failure(IOException("Download Content-Length $contentLength exceeds limit of $MAX_DOWNLOAD_BYTES"))
            }
            val mimeFromHeader = body.contentType()?.toString()?.lowercase() ?: ""
            val resolvedExt = when {
                mimeFromHeader.contains("mp4") -> "mp4"
                mimeFromHeader.contains("webm") -> "webm"
                mimeFromHeader.contains("quicktime") -> "mov"
                mimeFromHeader.contains("matroska") -> "mkv"
                mimeFromHeader.contains("gif") -> "gif"
                mimeFromHeader.contains("png") -> "png"
                mimeFromHeader.contains("webp") -> "webp"
                mimeFromHeader.contains("jpeg") || mimeFromHeader.contains("jpg") -> "jpg"
                else -> extensionFromUrl(downloadUrl)
            }
            body.byteStream().use { input -> writeToMediaStore(context, media, input, resolvedExt) }
        }
    }

    private fun extensionFromUrl(url: String): String {
        val last = url.substringBefore("?").substringBefore("#").substringAfterLast("/")
        val ext = last.substringAfterLast(".", "").lowercase()
        return if (ext.matches(Regex("[a-z0-9]{1,5}"))) ext else "jpg"
    }

    private fun writeToMediaStore(
        context: Context,
        media: RemoteMedia,
        input: InputStream,
        ext: String
    ): Result<String> {
        var insertedUri: android.net.Uri? = null
        var isSuccess = false
        return try {
            val isVideoMedia = media.isVideo || ext in listOf("mp4", "webm", "mov", "mkv")
            val mimeType = when (ext) {
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mov" -> "video/quicktime"
                "mkv" -> "video/x-matroska"
                "gif" -> "image/gif"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "jpg", "jpeg" -> "image/jpeg"
                else -> if (isVideoMedia) "video/mp4" else "image/jpeg"
            }
            val safeSource = sanitizeFilenamePart(media.source.lowercase().trim())
            val safeId = sanitizeFilenamePart(media.id.trim())
            val filename = "booru_${safeSource}_${safeId}_${System.currentTimeMillis()}.$ext"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isVideoMedia) "${Environment.DIRECTORY_MOVIES}/Booru" else "${Environment.DIRECTORY_PICTURES}/Booru"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = if (isVideoMedia) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val uri = context.contentResolver.insert(collection, contentValues)
                ?: throw IOException("MediaStore insert failed")
            insertedUri = uri

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                    if (totalRead > MAX_DOWNLOAD_BYTES) {
                        throw IOException("Download exceeded maximum allowed size")
                    }
                    outStream.write(buffer, 0, bytesRead)
                }
            } ?: throw IOException("Could not open stream for saving")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            isSuccess = true
            Result.success(filename)
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (!isSuccess) {
                insertedUri?.let { uri ->
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    suspend fun applyWallpaper(
        context: Context,
        target: Int,
        media: RemoteMedia
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val outcome = withTimeoutOrNull(45_000L) {
            executeApplyWallpaper(context, target, media)
        }
        outcome ?: Result.failure(IOException("Setting wallpaper timed out"))
    }

    private suspend fun executeApplyWallpaper(
        context: Context,
        target: Int,
        media: RemoteMedia
    ): Result<Unit> {
        return try {
            val displayMetrics = context.resources.displayMetrics
            val maxDim = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels).coerceAtLeast(1080)

            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(media.url.ifBlank { media.sample.ifBlank { media.preview } })
                .size(maxDim, maxDim)
                .scale(Scale.FIT)
                .allowHardware(false)
                .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                return Result.failure(IOException("Failed to load image for wallpaper"))
            }

            val bitmap: Bitmap = runCatching { result.image.toBitmap() }.getOrNull()
                ?: return Result.failure(IOException("Could not decode bitmap"))

            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                1 -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
                2 -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
                3 -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                }
                else -> return Result.failure(IllegalArgumentException("Unknown wallpaper target: $target"))
            }

            Result.success(Unit)
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
