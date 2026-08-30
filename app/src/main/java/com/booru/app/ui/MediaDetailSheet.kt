package com.booru.app.ui

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.AppLanguage
import com.booru.app.data.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    media: RemoteMedia,
    vm: GalleryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = vm.language
    val isFav = vm.isFavorite(media)

    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = Motion.softSpring(),
        label = "zoomScale"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = Motion.softSpring(),
        label = "zoomOffset"
    )

    val coroutineScope = rememberCoroutineScope()
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var isSettingWallpaper by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    fun downloadOriginalFile() {
        if (isDownloading) return
        coroutineScope.launch {
            isDownloading = true
            Toast.makeText(context, Strings.loadingOriginal(lang), Toast.LENGTH_SHORT).show()

            withContext(Dispatchers.IO) {
                try {
                    val rawUrl = media.url.ifBlank { media.sample.ifBlank { media.preview } }
                    if (rawUrl.isBlank()) {
                        throw IOException("URL is empty")
                    }

                    val ext = rawUrl.substringAfterLast(".").substringBefore("?").ifBlank {
                        if (media.isVideo) "mp4" else if (media.isGif) "gif" else "jpg"
                    }
                    val filename = "booru_${media.source.lowercase()}_${media.id}_${System.currentTimeMillis()}.$ext"
                    val mimeType = when (ext.lowercase()) {
                        "mp4" -> "video/mp4"
                        "webm" -> "video/webm"
                        "gif" -> "image/gif"
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }

                    val referer = when {
                        rawUrl.contains("gelbooru.com") -> "https://gelbooru.com/"
                        rawUrl.contains("rule34.xxx") -> "https://rule34.xxx/"
                        rawUrl.contains("realbooru.com") -> "https://realbooru.com/"
                        rawUrl.contains("xbooru.com") -> "https://xbooru.com/"
                        rawUrl.contains("tbib.org") -> "https://tbib.org/"
                        rawUrl.contains("safebooru.org") -> "https://safebooru.org/"
                        rawUrl.contains("yande.re") -> "https://yande.re/"
                        rawUrl.contains("konachan") -> "https://konachan.net/"
                        else -> "https://gelbooru.com/"
                    }

                    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url(rawUrl)
                        .header("User-Agent", userAgent)
                        .header("Referer", referer)
                        .header("Accept", "*/*")
                        .build()

                    var response = client.newCall(request).execute()
                    if (!response.isSuccessful && rawUrl.contains("realbooru.com")) {
                        response.close()
                        val candidates = listOf(
                            rawUrl.substringBeforeLast(".") + ".jpeg",
                            rawUrl.substringBeforeLast(".") + ".jpg",
                            rawUrl.substringBeforeLast(".") + ".png",
                            rawUrl.substringBeforeLast(".") + ".mp4",
                            rawUrl.substringBeforeLast(".") + ".webm"
                        )
                        for (cand in candidates) {
                            if (cand == rawUrl) continue
                            val altReq = okhttp3.Request.Builder()
                                .url(cand)
                                .header("User-Agent", userAgent)
                                .header("Referer", "https://realbooru.com/")
                                .header("Accept", "*/*")
                                .build()
                            val altResp = client.newCall(altReq).execute()
                            if (altResp.isSuccessful) {
                                response = altResp
                                break
                            }
                            altResp.close()
                        }
                    }

                    if (!response.isSuccessful || response.body == null) {
                        throw IOException("HTTP ${response.code}")
                    }

                    val body = response.body!!
                    val inputStream = body.byteStream()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                if (media.isVideo) "${Environment.DIRECTORY_MOVIES}/Booru" else "${Environment.DIRECTORY_PICTURES}/Booru"
                            )
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                        val collection = if (media.isVideo) {
                            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        }

                        val uri = context.contentResolver.insert(collection, contentValues)
                            ?: throw IOException("MediaStore insert failed")

                        context.contentResolver.openOutputStream(uri)?.use { outStream ->
                            inputStream.copyTo(outStream)
                        } ?: throw IOException("Could not open stream for saving")

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                    } else {
                        val targetDir = File(
                            Environment.getExternalStoragePublicDirectory(
                                if (media.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                            ),
                            "Booru"
                        ).apply { mkdirs() }

                        val targetFile = File(targetDir, filename)
                        targetFile.outputStream().use { outStream ->
                            inputStream.copyTo(outStream)
                        }

                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetFile.absolutePath),
                            arrayOf(mimeType),
                            null
                        )
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${Strings.downloadSuccess(lang)}: $filename",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${Strings.downloadFailed(lang)}: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                    }
                }
            }
        }
    }

    fun applyWallpaper(target: Int) {
        coroutineScope.launch {
            isSettingWallpaper = true
            showWallpaperDialog = false
            Toast.makeText(context, Strings.settingWallpaper(lang), Toast.LENGTH_SHORT).show()
            withContext(Dispatchers.IO) {
                try {
                    val imageLoader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(media.url.ifBlank { media.sample.ifBlank { media.preview } })
                        .allowHardware(false)
                        .build()
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val wallpaperManager = WallpaperManager.getInstance(context)
                            when (target) {
                                1 -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                    } else {
                                        wallpaperManager.setBitmap(bitmap)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, Strings.wallpaperSuccess(lang), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                2 -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                    } else {
                                        wallpaperManager.setBitmap(bitmap)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, Strings.wallpaperSuccess(lang), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                3 -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                                    } else {
                                        wallpaperManager.setBitmap(bitmap)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, Strings.wallpaperSuccess(lang), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, Strings.wallpaperFailed(lang), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, Strings.wallpaperFailed(lang), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${Strings.wallpaperFailed(lang)}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isSettingWallpaper = false
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(top = 56.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 4.dp,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            ) {}
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 340.dp, max = 560.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (media.isVideo) {
                    BooruVideoPlayer(
                        videoUrl = media.url,
                        previewUrl = media.sample.ifBlank { media.preview.ifBlank { media.url } },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (rawScale > 1f) {
                                            rawScale = 1f
                                            rawOffset = Offset.Zero
                                        } else {
                                            rawScale = 2.5f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    rawScale = (rawScale * zoom).coerceIn(1f, 4.5f)
                                    if (rawScale > 1f) {
                                        rawOffset += pan
                                    } else {
                                        rawOffset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(media.url.ifBlank { media.sample.ifBlank { media.preview } })
                                .placeholderMemoryCacheKey(media.sample)
                                .crossfade(300)
                                .allowHardware(!media.isGif)
                                .build(),
                            contentDescription = media.tags,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    translationX = animatedOffset.x,
                                    translationY = animatedOffset.y
                                ),
                            contentScale = ContentScale.Fit
                        )

                        if (rawScale > 1f) {
                            FilledTonalIconButton(
                                onClick = {
                                    rawScale = 1f
                                    rawOffset = Offset.Zero
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                                    .size(38.dp)
                                    .bouncyPress()
                            ) {
                                Icon(
                                    Icons.Rounded.ZoomOutMap,
                                    contentDescription = "Reset zoom",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = media.source,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    RatingBadge(media.rating, lang)

                    if (media.isGif) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "GIF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (media.isVideo) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "VIDEO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = { downloadOriginalFile() },
                        enabled = !isDownloading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .bouncyPress()
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Strings.loadingOriginal(lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(Icons.Rounded.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Strings.downloadBtn(lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val favBg by animateColorAsState(
                            targetValue = if (isFav) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            label = "favBg"
                        )
                        val favFg by animateColorAsState(
                            targetValue = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "favFg"
                        )
                        FilledTonalIconButton(
                            onClick = { vm.toggleFavorite(media) },
                            modifier = Modifier
                                .size(44.dp)
                                .bouncyPress(),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = favBg,
                                contentColor = favFg
                            )
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (!media.isVideo) {
                            FilledTonalIconButton(
                                onClick = { showWallpaperDialog = true },
                                enabled = !isSettingWallpaper,
                                modifier = Modifier
                                    .size(44.dp)
                                    .bouncyPress(),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                if (isSettingWallpaper) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Wallpaper,
                                        contentDescription = Strings.setWallpaperTitle(lang),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, media.postWebUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .bouncyPress(),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.postWebUrl)))
                                }.onFailure {
                                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .bouncyPress(),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open in browser",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Star,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = Strings.scoreLabel(media.score, lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (media.width > 0 && media.height > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.AspectRatio,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "${media.width}×${media.height}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Resolution",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Sell,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = Strings.tagsLabel(media.tagList.size, lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Tags", media.tags))
                        Toast.makeText(context, Strings.tagsCopied(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .bouncyPress(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Strings.copyTags(lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OptInFlowDetailTags(
                tags = media.tagList,
                onTagClick = { tag ->
                    vm.searchTag(tag)
                    onDismiss()
                }
            )
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = Strings.setWallpaperTitle(lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Smartphone,
                        title = Strings.wallpaperHomeScreen(lang),
                        onClick = { applyWallpaper(1) }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Lock,
                        title = Strings.wallpaperLockScreen(lang),
                        onClick = { applyWallpaper(2) }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = Strings.wallpaperBoth(lang),
                        onClick = { applyWallpaper(3) }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showWallpaperDialog = false },
                    modifier = Modifier.bouncyPress()
                ) {
                    Text(
                        Strings.cancelBtn(lang),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun WallpaperOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyPress()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowDetailTags(
    tags: List<String>,
    onTagClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { onTagClick(tag) },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null,
                modifier = Modifier.bouncyPress()
            )
        }
    }
}

@Composable
private fun RatingBadge(rating: String, lang: AppLanguage) {
    val (label, bg, fg) = when (rating.lowercase()) {
        "e", "explicit" -> Triple(
            Strings.ratingExplicit(lang),
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.onErrorContainer
        )
        "q", "questionable" -> Triple(
            Strings.ratingQuestionable(lang),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            Strings.ratingSafe(lang),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = CircleShape,
        color = bg,
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun BooruVideoPlayer(
    videoUrl: String,
    previewUrl: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekRatio by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(videoUrl) {
        val referer = when {
            videoUrl.contains("gelbooru.com") -> "https://gelbooru.com/"
            videoUrl.contains("rule34.xxx") -> "https://rule34.xxx/"
            videoUrl.contains("realbooru.com") -> "https://realbooru.com/"
            videoUrl.contains("xbooru.com") -> "https://xbooru.com/"
            videoUrl.contains("tbib.org") -> "https://tbib.org/"
            videoUrl.contains("safebooru.org") -> "https://safebooru.org/"
            videoUrl.contains("yande.re") -> "https://yande.re/"
            videoUrl.contains("konachan") -> "https://konachan.net/"
            else -> "https://gelbooru.com/"
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to referer,
                    "Accept" to "*/*",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
            )

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isReady = true
                            if (duration > 0L) durationMs = duration
                        }
                    }
                    override fun onRenderedFirstFrame() {
                        isReady = true
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
                prepare()
            }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (isActive) {
            if (!isSeeking) {
                currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration
                if (dur > 0L) durationMs = dur
            }
            delay(16)
        }
    }

    LaunchedEffect(showControls, isPlaying, isSeeking) {
        if (showControls && isPlaying && !isSeeking) {
            delay(4000)
            showControls = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { showControls = !showControls },
        contentAlignment = Alignment.Center
    ) {

        if (previewUrl.isNotBlank() && !isReady) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isReady) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(target)
                        currentPosMs = target
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .bouncyPress(),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .bouncyPress(),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        val dur = if (durationMs > 0) durationMs else Long.MAX_VALUE
                        val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                        exoPlayer.seekTo(target)
                        currentPosMs = target
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .bouncyPress(),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        Icons.Rounded.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val sliderPosition = when {
                    isSeeking -> seekRatio
                    durationMs > 0L -> (currentPosMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    else -> 0f
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isSeeking = true
                        seekRatio = it
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        if (durationMs > 0L) {
                            val target = (seekRatio * durationMs).toLong()
                            exoPlayer.seekTo(target)
                            currentPosMs = target
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayPos = if (isSeeking && durationMs > 0L) (seekRatio * durationMs).toLong() else currentPosMs
                    val timeText = "${formatVideoTime(displayPos)} / ${formatVideoTime(durationMs)}"

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )

                    FilledTonalIconButton(
                        onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .bouncyPress()
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
