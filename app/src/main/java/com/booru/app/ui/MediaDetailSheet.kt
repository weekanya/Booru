package com.booru.app.ui

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import kotlin.math.abs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

@Composable
fun MediaDetailSheet(
    media: RemoteMedia,
    vm: GalleryViewModel,
    onDismiss: () -> Unit,
    onNavigateToExplore: (() -> Unit)? = null
) {
    MediaDetailSheet(
        initialIndex = 0,
        mediaList = listOf(media),
        vm = vm,
        onDismiss = onDismiss,
        onLoadMore = null,
        onNavigateToExplore = onNavigateToExplore
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaDetailSheet(
    initialIndex: Int = 0,
    mediaList: List<RemoteMedia>,
    vm: GalleryViewModel,
    onDismiss: () -> Unit,
    onLoadMore: (() -> Unit)? = null,
    onNavigateToExplore: (() -> Unit)? = null
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val context = LocalContext.current
    val lang = vm.language
    val coroutineScope = rememberCoroutineScope()

    val safeInitial = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { mediaList.size }
    )

    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var resetZoomKey by remember { mutableIntStateOf(0) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var selectedTagForAction by remember { mutableStateOf<String?>(null) }
    var isSettingWallpaper by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()

    BackHandler {
        when {
            selectedTagForAction != null -> selectedTagForAction = null
            showWallpaperDialog -> showWallpaperDialog = false
            isCurrentPageZoomed -> {
                resetZoomKey++
                isCurrentPageZoomed = false
            }
            else -> onDismiss()
        }
    }

    LaunchedEffect(pagerState.currentPage, mediaList.size) {
        isCurrentPageZoomed = false
        if (onLoadMore != null && pagerState.currentPage >= mediaList.size - 4) {
            onLoadMore()
        }
    }

    fun downloadCurrentMedia(media: RemoteMedia) {
        if (isDownloading) return
        coroutineScope.launch {
            isDownloading = true
            Toast.makeText(context, Strings.loadingOriginal(lang), Toast.LENGTH_SHORT).show()

            withContext(Dispatchers.IO) {
                try {
                    val rawUrl = media.url.ifBlank { media.sample.ifBlank { media.preview } }
                    if (rawUrl.isBlank()) throw IOException("URL is empty")

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

                    val candidates = LinkedHashSet<String>()
                    val base = rawUrl.substringBeforeLast(".")
                    candidates.add(rawUrl)
                    candidates.add("$base.jpeg")
                    candidates.add("$base.jpg")
                    candidates.add("$base.png")
                    candidates.add("$base.mp4")
                    candidates.add("$base.webm")
                    candidates.add("$base.gif")

                    if (media.sample.isNotBlank() && media.sample != rawUrl) {
                        candidates.add(media.sample)
                    }
                    if (media.preview.isNotBlank() && media.preview != rawUrl) {
                        candidates.add(media.preview)
                    }
                    if (rawUrl.contains("xbooru.com") && !rawUrl.contains("?")) {
                        candidates.add("$rawUrl?1")
                    }

                    var successfulResp: okhttp3.Response? = null
                    var successfulCandUrl = rawUrl
                    for (cand in candidates) {
                        val altReq = okhttp3.Request.Builder()
                            .url(cand)
                            .header("User-Agent", userAgent)
                            .header("Referer", referer)
                            .header("Accept", "*/*")
                            .build()
                        try {
                            val altResp = client.newCall(altReq).execute()
                            if (altResp.isSuccessful && altResp.body != null) {
                                successfulResp = altResp
                                successfulCandUrl = cand
                                break
                            }
                            altResp.close()
                        } catch (_: Exception) {}
                    }

                    val response = successfulResp ?: throw IOException("HTTP download failed")
                    var insertedUri: Uri? = null
                    var isSuccess = false
                    var downloadedFilename = ""
                    try {
                        response.use { resp ->
                            val body = resp.body ?: throw IOException("Empty response body")
                            val headerContentType = resp.header("Content-Type")?.substringBefore(";")?.trim()?.lowercase()

                            val resolvedExt = when {
                                headerContentType == "image/png" -> "png"
                                headerContentType == "image/gif" -> "gif"
                                headerContentType == "image/webp" -> "webp"
                                headerContentType == "video/mp4" -> "mp4"
                                headerContentType == "video/webm" -> "webm"
                                headerContentType == "image/jpeg" -> "jpg"
                                else -> {
                                    successfulCandUrl.substringAfterLast(".").substringBefore("?").ifBlank {
                                        if (media.isVideo) "mp4" else if (media.isGif) "gif" else "jpg"
                                    }
                                }
                            }
                            val mimeType = when (resolvedExt.lowercase()) {
                                "mp4" -> "video/mp4"
                                "webm" -> "video/webm"
                                "gif" -> "image/gif"
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                else -> "image/jpeg"
                            }
                            val isVideoMedia = media.isVideo || resolvedExt in listOf("mp4", "webm")
                            val filename = "booru_${media.source.lowercase()}_${media.id}_${System.currentTimeMillis()}.$resolvedExt"
                            downloadedFilename = filename

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
                                    inputStream.copyTo(outStream)
                                } ?: throw IOException("Could not open stream for saving")

                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                context.contentResolver.update(uri, contentValues, null, null)
                                isSuccess = true
                            } else {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    throw IOException("Storage permission required")
                                }

                                val targetDir = File(
                                    Environment.getExternalStoragePublicDirectory(
                                        if (isVideoMedia) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
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
                                isSuccess = true
                            }
                        }
                    } finally {
                        if (!isSuccess) {
                            insertedUri?.let { uri ->
                                try {
                                    context.contentResolver.delete(uri, null, null)
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${Strings.downloadSuccess(lang)}: $downloadedFilename",
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

    fun applyWallpaper(target: Int, media: RemoteMedia) {
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
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: runCatching { result.drawable.toBitmap() }.getOrNull()
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
        modifier = Modifier.statusBarsPadding(),
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
                    .heightIn(min = 360.dp, max = 560.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isCurrentPageZoomed,
                    key = { page ->
                        val m = mediaList.getOrNull(page)
                        if (m != null) "${m.source}_${m.id.ifBlank { m.url }}_$page" else page
                    }
                ) { page ->
                    val item = mediaList[page]
                    if (item.isVideo) {
                        BooruVideoPlayer(
                            videoUrl = item.url,
                            previewUrl = item.sample.ifBlank { item.preview.ifBlank { item.url } },
                            modifier = Modifier.fillMaxSize(),
                            isActive = (pagerState.currentPage == page)
                        )
                    } else {
                        DetailZoomableImage(
                            media = item,
                            vm = vm,
                            isActive = (pagerState.currentPage == page),
                            resetZoomKey = if (pagerState.currentPage == page) resetZoomKey else 0,
                            onZoomChanged = { zoomed ->
                                if (pagerState.currentPage == page) {
                                    isCurrentPageZoomed = zoomed
                                }
                            }
                        )
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
                            text = currentMedia.source.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    RatingBadge(currentMedia.rating, lang)

                    if (currentMedia.isGif) {
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
                    } else if (currentMedia.isVideo) {
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

                if (mediaList.size > 1) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

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
                        onClick = { downloadCurrentMedia(currentMedia) },
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
                        val isFav = vm.isFavorite(currentMedia)
                        val favBg by animateColorAsState(
                            targetValue = if (isFav) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            label = "favBg"
                        )
                        val favFg by animateColorAsState(
                            targetValue = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "favFg"
                        )
                        FilledTonalIconButton(
                            onClick = { vm.toggleFavorite(currentMedia) },
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

                        if (!currentMedia.isVideo) {
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
                                    val shareUrl = currentMedia.postWebUrl.ifBlank { currentMedia.url.ifBlank { currentMedia.sample } }
                                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, Strings.share(lang)))
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
                                val browserUrl = currentMedia.postWebUrl.ifBlank { currentMedia.url.ifBlank { currentMedia.sample } }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
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
                                text = Strings.scoreLabel(currentMedia.score, lang),
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

                if (currentMedia.width > 0 && currentMedia.height > 0) {
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
                                    text = "${currentMedia.width}×${currentMedia.height}",
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
                    text = Strings.tagsLabel(currentMedia.tagList.size, lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            OptInFlowDetailTags(
                tags = currentMedia.tagList,
                onTagClick = { tag ->
                    vm.searchTag(tag, currentMedia.source)
                    onDismiss()
                    onNavigateToExplore?.invoke()
                },
                onTagLongClick = { tag ->
                    selectedTagForAction = tag
                }
            )
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Strings.setWallpaperTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { showWallpaperDialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Smartphone,
                        title = Strings.wallpaperHomeScreen(lang),
                        onClick = { applyWallpaper(1, currentMedia) }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Lock,
                        title = Strings.wallpaperLockScreen(lang),
                        onClick = { applyWallpaper(2, currentMedia) }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = Strings.wallpaperBoth(lang),
                        onClick = { applyWallpaper(3, currentMedia) }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (selectedTagForAction != null) {
        val currentActionTag = selectedTagForAction!!
        val isBlacklisted = vm.tagBlacklist.any { it.equals(currentActionTag, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { selectedTagForAction = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = currentActionTag,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { selectedTagForAction = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Tag", currentActionTag))
                                Toast.makeText(context, Strings.tagCopied(lang), Toast.LENGTH_SHORT).show()
                                selectedTagForAction = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Strings.copyTag(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable {
                                if (isBlacklisted) {
                                    vm.removeBlacklistedTag(currentActionTag)
                                    Toast.makeText(context, Strings.tagRemovedFromBlacklist(currentActionTag, lang), Toast.LENGTH_SHORT).show()
                                } else {
                                    vm.addBlacklistedTag(currentActionTag)
                                    Toast.makeText(context, Strings.tagAddedToBlacklist(currentActionTag, lang), Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                selectedTagForAction = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isBlacklisted) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                contentDescription = null,
                                tint = if (isBlacklisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBlacklisted) Strings.removeFromBlacklist(lang) else Strings.addToBlacklist(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBlacklisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun DetailZoomableImage(
    media: RemoteMedia,
    vm: GalleryViewModel,
    isActive: Boolean,
    resetZoomKey: Int = 0,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }
    var detailLoadError by remember(media.id, media.url) { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            rawScale = 1f
            rawOffset = Offset.Zero
            onZoomChanged(false)
        }
    }

    LaunchedEffect(resetZoomKey) {
        if (resetZoomKey > 0) {
            rawScale = 1f
            rawOffset = Offset.Zero
            onZoomChanged(false)
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (rawScale > 1.05f) {
                            rawScale = 1f
                            rawOffset = Offset.Zero
                            onZoomChanged(false)
                        } else {
                            val newScale = 2.5f
                            rawScale = newScale
                            val maxOffsetX = ((newScale - 1f) * size.width.toFloat() / 2f).coerceAtLeast(0f)
                            val maxOffsetY = ((newScale - 1f) * size.height.toFloat() / 2f).coerceAtLeast(0f)
                            val targetX = (size.width.toFloat() / 2f - tapOffset.x) * (newScale - 1f)
                            val targetY = (size.height.toFloat() / 2f - tapOffset.y) * (newScale - 1f)
                            rawOffset = Offset(
                                x = targetX.coerceIn(-maxOffsetX, maxOffsetX),
                                y = targetY.coerceIn(-maxOffsetY, maxOffsetY)
                            )
                            onZoomChanged(true)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        val pointerCount = event.changes.size
                        if (pointerCount >= 2 || rawScale > 1.05f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop || rawScale > 1.05f) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val newScale = (rawScale * zoomChange).coerceIn(1f, 4.5f)
                                rawScale = newScale
                                val isZoomNow = newScale > 1.05f
                                onZoomChanged(isZoomNow)

                                if (isZoomNow) {
                                    val maxOffsetX = ((newScale - 1f) * size.width.toFloat() / 2f).coerceAtLeast(0f)
                                    val maxOffsetY = ((newScale - 1f) * size.height.toFloat() / 2f).coerceAtLeast(0f)
                                    val candidateOffset = rawOffset + panChange
                                    rawOffset = Offset(
                                        x = candidateOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                        y = candidateOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    rawOffset = Offset.Zero
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val detailTargetUrl = if (detailLoadError) {
            media.sample.ifBlank { media.preview.ifBlank { media.url } }
        } else {
            vm.resolveMediaUrl(media)
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(detailTargetUrl)
                .placeholderMemoryCacheKey(media.sample)
                .crossfade(300)
                .allowHardware(!media.isGif)
                .listener(
                    onError = { _, _ ->
                        if (!detailLoadError && detailTargetUrl != media.sample && media.sample.isNotBlank()) {
                            detailLoadError = true
                        }
                    }
                )
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

        if (rawScale > 1.05f) {
            FilledTonalIconButton(
                onClick = {
                    rawScale = 1f
                    rawOffset = Offset.Zero
                    onZoomChanged(false)
                },
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
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

@Composable
private fun WallpaperOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyPress()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
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
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .bouncyPress()
                    .pointerInput(tag) {
                        detectTapGestures(
                            onTap = { onTagClick(tag) },
                            onLongPress = { onTagLongClick(tag) }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
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
    modifier: Modifier = Modifier,
    isActive: Boolean = true
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
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

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

        val httpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        )
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to referer,
                    "Accept" to "*/*"
                )
            )

        val cache = com.booru.app.BooruVideoCache.getCache(context)
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                 3_000,
                 20_000,
                 250,
                 750
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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

    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer, isPlaying, isActive) {
        while (isActive && isPlaying) {
            if (!isSeeking) {
                currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration
                if (dur > 0L) durationMs = dur
            }
            delay(200)
        }
        if (isActive && !isSeeking) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        }
    }

    LaunchedEffect(showControls, isPlaying, isSeeking) {
        if (showControls && isPlaying && !isSeeking) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(isActive, exoPlayer) {
        if (!isActive) {
            exoPlayer.pause()
        } else if (isPlaying) {
            exoPlayer.play()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
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
                    setOnClickListener { showControls = !showControls }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speedOptions = listOf(1f, 1.25f, 1.5f, 2f, 0.5f)
                        FilledTonalIconButton(
                            onClick = {
                                val idx = speedOptions.indexOf(playbackSpeed)
                                val nextIdx = if (idx in 0 until speedOptions.size - 1) idx + 1 else 0
                                playbackSpeed = speedOptions[nextIdx]
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .size(34.dp)
                                .bouncyPress()
                        ) {
                            val text = if (playbackSpeed == 1f) "1x" else if (playbackSpeed == 2f) "2x" else "${playbackSpeed}x"
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
}

private fun formatVideoTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
