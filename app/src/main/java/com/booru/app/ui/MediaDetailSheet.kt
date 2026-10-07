package com.booru.app.ui

import android.app.Activity
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.ui.text.style.TextAlign
import com.booru.app.data.Strings
import com.booru.app.data.TagClassifier
import com.booru.app.data.TagCategory
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import com.booru.app.data.network.NetworkClient
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
    var showFolderDialog by remember { mutableStateOf(false) }
    var selectedTagForAction by remember { mutableStateOf<String?>(null) }
    var isTagsExpanded by remember { mutableStateOf(false) }
    var showTrueFullscreen by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isDismissingSheet by remember { mutableStateOf(false) }
    val dismissSheetAnimated: () -> Unit = {
        if (!isDismissingSheet) {
            isDismissingSheet = true
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
    }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()
    val isDownloading = currentMedia.mediaKey in vm.activeDownloads
    val isSettingWallpaper = vm.isSettingWallpaper

    BackHandler {
        when {
            showTrueFullscreen -> showTrueFullscreen = false
            selectedTagForAction != null -> selectedTagForAction = null
            showWallpaperDialog -> showWallpaperDialog = false
            showFolderDialog -> showFolderDialog = false
            isCurrentPageZoomed -> {
                resetZoomKey++
                isCurrentPageZoomed = false
            }
            else -> dismissSheetAnimated()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        vm.updateFullscreenIndex(pagerState.currentPage)
    }

    LaunchedEffect(pagerState.currentPage, mediaList.size) {
        isCurrentPageZoomed = false
        if (onLoadMore != null && pagerState.currentPage >= mediaList.size - 4) {
            onLoadMore()
        }
    }

    fun downloadCurrentMedia(media: RemoteMedia) {
        vm.downloadMedia(media)
    }

    fun applyWallpaper(target: Int, media: RemoteMedia) {
        showWallpaperDialog = false
        vm.applyWallpaper(target, media)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 4.dp,
        dragHandle = null,
        sheetState = sheetState,
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
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(width = 32.dp, height = 4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }

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
                    val item = mediaList.getOrNull(page) ?: return@HorizontalPager
                    if (item.isVideo) {
                        BooruVideoPlayer(
                            videoUrl = vm.resolveVideoUrl(item),
                            lang = lang,
                            previewUrl = item.gridImageUrl(vm.imageQuality != com.booru.app.data.ImageQuality.SAVER),
                            modifier = Modifier.fillMaxSize(),
                            isActive = pagerState.currentPage == page && !showTrueFullscreen && !isDismissingSheet
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

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mediaList.size > 1) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
                            shadowElevation = 2.dp
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

                Surface(
                    onClick = { showTrueFullscreen = true },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(38.dp)
                        .bouncyPress()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Fullscreen,
                            contentDescription = Strings.fullscreen(lang),
                            modifier = Modifier.size(20.dp)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .bouncyPress()
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Strings.loadingOriginal(lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        } else {
                            Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Strings.downloadBtn(lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                .size(40.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isFav,
                            enter = fadeIn(tween(180)) + expandHorizontally(tween(200)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180))
                        ) {
                            FilledTonalIconButton(
                                onClick = { showFolderDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .bouncyPress(),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (vm.getMediaFolder(currentMedia) != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (vm.getMediaFolder(currentMedia) != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (vm.getMediaFolder(currentMedia) != null) Icons.Rounded.Folder else Icons.Rounded.FolderOpen,
                                    contentDescription = Strings.addToFolder(lang),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { showTrueFullscreen = true },
                            modifier = Modifier
                                .size(40.dp)
                                .bouncyPress(),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Fullscreen,
                                contentDescription = Strings.fullscreen(lang),
                                modifier = Modifier.size(20.dp)
                            )
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
                                .size(40.dp)
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
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box {
                            FilledTonalIconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .bouncyPress(),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    contentDescription = "More",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                if (!currentMedia.isVideo) {
                                    DropdownMenuItem(
                                        text = { Text(Strings.setWallpaperTitle(lang)) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Wallpaper, null, modifier = Modifier.size(20.dp))
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showWallpaperDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (lang == AppLanguage.RUSSIAN) "Открыть в браузере" else "Open in browser") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(20.dp))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        val browserUrl = currentMedia.postWebUrl.ifBlank { currentMedia.url.ifBlank { currentMedia.sample } }
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
                                        }.onFailure {
                                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val isRealbooru = currentMedia.source.equals("realbooru", ignoreCase = true) || currentMedia.url.contains("realbooru.com")
            val showScore = !isRealbooru
            val showResolution = currentMedia.width > 0 && currentMedia.height > 0

            if (showScore || showResolution) {
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showScore) {
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
                    }

                    if (showResolution) {
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
            }

            Spacer(Modifier.height(14.dp))

            val cardBg by animateColorAsState(
                targetValue = if (isTagsExpanded)
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f),
                animationSpec = tween(220),
                label = "tagsCardBg"
            )
            val chevronBg by animateColorAsState(
                targetValue = if (isTagsExpanded)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(220),
                label = "chevronBg"
            )
            val chevronTint by animateColorAsState(
                targetValue = if (isTagsExpanded)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "chevronTint"
            )
            val rotation by animateFloatAsState(
                targetValue = if (isTagsExpanded) 180f else 0f,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "tagsChevron"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable { isTagsExpanded = !isTagsExpanded }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
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
                                        Icons.Rounded.Sell,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = Strings.tagsLabel(currentMedia.tagList.size, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isTagsExpanded) {
                                        if (lang == AppLanguage.RUSSIAN) "Нажмите, чтобы скрыть" else "Tap to collapse"
                                    } else {
                                        if (lang == AppLanguage.RUSSIAN) "Нажмите, чтобы показать" else "Tap to expand"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = chevronBg,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.ExpandMore,
                                    contentDescription = if (isTagsExpanded) "Collapse" else "Expand",
                                    tint = chevronTint,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isTagsExpanded,
                        enter = expandVertically(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OptInFlowDetailTags(
                                tags = currentMedia.tagList,
                                blacklistedTags = vm.tagBlacklist,
                                lang = lang,
                                onTagClick = { tag ->
                                    vm.searchTag(tag, currentMedia.sourceId.ifBlank { currentMedia.source })
                                    onDismiss()
                                    onNavigateToExplore?.invoke()
                                },
                                onTagLongClick = { tag ->
                                    selectedTagForAction = tag
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWallpaperDialog) {
        val wallpaperSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showWallpaperDialog = false },
            sheetState = wallpaperSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = Strings.setWallpaperTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                wallpaperSheetState.hide()
                            }.invokeOnCompletion {
                                showWallpaperDialog = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Smartphone,
                        title = Strings.wallpaperHomeScreen(lang),
                        onClick = {
                            coroutineScope.launch {
                                wallpaperSheetState.hide()
                            }.invokeOnCompletion {
                                showWallpaperDialog = false
                            }
                            applyWallpaper(1, currentMedia)
                        }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Lock,
                        title = Strings.wallpaperLockScreen(lang),
                        onClick = {
                            coroutineScope.launch {
                                wallpaperSheetState.hide()
                            }.invokeOnCompletion {
                                showWallpaperDialog = false
                            }
                            applyWallpaper(2, currentMedia)
                        }
                    )
                    WallpaperOptionItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = Strings.wallpaperBoth(lang),
                        onClick = {
                            coroutineScope.launch {
                                wallpaperSheetState.hide()
                            }.invokeOnCompletion {
                                showWallpaperDialog = false
                            }
                            applyWallpaper(3, currentMedia)
                        }
                    )
                }
            }
        }
    }

    if (showFolderDialog) {
        val folderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentFolder = vm.getMediaFolder(currentMedia)
        ModalBottomSheet(
            onDismissRequest = { showFolderDialog = false },
            sheetState = folderSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Strings.addToFolder(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    onClick = {
                        vm.setMediaFolder(currentMedia, null)
                        coroutineScope.launch {
                            folderSheetState.hide()
                        }.invokeOnCompletion {
                            showFolderDialog = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (currentFolder == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FolderOff, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(Strings.allFavoritesFolder(lang), fontWeight = FontWeight.SemiBold)
                    }
                }

                vm.customFolders.forEach { folder ->
                    val isSelected = currentFolder == folder
                    Surface(
                        onClick = {
                            vm.setMediaFolder(currentMedia, folder)
                            coroutineScope.launch {
                                folderSheetState.hide()
                            }.invokeOnCompletion {
                                showFolderDialog = false
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(folder, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (selectedTagForAction != null) {
        val currentActionTag = selectedTagForAction!!
        val isBlacklisted = vm.tagBlacklist.any { it.equals(currentActionTag, ignoreCase = true) }
        val tagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedTagForAction = null },
            sheetState = tagSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isBlacklisted)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isBlacklisted) Icons.Rounded.Block else Icons.Rounded.Tag,
                                    contentDescription = null,
                                    tint = if (isBlacklisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "#$currentActionTag",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isBlacklisted) {
                                Text(
                                    text = Strings.tagBlacklistedStatus(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                tagSheetState.hide()
                            }.invokeOnCompletion {
                                selectedTagForAction = null
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable {
                                coroutineScope.launch {
                                    tagSheetState.hide()
                                }.invokeOnCompletion {
                                    selectedTagForAction = null
                                    vm.searchTag(currentActionTag, currentMedia.sourceId.ifBlank { currentMedia.source })
                                    onDismiss()
                                    onNavigateToExplore?.invoke()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = Strings.searchPostsWithTag(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Tag", currentActionTag))
                                Toast.makeText(context, Strings.tagCopied(lang), Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    tagSheetState.hide()
                                }.invokeOnCompletion {
                                    selectedTagForAction = null
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = Strings.copyTag(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isBlacklisted)
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                            .clickable {
                                if (isBlacklisted) {
                                    vm.removeBlacklistedTag(currentActionTag)
                                    Toast.makeText(context, Strings.tagRemovedFromBlacklist(currentActionTag, lang), Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        tagSheetState.hide()
                                    }.invokeOnCompletion {
                                        selectedTagForAction = null
                                    }
                                } else {
                                    vm.addBlacklistedTag(currentActionTag)
                                    Toast.makeText(context, Strings.tagAddedToBlacklist(currentActionTag, lang), Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        tagSheetState.hide()
                                    }.invokeOnCompletion {
                                        selectedTagForAction = null
                                        onDismiss()
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isBlacklisted)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isBlacklisted) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                        contentDescription = null,
                                        tint = if (isBlacklisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isBlacklisted) Strings.removeFromBlacklist(lang) else Strings.addToBlacklist(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBlacklisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTrueFullscreen) {
        ImmersiveMediaViewer(
            initialIndex = pagerState.currentPage,
            mediaList = mediaList,
            vm = vm,
            onDismiss = { newIndex ->
                showTrueFullscreen = false
                if (newIndex in mediaList.indices && newIndex != pagerState.currentPage) {
                    coroutineScope.launch {
                        delay(50)
                        pagerState.scrollToPage(newIndex)
                    }
                }
            },
            onDownload = { downloadCurrentMedia(it) },
            onLoadMore = onLoadMore
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
    var retryKey by remember(media.id, media.url) { mutableIntStateOf(0) }
    val thumbnailKey = remember(media.mediaKey, vm.imageQuality) {
        media.gridImageUrl(vm.imageQuality != com.booru.app.data.ImageQuality.SAVER)
    }
    var detectedRatio by remember(media.id, media.url) {
        mutableFloatStateOf(
            if (media.width > 0 && media.height > 0) {
                media.height.toFloat() / media.width.toFloat()
            } else 1f
        )
    }

    val isComic = detectedRatio >= 3.0f

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

    if (isComic) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detailTargetUrl)
                    .size(coil.size.Size(1080, 4096))
                    .placeholderMemoryCacheKey(thumbnailKey)
                    .crossfade(260)
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
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    } else {
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

        val haptic = LocalHapticFeedback.current
        var showHeartBurst by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (rawScale <= 1.05f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!vm.isFavorite(media)) {
                                    vm.toggleFavorite(media)
                                }
                                showHeartBurst = true
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
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
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detailTargetUrl)
                    .placeholderMemoryCacheKey(thumbnailKey)
                    .crossfade(260)
                    .setParameter("retry", retryKey, memoryCacheKey = null)
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
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        translationX = animatedOffset.x
                        translationY = animatedOffset.y
                    },
                contentScale = ContentScale.Fit
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Loading) {
                    val scope = this
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        scope.SubcomposeAsyncImageContent()
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        )
                    }
                } else if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                detailLoadError = false
                                retryKey++
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = Strings.imageLoadFailed(vm.language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }

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

            HeartBurstOverlay(
                visible = showHeartBurst,
                onAnimationEnd = { showHeartBurst = false }
            )
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
    onTagLongClick: (String) -> Unit,
    blacklistedTags: Collection<String> = emptyList(),
    lang: AppLanguage = AppLanguage.ENGLISH
) {
    if (tags.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.noTags(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val classifiedTags = remember(tags) {
            tags.map { TagClassifier.classify(it) }
        }
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            classifiedTags.forEach { item ->
                val isBlacklisted = blacklistedTags.any {
                    it.equals(item.rawTag, ignoreCase = true) || (it.contains(":") && it.substringAfter(":") == item.rawTag)
                }
                val cat = item.category
                val chipBg = when {
                    isBlacklisted -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    cat.containerColor(isDark) != null -> cat.containerColor(isDark)!!
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
                val chipContent = when {
                    isBlacklisted -> MaterialTheme.colorScheme.error
                    cat.contentColor(isDark) != null -> cat.contentColor(isDark)!!
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val chipIcon = if (isBlacklisted) Icons.Rounded.Block else cat.icon
                val iconTint = if (isBlacklisted) MaterialTheme.colorScheme.error else (cat.contentColor(isDark) ?: MaterialTheme.colorScheme.primary)

                Surface(
                    shape = CircleShape,
                    color = chipBg,
                    modifier = Modifier
                        .bouncyPress()
                        .pointerInput(item.rawTag) {
                            detectTapGestures(
                                onTap = { onTagClick(item.rawTag) },
                                onLongPress = { onTagLongClick(item.rawTag) }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = chipIcon,
                            contentDescription = cat.displayName,
                            modifier = Modifier.size(13.dp),
                            tint = iconTint
                        )
                        Text(
                            text = item.displayTag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (cat != TagCategory.GENERAL) FontWeight.SemiBold else FontWeight.Medium,
                            color = chipContent,
                            textDecoration = if (isBlacklisted) TextDecoration.LineThrough else null
                        )
                    }
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
    lang: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isExternalControls: Boolean = false,
    externalShowControls: Boolean = true,
    onToggleControls: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activeState = rememberUpdatedState(isActive)
    var playbackError by remember(videoUrl) { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var userPaused by remember(videoUrl) { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var internalShowControls by remember { mutableStateOf(true) }
    val effectiveShowControls = if (isExternalControls) externalShowControls else internalShowControls
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekRatio by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    fun toggleControls() {
        if (isExternalControls) {
            onToggleControls?.invoke()
        } else {
            internalShowControls = !internalShowControls
        }
    }

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
            NetworkClient.baseClient.newBuilder()
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
                playWhenReady = activeState.value
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isReady = true
                            playbackError = false
                            if (duration > 0L) durationMs = duration
                            if (activeState.value && !userPaused && !isPlaying) {
                                play()
                            } else if (!activeState.value && isPlaying) {
                                pause()
                            }
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playbackError = true
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

    LaunchedEffect(internalShowControls, isPlaying, isSeeking, isExternalControls) {
        if (!isExternalControls && internalShowControls && isPlaying && !isSeeking) {
            delay(4000)
            internalShowControls = false
        }
    }

    LaunchedEffect(isActive, exoPlayer, userPaused) {
        if (!isActive) {
            exoPlayer.pause()
        } else if (!userPaused) {
            exoPlayer.playWhenReady = true
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(exoPlayer, lifecycleOwner) {
        com.booru.app.BooruVideoCache.acquirePlayer()
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
            com.booru.app.BooruVideoCache.releasePlayer()
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
                    setOnClickListener { toggleControls() }
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleControls() }
                    )
                }
        )

        if (playbackError) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = Strings.playbackError(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            playbackError = false
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = activeState.value
                        },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else if (!isReady) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        AnimatedVisibility(
            visible = effectiveShowControls || !isPlaying,
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
                            userPaused = true
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            userPaused = false
                            exoPlayer.playWhenReady = true
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0L)
                            }
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
            visible = effectiveShowControls || !isPlaying,
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
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = if (isExternalControls) 20.dp else 10.dp
                    )
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
