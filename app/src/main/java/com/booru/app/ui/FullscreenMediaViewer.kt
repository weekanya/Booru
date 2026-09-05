package com.booru.app.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import coil.Coil
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMediaViewer(
    initialIndex: Int,
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

    val activity = context as? Activity
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val prevLightStatus = controller.isAppearanceLightStatusBars
            val prevLightNav = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            onDispose {
                controller.isAppearanceLightStatusBars = prevLightStatus
                controller.isAppearanceLightNavigationBars = prevLightNav
            }
        } else {
            onDispose {}
        }
    }

    val safeInitial = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { mediaList.size }
    )

    var showControls by remember { mutableStateOf(true) }
    var showTagsSheet by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()
    val isFav = vm.isFavorite(currentMedia)

    BackHandler {
        if (showWallpaperDialog) {
            showWallpaperDialog = false
        } else if (showTagsSheet) {
            showTagsSheet = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(pagerState.currentPage, mediaList.size) {
        isCurrentPageZoomed = false
        if (onLoadMore != null && pagerState.currentPage >= mediaList.size - 4) {
            onLoadMore()
        }
    }

    fun downloadCurrentMedia() {
        if (isDownloading) return
        val media = currentMedia
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

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val req = okhttp3.Request.Builder()
                        .url(rawUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", rawUrl.substringBeforeLast("/") + "/")
                        .build()

                    val response = client.newCall(req).execute()
                    if (!response.isSuccessful || response.body == null) {
                        throw IOException("HTTP ${response.code}")
                    }

                    response.use { resp ->
                        val inputStream = resp.body!!.byteStream()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val contentValues = ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                put(
                                    android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                    if (media.isVideo) "${Environment.DIRECTORY_MOVIES}/Booru" else "${Environment.DIRECTORY_PICTURES}/Booru"
                                )
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                            val collection = if (media.isVideo) {
                                android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            } else {
                                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            }
                            val uri = context.contentResolver.insert(collection, contentValues)
                                ?: throw IOException("Insert failed")

                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                inputStream.copyTo(out)
                            }
                            contentValues.clear()
                            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                            context.contentResolver.update(uri, contentValues, null, null)
                        } else {
                            val targetDir = File(
                                Environment.getExternalStoragePublicDirectory(
                                    if (media.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                                ),
                                "Booru"
                            ).apply { mkdirs() }

                            val targetFile = File(targetDir, filename)
                            targetFile.outputStream().use { out ->
                                inputStream.copyTo(out)
                            }
                            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        Toast.makeText(context, Strings.downloadComplete(filename, lang), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        Toast.makeText(context, "${Strings.downloadFailed(lang)}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun applyWallpaper(flag: Int) {
        val media = currentMedia
        showWallpaperDialog = false
        coroutineScope.launch {
            Toast.makeText(context, Strings.settingWallpaper(lang), Toast.LENGTH_SHORT).show()
            withContext(Dispatchers.IO) {
                try {
                    val rawUrl = media.url.ifBlank { media.sample.ifBlank { media.preview } }
                    val loader = Coil.imageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(rawUrl)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    val bitmap = result?.toBitmap()

                    if (bitmap != null) {
                        val wm = WallpaperManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val which = when (flag) {
                                1 -> WallpaperManager.FLAG_SYSTEM
                                2 -> WallpaperManager.FLAG_LOCK
                                else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            }
                            wm.setBitmap(bitmap, null, true, which)
                        } else {
                            wm.setBitmap(bitmap)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, Strings.wallpaperSuccess(lang), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${Strings.wallpaperFailed(lang)}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
            val pageMedia = mediaList[page]
            if (pageMedia.isVideo) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BooruVideoPlayer(
                        videoUrl = pageMedia.url,
                        previewUrl = pageMedia.sample.ifBlank { pageMedia.preview },
                        modifier = Modifier.fillMaxSize(),
                        isActive = (pagerState.currentPage == page)
                    )
                }
            } else {
                FullscreenZoomableImage(
                    media = pageMedia,
                    vm = vm,
                    isActive = (pagerState.currentPage == page),
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isCurrentPageZoomed = zoomed
                        }
                    },
                    onToggleControls = { showControls = !showControls }
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(140, easing = FastOutLinearInEasing)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.88f),
                            0.5f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Transparent
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = currentMedia.source.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = when (currentMedia.rating.lowercase()) {
                                        "e", "explicit" -> Color(0xFFE53935)
                                        "q", "questionable" -> Color(0xFFFFA000)
                                        else -> Color(0xFF43A047)
                                    }
                                ) {
                                    Text(
                                        text = when (currentMedia.rating.lowercase()) {
                                            "e", "explicit" -> "18+"
                                            "q", "questionable" -> "Q"
                                            else -> "SAFE"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, currentMedia.url.ifBlank { currentMedia.sample })
                                }
                                context.startActivity(Intent.createChooser(shareIntent, Strings.share(lang)))
                            },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        Surface(
                            onClick = {
                                val browserUrl = currentMedia.url.ifBlank { currentMedia.sample }
                                if (browserUrl.isNotBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
                                }
                            },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open in browser", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(140, easing = FastOutLinearInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.4f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.90f)
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { vm.toggleFavorite(currentMedia) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFEF5350) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { downloadCurrentMedia() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.FileDownload,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (!currentMedia.isVideo) {
                            IconButton(
                                onClick = { showWallpaperDialog = true },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    contentDescription = "Wallpaper",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { showTagsSheet = true },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Sell,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${Strings.tags(lang)} (${currentMedia.tagList.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.Wallpaper,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(Strings.setWallpaperTitle(lang), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { applyWallpaper(1) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.wallpaperHomeScreen(lang))
                    }
                    TextButton(
                        onClick = { applyWallpaper(2) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.wallpaperLockScreen(lang))
                    }
                    TextButton(
                        onClick = { applyWallpaper(3) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.wallpaperBoth(lang))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text(Strings.cancelBtn(lang))
                }
            }
        )
    }

    if (showTagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTagsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Strings.infoAndTags(lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Strings.source(lang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentMedia.source, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (currentMedia.id.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ID", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentMedia.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (currentMedia.width > 0 && currentMedia.height > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(Strings.resolution(lang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentMedia.width} × ${currentMedia.height}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Strings.scoreLabel(currentMedia.score, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = Strings.tagsLabel(currentMedia.tagList.size, lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentMedia.tagList.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    showTagsSheet = false
                                    onDismiss()
                                    onNavigateToExplore?.invoke()
                                    vm.search(vm.source, tag, vm.safeMode)
                                },
                                onLongClick = {
                                    vm.addBlacklistedTag(tag)
                                    Toast.makeText(context, Strings.tagAddedToBlacklist(tag, lang), Toast.LENGTH_SHORT).show()
                                }
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenZoomableImage(
    media: RemoteMedia,
    vm: GalleryViewModel,
    isActive: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isActive) {
        if (!isActive && rawScale > 1f) {
            rawScale = 1f
            rawOffset = Offset.Zero
            onZoomChanged(false)
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fsZoomScale"
    )
    val animatedOffset by androidx.compose.animation.core.animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fsZoomOffset"
    )

    var hasLoadError by remember(media.id, media.url) { mutableStateOf(false) }
    val displayUrl = if (hasLoadError) {
        media.sample.ifBlank { media.preview.ifBlank { media.url } }
    } else {
        vm.resolveMediaUrl(media)
    }

    val isZoomed = rawScale > 1.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (rawScale > 1.05f) {
                            rawScale = 1f
                            rawOffset = Offset.Zero
                            onZoomChanged(false)
                        } else {
                            rawScale = 2.5f
                            onZoomChanged(true)
                        }
                    }
                )
            }
            .then(
                if (isZoomed) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (rawScale * zoom).coerceIn(1f, 4.5f)
                            rawScale = newScale
                            if (newScale > 1.05f) {
                                val maxOffsetX = (newScale - 1f) * 600f
                                val maxOffsetY = (newScale - 1f) * 800f
                                val newOffset = rawOffset + pan
                                rawOffset = Offset(
                                    x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                onZoomChanged(true)
                            } else {
                                rawScale = 1f
                                rawOffset = Offset.Zero
                                onZoomChanged(false)
                            }
                        }
                    }
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.size >= 2) {
                                    val zoom = event.calculateZoom()
                                    if (zoom > 1.05f) {
                                        rawScale = (rawScale * zoom).coerceIn(1f, 4.5f)
                                        onZoomChanged(rawScale > 1.05f)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(displayUrl)
                .crossfade(180)
                .allowHardware(!media.isGif)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = animatedOffset.x
                    translationY = animatedOffset.y
                },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.preview.isNotBlank() && displayUrl != media.preview) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(media.preview)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            error = {
                if (!hasLoadError && displayUrl != media.sample && media.sample.isNotBlank()) {
                    hasLoadError = true
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Strings.errorLoading(vm.language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        )
    }
}
