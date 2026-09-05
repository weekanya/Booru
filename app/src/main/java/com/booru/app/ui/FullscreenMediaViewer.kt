package com.booru.app.ui

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.compose.AsyncImage
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
    onLoadMore: (() -> Unit)? = null
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

    var showControls by remember { mutableStateOf(true) }
    var showTagsSheet by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()
    val isFav = vm.isFavorite(currentMedia)

    BackHandler {
        if (showTagsSheet) {
            showTagsSheet = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(pagerState.currentPage, mediaList.size) {
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
                            targetFile.outputStream().use { out -> inputStream.copyTo(out) }
                            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${Strings.downloadSuccess(lang)}: $filename", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${Strings.downloadFailed(lang)}: ${e.message}", Toast.LENGTH_LONG).show()
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
        val media = currentMedia
        coroutineScope.launch {
            showWallpaperDialog = false
            Toast.makeText(context, Strings.settingWallpaper(lang), Toast.LENGTH_SHORT).show()
            withContext(Dispatchers.IO) {
                try {
                    val targetUrl = vm.resolveMediaUrl(media)
                    val request = ImageRequest.Builder(context)
                        .data(targetUrl)
                        .allowHardware(false)
                        .build()
                    val result = Coil.imageLoader(context).execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                            ?: runCatching { result.drawable.toBitmap() }.getOrNull()
                        if (bitmap != null) {
                            val wm = WallpaperManager.getInstance(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                when (target) {
                                    1 -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                    2 -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                    else -> {
                                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                    }
                                }
                            } else {
                                wm.setBitmap(bitmap)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, Strings.wallpaperSuccess(lang), Toast.LENGTH_SHORT).show()
                            }
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                FullscreenZoomableImage(
                    media = pageMedia,
                    vm = vm,
                    onToggleControls = { showControls = !showControls }
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentMedia.source,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = CircleShape,
                                color = when (currentMedia.rating.lowercase()) {
                                    "e", "explicit" -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    "q", "questionable" -> Color(0xFFFF9800).copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                }
                            ) {
                                Text(
                                    text = currentMedia.rating.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, currentMedia.url.ifBlank { currentMedia.sample })
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = {
                            val browserUrl = currentMedia.url.ifBlank { currentMedia.sample }
                            if (browserUrl.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open in browser", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    FilledTonalIconButton(
                        onClick = { vm.toggleFavorite(currentMedia) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isFav) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.5f),
                            contentColor = if (isFav) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { downloadCurrentMedia() },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Rounded.FileDownload,
                                contentDescription = "Download",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (!currentMedia.isVideo) {
                        FilledTonalIconButton(
                            onClick = { showWallpaperDialog = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Wallpaper,
                                contentDescription = "Wallpaper",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showTagsSheet = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Sell,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = Strings.infoAndTags(lang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
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
                        Text(Strings.wallpaperHomeScreen(lang), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(
                        onClick = { applyWallpaper(2) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.wallpaperLockScreen(lang), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(
                        onClick = { applyWallpaper(3) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.wallpaperBoth(lang), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
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
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        label = "fsZoomScale"
    )
    val animatedOffset by androidx.compose.animation.core.animateOffsetAsState(
        targetValue = rawOffset,
        label = "fsZoomOffset"
    )

    var hasLoadError by remember(media.id, media.url) { mutableStateOf(false) }
    val displayUrl = if (hasLoadError) {
        media.sample.ifBlank { media.preview.ifBlank { media.url } }
    } else {
        vm.resolveMediaUrl(media)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
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
                        val maxOffset = (rawScale - 1f) * 600f
                        val newOffset = rawOffset + pan
                        rawOffset = Offset(
                            x = newOffset.x.coerceIn(-maxOffset, maxOffset),
                            y = newOffset.y.coerceIn(-maxOffset, maxOffset)
                        )
                    } else {
                        rawOffset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        var isLoading by remember { mutableStateOf(true) }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(displayUrl)
                .placeholderMemoryCacheKey(media.preview)
                .crossfade(200)
                .allowHardware(!media.isGif)
                .listener(
                    onStart = { isLoading = true },
                    onSuccess = { _, _ -> isLoading = false },
                    onError = { _, _ ->
                        isLoading = false
                        if (!hasLoadError && displayUrl != media.sample && media.sample.isNotBlank()) {
                            hasLoadError = true
                        }
                    }
                )
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
                }
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
