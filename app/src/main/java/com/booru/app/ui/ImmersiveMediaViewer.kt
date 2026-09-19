package com.booru.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImmersiveMediaViewer(
    initialIndex: Int,
    mediaList: List<RemoteMedia>,
    vm: GalleryViewModel,
    onDismiss: (Int) -> Unit,
    onDownload: (RemoteMedia) -> Unit,
    onLoadMore: (() -> Unit)? = null
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss(0) }
        return
    }

    val context = LocalContext.current
    val view = LocalView.current
    val safeInitial = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { mediaList.size }
    )

    var showControls by remember { mutableStateOf(true) }
    var isPageZoomed by remember { mutableStateOf(false) }
    var resetZoomKey by remember { mutableIntStateOf(0) }

    BackHandler {
        onDismiss(pagerState.currentPage)
    }

    LaunchedEffect(pagerState.currentPage) {
        isPageZoomed = false
        if (onLoadMore != null && pagerState.currentPage >= mediaList.size - 4) {
            onLoadMore()
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Dialog(
        onDismissRequest = { onDismiss(pagerState.currentPage) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DisposableEffect(Unit) {
            var parent = view.parent
            var targetWindow: android.view.Window? = null
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    targetWindow = parent.window
                    break
                }
                parent = parent.parent
            }
            val window = targetWindow ?: (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
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
                userScrollEnabled = !isPageZoomed,
                beyondViewportPageCount = 1,
                key = { page ->
                    val m = mediaList.getOrNull(page)
                    if (m != null) "fs_${m.source}_${m.id.ifBlank { m.url }}_$page" else page
                }
            ) { page ->
                val item = mediaList[page]
                val isCurrent = (pagerState.currentPage == page)
                if (item.isVideo) {
                    BooruVideoPlayer(
                        videoUrl = vm.resolveVideoUrl(item),
                        previewUrl = if (vm.imageQuality == com.booru.app.data.ImageQuality.SAVER) item.preview.ifBlank { item.sample } else item.sample.ifBlank { item.preview.ifBlank { item.url } },
                        modifier = Modifier.fillMaxSize(),
                        isActive = isCurrent,
                        isExternalControls = true,
                        externalShowControls = showControls,
                        onToggleControls = {
                            showControls = !showControls
                        }
                    )
                } else {
                    FullscreenZoomableImage(
                        media = item,
                        vm = vm,
                        isActive = isCurrent,
                        resetZoomKey = if (isCurrent) resetZoomKey else 0,
                        onZoomChanged = { zoomed ->
                            if (isCurrent) isPageZoomed = zoomed
                        },
                        onToggleControls = {
                            showControls = !showControls
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onDismiss(pagerState.currentPage) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.40f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val current = mediaList[pagerState.currentPage]
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.40f)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            if (current.isVideo) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.40f)
                                ) {
                                    Text(
                                        text = current.source.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val current = mediaList[pagerState.currentPage]
                            val isFav = vm.isFavorite(current)
                            IconButton(
                                onClick = { vm.toggleFavorite(current) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    contentColor = if (isFav) MaterialTheme.colorScheme.primary else Color.White
                                )
                            ) {
                                Icon(
                                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favorite"
                                )
                            }

                            IconButton(
                                onClick = { onDownload(current) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = "Download")
                            }
                        }
                    }
                }
            }

            val currentMediaItem = mediaList.getOrNull(pagerState.currentPage)
            AnimatedVisibility(
                visible = showControls && (currentMediaItem?.isVideo == false),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val current = mediaList[pagerState.currentPage]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = current.source.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (current.width > 0 && current.height > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = "${current.width} × ${current.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (isPageZoomed) {
                            FilledTonalIconButton(
                                onClick = {
                                    resetZoomKey++
                                    isPageZoomed = false
                                },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.ZoomOutMap, contentDescription = "Reset Zoom")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenZoomableImage(
    media: RemoteMedia,
    vm: GalleryViewModel,
    isActive: Boolean,
    resetZoomKey: Int,
    onZoomChanged: (Boolean) -> Unit,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var rawScale by remember { mutableFloatStateOf(1f) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }
    var detailLoadError by remember(media.id, media.url) { mutableStateOf(false) }
    var detectedRatio by remember(media.id, media.url) {
        mutableFloatStateOf(
            if (media.width > 0 && media.height > 0) {
                media.height.toFloat() / media.width.toFloat()
            } else 1f
        )
    }

    val isComic = (media.width > 0 && media.height > 0 && media.height.toFloat() / media.width.toFloat() >= 2.5f) || (detectedRatio >= 2.5f)

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

    val detailTargetUrl = if (detailLoadError) {
        media.preview.ifBlank { media.sample.ifBlank { media.url } }
    } else {
        vm.resolveMediaUrl(media)
    }

    val animatedScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = Motion.softSpring(),
        label = "fsZoomScale"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = Motion.softSpring(),
        label = "fsZoomOffset"
    )

    if (isComic) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleControls() }
                    )
                }
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detailTargetUrl)
                    .size(coil.size.Size(1080, 4096))
                    .crossfade(300)
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, result ->
                            val w = result.drawable.intrinsicWidth
                            val h = result.drawable.intrinsicHeight
                            if (w > 0 && h > 0) {
                                detectedRatio = h.toFloat() / w.toFloat()
                            }
                        },
                        onError = { _, _ ->
                            if (!detailLoadError && detailTargetUrl != media.sample && media.sample.isNotBlank()) {
                                detailLoadError = true
                            } else if (!detailLoadError && detailTargetUrl != media.preview && media.preview.isNotBlank()) {
                                detailLoadError = true
                            }
                        }
                    )
                    .build(),
                contentDescription = media.tags,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleControls() },
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
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) break

                            val pointerCount = event.changes.size
                            if (pointerCount >= 2 || rawScale > 1.05f) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val newScale = (rawScale * zoomChange).coerceIn(1f, 5f)
                                rawScale = newScale
                                val zoomed = newScale > 1.05f
                                onZoomChanged(zoomed)

                                if (zoomed) {
                                    val maxOffsetX = ((newScale - 1f) * size.width.toFloat() / 2f).coerceAtLeast(0f)
                                    val maxOffsetY = ((newScale - 1f) * size.height.toFloat() / 2f).coerceAtLeast(0f)
                                    val candidate = rawOffset + panChange
                                    rawOffset = Offset(
                                        x = candidate.x.coerceIn(-maxOffsetX, maxOffsetX),
                                        y = candidate.y.coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    rawOffset = Offset.Zero
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detailTargetUrl)
                    .crossfade(300)
                    .allowHardware(!media.isGif)
                    .listener(
                        onSuccess = { _, result ->
                            val w = result.drawable.intrinsicWidth
                            val h = result.drawable.intrinsicHeight
                            if (w > 0 && h > 0) {
                                detectedRatio = h.toFloat() / w.toFloat()
                            }
                        },
                        onError = { _, _ ->
                            if (!detailLoadError && detailTargetUrl != media.sample && media.sample.isNotBlank()) {
                                detailLoadError = true
                            } else if (!detailLoadError && detailTargetUrl != media.preview && media.preview.isNotBlank()) {
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
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Icon(
                        Icons.Rounded.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}
