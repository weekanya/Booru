package com.booru.app.ui

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.booru.app.RemoteMedia
import com.booru.app.data.ImageQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object MediaActionHandler {

    private const val MAX_DOWNLOAD_BYTES = 200L * 1024L * 1024L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun downloadMedia(
        context: Context,
        media: RemoteMedia,
        quality: ImageQuality
    ): Result<String> = withContext(Dispatchers.IO) {
        var insertedUri: android.net.Uri? = null
        var targetPreQFile: File? = null
        var isSuccess = false

        try {
            val downloadUrl = when (quality) {
                ImageQuality.ORIGINAL -> media.url.ifBlank { media.sample.ifBlank { media.preview } }
                ImageQuality.SAMPLE   -> media.sample.ifBlank { media.url.ifBlank { media.preview } }
                ImageQuality.SAVER    -> media.preview.ifBlank { media.sample.ifBlank { media.url } }
            }

            if (downloadUrl.isBlank()) {
                return@withContext Result.failure(IOException("Empty media URL"))
            }

            val referer = when {
                downloadUrl.contains("gelbooru.com") -> "https://gelbooru.com/"
                downloadUrl.contains("rule34.xxx") -> "https://rule34.xxx/"
                downloadUrl.contains("realbooru.com") -> "https://realbooru.com/"
                downloadUrl.contains("xbooru.com") -> "https://xbooru.com/"
                downloadUrl.contains("tbib.org") -> "https://tbib.org/"
                downloadUrl.contains("safebooru.org") -> "https://safebooru.org/"
                downloadUrl.contains("yande.re") -> "https://yande.re/"
                downloadUrl.contains("konachan") -> "https://konachan.net/"
                else -> "https://gelbooru.com/"
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", referer)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP error ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
                val cleanUrl = downloadUrl.substringBefore("?")
                val originalExt = cleanUrl.substringAfterLast(".", "jpg").lowercase()

                val mimeFromHeader = body.contentType()?.toString()?.lowercase() ?: ""
                val resolvedExt = when {
                    mimeFromHeader.contains("mp4") -> "mp4"
                    mimeFromHeader.contains("webm") -> "webm"
                    mimeFromHeader.contains("gif") -> "gif"
                    mimeFromHeader.contains("png") -> "png"
                    mimeFromHeader.contains("webp") -> "webp"
                    mimeFromHeader.contains("jpeg") || mimeFromHeader.contains("jpg") -> "jpg"
                    else -> originalExt
                }

                val mimeType = when (resolvedExt) {
                    "mp4" -> "video/mp4"
                    "webm" -> "video/webm"
                    "gif" -> "image/gif"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }

                val isVideoMedia = media.isVideo || resolvedExt in listOf("mp4", "webm")
                val filename = "booru_${media.source.lowercase().trim()}_${media.id}_${System.currentTimeMillis()}.$resolvedExt"

                val inputStream = body.byteStream()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
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
                } else {
                    val targetDir = File(
                        Environment.getExternalStoragePublicDirectory(
                            if (isVideoMedia) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                        ),
                        "Booru"
                    ).apply { mkdirs() }

                    val targetFile = File(targetDir, filename)
                    targetPreQFile = targetFile
                    targetFile.outputStream().use { outStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            totalRead += bytesRead
                            if (totalRead > MAX_DOWNLOAD_BYTES) {
                                throw IOException("Download exceeded maximum allowed size")
                            }
                            outStream.write(buffer, 0, bytesRead)
                        }
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    isSuccess = true
                }

                Result.success(filename)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (!isSuccess) {
                insertedUri?.let { uri ->
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
                targetPreQFile?.let { f ->
                    runCatching { if (f.exists()) f.delete() }
                }
            }
        }
    }

    suspend fun applyWallpaper(
        context: Context,
        target: Int,
        media: RemoteMedia
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val displayMetrics = context.resources.displayMetrics
            val maxDim = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels).coerceAtLeast(1080)

            val imageLoader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(media.url.ifBlank { media.sample.ifBlank { media.preview } })
                .size(maxDim, maxDim)
                .scale(Scale.FIT)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                return@withContext Result.failure(IOException("Failed to load image for wallpaper"))
            }

            val bitmap: Bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: runCatching { result.drawable.toBitmap() }.getOrNull()
                ?: return@withContext Result.failure(IOException("Could not decode bitmap"))

            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                1 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                3 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                else -> return@withContext Result.failure(IllegalArgumentException("Unknown wallpaper target: $target"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
