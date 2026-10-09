@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.Crossfade
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val showMessage = LocalShowMessage.current
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

    val sheetState = rememberExpandedSheetState()
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

    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.statusBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = null,
            sheetState = sheetState,
            shape = ShapeTokens.ExtraLargeIncreasedTop
        ) {
            Box {
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
                        .clip(ShapeTokens.ExtraLargeIncreased)
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
                                isActive = pagerState.currentPage == page && !showTrueFullscreen && !isDismissingSheet,
                                onFullscreen = { showTrueFullscreen = true }
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
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
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
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
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
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f)
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
    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isCurrentPageZoomed && !currentMedia.isVideo,
                        enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.8f),
                        exit = fadeOut(Motion.effectsFast()) + scaleOut(Motion.effectsFast(), targetScale = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        val pressSource1 = remember { MutableInteractionSource() }
                        Surface(
                            onClick = { showTrueFullscreen = true },
                            shape = pressMorphShape(pressSource1),
                            interactionSource = pressSource1,
                            color = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = Strings.fullscreen(lang),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
    
                Spacer(Modifier.height(14.dp))
    
                var showMoreSheet by remember { mutableStateOf(false) }
                val isFav = vm.isFavorite(currentMedia)
                val inFolder = vm.getMediaFolder(currentMedia) != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailAction(
                        position = GroupPosition.Leading,
                        onClick = { if (!isDownloading) downloadCurrentMedia(currentMedia) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isDownloading) {
                            LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Strings.loadingOriginal(lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Strings.downloadBtn(lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(2.dp))
                    val favContainer by animateColorAsState(
                        targetValue = if (isFav) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        animationSpec = Motion.effectsDefault(),
                        label = "favContainer"
                    )
                    DetailAction(
                        position = GroupPosition.Middle,
                        onClick = { vm.toggleFavorite(currentMedia) },
                        containerColor = favContainer,
                        contentColor = if (isFav) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Crossfade(targetState = isFav, animationSpec = Motion.effectsDefault(), label = "favIcon") { fav ->
                            Icon(
                                imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = isFav,
                        enter = expandHorizontally(Motion.spatialDefault(), expandFrom = Alignment.Start) + fadeIn(Motion.effectsDefault()),
                        exit = shrinkHorizontally(Motion.effectsFast(), shrinkTowards = Alignment.Start) + fadeOut(Motion.effectsFast())
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            Spacer(Modifier.width(2.dp))
                            DetailAction(
                                position = GroupPosition.Middle,
                                onClick = { showFolderDialog = true },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.width(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (inFolder) Icons.Rounded.Folder else Icons.Rounded.CreateNewFolder,
                                    contentDescription = Strings.addToFolder(lang),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(2.dp))
                    DetailAction(
                        position = GroupPosition.Trailing,
                        onClick = { showMoreSheet = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", modifier = Modifier.size(22.dp))
                    }
                }

                if (showMoreSheet) {
                    val moreSheetState = rememberExpandedSheetState()
                    SheetMotion {
                        ModalBottomSheet(
                            onDismissRequest = { showMoreSheet = false },
                            sheetState = moreSheetState,
                            shape = ShapeTokens.ExtraLargeTop,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 24.dp)
                            ) {
                                val actions = if (currentMedia.isVideo) listOf(2, 1) else listOf(2, 0, 1)
                                actions.forEachIndexed { index, action ->
                                    TagActionItem(
                                        icon = when (action) {
                                            0 -> Icons.Rounded.Wallpaper
                                            2 -> Icons.Rounded.Share
                                            else -> Icons.AutoMirrored.Rounded.OpenInNew
                                        },
                                        title = when (action) {
                                            0 -> Strings.setWallpaperTitle(lang)
                                            2 -> Strings.share(lang)
                                            else -> Strings.tr(lang, "Open in browser", "Открыть в браузере", "ブラウザで開く", "在浏览器中打开", "브라우저에서 열기", "فتح في المتصفح")
                                        },
                                        shape = segmentedListShape(index, actions.size),
                                        onClick = {
                                            coroutineScope.launch { moreSheetState.hide() }.invokeOnCompletion {
                                                showMoreSheet = false
                                                if (action == 0) {
                                                    showWallpaperDialog = true
                                                } else if (action == 2) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        val shareUrl = currentMedia.postWebUrl.ifBlank { currentMedia.url.ifBlank { currentMedia.sample } }
                                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, Strings.share(lang)))
                                                } else {
                                                    val browserUrl = currentMedia.postWebUrl.ifBlank { currentMedia.url.ifBlank { currentMedia.sample } }
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)))
                                                    }.onFailure {
                                                        showMessage(Strings.noBrowserFound(lang))
                                                    }
                                                }
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
                    Spacer(Modifier.height(12.dp))
                    val stats = buildList {
                        if (showScore) add(Triple(Icons.Rounded.Star, currentMedia.score.toString(), Strings.tr(lang, "Score", "Рейтинг", "スコア", "评分", "점수", "النقاط")))
                        if (showResolution) add(Triple(Icons.Rounded.AspectRatio, "${currentMedia.width}×${currentMedia.height}", Strings.tr(lang, "Resolution", "Разрешение", "解像度", "分辨率", "해상도", "الدقة")))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        stats.forEachIndexed { index, (icon, value, label) ->
                            val outer = 20.dp
                            val inner = 4.dp
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = if (index == 0) outer else inner,
                                    bottomStart = if (index == 0) outer else inner,
                                    topEnd = if (index == stats.lastIndex) outer else inner,
                                    bottomEnd = if (index == stats.lastIndex) outer else inner
                                ),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = ShapeTokens.Medium,
                                        color = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (index == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(icon, null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
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
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = Motion.effectsDefault(),
                    label = "tagsCardBg"
                )
                val chevronBg by animateColorAsState(
                    targetValue = if (isTagsExpanded)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = Motion.effectsDefault(),
                    label = "chevronBg"
                )
                val chevronTint by animateColorAsState(
                    targetValue = if (isTagsExpanded)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = Motion.effectsDefault(),
                    label = "chevronTint"
                )
                val rotation by animateFloatAsState(
                    targetValue = if (isTagsExpanded) 180f else 0f,
                    animationSpec = Motion.effectsDefault(),
                    label = "tagsChevron"
                )
    
                Surface(
                    shape = ShapeTokens.LargeIncreased,
                    color = cardBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTagsExpanded = !isTagsExpanded }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = ShapeTokens.Medium,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Sell,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = Strings.tr(lang, "Tags", "Теги", "タグ", "标签", "태그", "الوسوم"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = currentMedia.tagList.size.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                animationSpec = Motion.effectsDefault(),
                                expandFrom = Alignment.Top
                            ) + fadeIn(
                                animationSpec = Motion.effectsDefault()
                            ),
                            exit = shrinkVertically(
                                animationSpec = Motion.effectsDefault(),
                                shrinkTowards = Alignment.Top
                            ) + fadeOut(
                                animationSpec = Motion.effectsDefault()
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
                                        vm.searchTag(tag)
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
            SheetSnackbarHost(Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
            }
        }
    }

    if (showWallpaperDialog) {
        val wallpaperSheetState = rememberExpandedSheetState()
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showWallpaperDialog = false },
                sheetState = wallpaperSheetState,
                shape = ShapeTokens.ExtraLargeTop,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    }
    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TagActionItem(
                            icon = Icons.Rounded.Smartphone,
                            title = Strings.wallpaperHomeScreen(lang),
                            shape = segmentedListShape(0, 3),
                            onClick = {
                                coroutineScope.launch {
                                    wallpaperSheetState.hide()
                                }.invokeOnCompletion {
                                    showWallpaperDialog = false
                                }
                                applyWallpaper(1, currentMedia)
                            }
                        )
                        TagActionItem(
                            icon = Icons.Rounded.Lock,
                            title = Strings.wallpaperLockScreen(lang),
                            shape = segmentedListShape(1, 3),
                            onClick = {
                                coroutineScope.launch {
                                    wallpaperSheetState.hide()
                                }.invokeOnCompletion {
                                    showWallpaperDialog = false
                                }
                                applyWallpaper(2, currentMedia)
                            }
                        )
                        TagActionItem(
                            icon = Icons.Rounded.Wallpaper,
                            title = Strings.wallpaperBoth(lang),
                            shape = segmentedListShape(2, 3),
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
    }

    if (showFolderDialog) {
        val folderKeyboardSheet = rememberKeyboardSheetState()
        val folderSheetState = folderKeyboardSheet.state
        val currentFolder = vm.getMediaFolder(currentMedia)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showFolderDialog = false },
                sheetState = folderSheetState,
                shape = ShapeTokens.ExtraLargeTop,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                KeyboardSheetEffects(folderKeyboardSheet, onDismiss = { showFolderDialog = false })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                    ) {
                        Surface(
                            shape = ShapeTokens.Medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.addToFolder(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentFolder ?: Strings.allFavoritesFolder(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val folderOptions = listOf<String?>(null) + vm.customFolders.toList()
                    val folderCounts = remember(vm.favoritesList, vm.favoriteFolders, vm.customFolders) {
                        folderOptions.associateWith { f -> vm.favoritesList.count { vm.getMediaFolder(it) == f } }
                    }
                    fun pick(folder: String?) {
                        vm.setMediaFolder(currentMedia, folder)
                        coroutineScope.launch { folderKeyboardSheet.hide() }.invokeOnCompletion { showFolderDialog = false }
                    }
                    val folderRows = folderOptions.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        folderRows.forEachIndexed { rowIndex, rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                rowItems.forEachIndexed { colIndex, folder ->
                                    val firstRow = rowIndex == 0
                                    val lastRow = rowIndex == folderRows.lastIndex
                                    val firstCol = colIndex == 0
                                    val lastCol = colIndex == rowItems.lastIndex
                                    FolderPickTile(
                                        title = folder ?: Strings.allFavoritesFolder(lang),
                                        count = folderCounts[folder] ?: 0,
                                        icon = if (folder == null) Icons.Rounded.FolderSpecial else Icons.Rounded.Folder,
                                        selected = currentFolder == folder,
                                        onClick = { pick(folder) },
                                        outerTopStart = firstRow && firstCol,
                                        outerTopEnd = firstRow && lastCol,
                                        outerBottomStart = lastRow && firstCol,
                                        outerBottomEnd = lastRow && lastCol,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    var newFolderName by remember { mutableStateOf("") }
                    val canCreate = newFolderName.isNotBlank() &&
                        vm.customFolders.none { it.equals(newFolderName.trim(), ignoreCase = true) }
                    fun create() {
                        if (!canCreate) return
                        val name = newFolderName.trim()
                        vm.addCustomFolder(name)
                        pick(name)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 18.dp, end = 12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CreateNewFolder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (newFolderName.isEmpty()) {
                                        Text(
                                            text = Strings.newFolder(lang),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = newFolderName,
                                        onValueChange = { newFolderName = it.take(32) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { create() }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        val createCorner by animateDpAsState(
                            targetValue = if (canCreate) 28.dp else 6.dp,
                            animationSpec = Motion.spatialDefault(),
                            label = "createFolderCorner"
                        )
                        FilledIconButton(
                            onClick = { create() },
                            enabled = canCreate,
                            shape = RoundedCornerShape(topStart = createCorner, bottomStart = createCorner, topEnd = 28.dp, bottomEnd = 28.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = Strings.create(lang), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (selectedTagForAction != null) {
        val currentActionTag = selectedTagForAction!!
        val isBlacklisted = vm.tagBlacklist.any { it.equals(currentActionTag, ignoreCase = true) }
        val tagSheetState = rememberExpandedSheetState()
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { selectedTagForAction = null },
                sheetState = tagSheetState,
                shape = ShapeTokens.ExtraLargeTop,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp)
                    ) {
                        val headerColor by animateColorAsState(
                            targetValue = if (isBlacklisted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            animationSpec = Motion.effectsDefault(),
                            label = "tagHeaderColor"
                        )
                        Surface(
                            shape = CircleShape,
                            color = headerColor,
                            contentColor = if (isBlacklisted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isBlacklisted) Icons.Rounded.Block else Icons.Rounded.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentActionTag.replace('_', ' '),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isBlacklisted,
                                enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.spatialDefault()),
                                exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.spatialDefault())
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = Strings.tagBlacklistedStatus(lang),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TagActionItem(
                        icon = Icons.Rounded.Search,
                        title = Strings.searchPostsWithTag(lang),
                        shape = segmentedListShape(0, 3),
                        onClick = {
                            coroutineScope.launch {
                                tagSheetState.hide()
                            }.invokeOnCompletion {
                                selectedTagForAction = null
                                vm.searchTag(currentActionTag)
                                onDismiss()
                                onNavigateToExplore?.invoke()
                            }
                        }
                    )
                    TagActionItem(
                        icon = Icons.Rounded.ContentCopy,
                        title = Strings.copyTag(lang),
                        shape = segmentedListShape(1, 3),
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Tag", currentActionTag))
                            coroutineScope.launch {
                                tagSheetState.hide()
                            }.invokeOnCompletion {
                                selectedTagForAction = null
                            }
                        }
                    )
                    TagActionItem(
                        icon = if (isBlacklisted) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.Block,
                        title = if (isBlacklisted) Strings.removeFromBlacklist(lang) else Strings.addToBlacklist(lang),
                        shape = segmentedListShape(2, 3),
                        destructive = !isBlacklisted,
                        onClick = {
                            if (isBlacklisted) {
                                vm.removeBlacklistedTag(currentActionTag)
                            } else {
                                vm.addBlacklistedTag(currentActionTag)
                            }
                        }
                    )
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
private fun TagActionItem(
    icon: ImageVector,
    title: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val badgeColor by animateColorAsState(
        targetValue = if (destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = Motion.effectsDefault(),
        label = "tagActionBadge"
    )
    val badgeContent by animateColorAsState(
        targetValue = if (destructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = Motion.effectsDefault(),
        label = "tagActionBadgeContent"
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                contentColor = badgeContent,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = icon,
                        transitionSpec = { (fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.7f)) togetherWith fadeOut(Motion.effectsFast()) },
                        label = "tagActionIcon"
                    ) { target ->
                        Icon(target, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(Motion.effectsDefault()) togetherWith fadeOut(Motion.effectsFast()) },
                label = "tagActionTitle",
                modifier = Modifier.weight(1f)
            ) { target ->
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
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
                        LoadingIndicator(modifier = Modifier.size(44.dp), color = MaterialTheme.colorScheme.primary)
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

                                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
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
                        LoadingIndicator(modifier = Modifier.size(40.dp), color = MaterialTheme.colorScheme.primary)
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
                    shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(38.dp)
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
    val pressSource2 = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = pressMorphShape(pressSource2, resting = 12.dp),
        interactionSource = pressSource2,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
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
                val scheme = MaterialTheme.colorScheme
                val roles = cat.roles()
                val isGeneral = cat == TagCategory.GENERAL
                val chipBg = when {
                    isBlacklisted -> scheme.errorContainer.copy(alpha = 0.25f)
                    isGeneral -> scheme.surfaceContainerHigh
                    else -> roles.container
                }
                val chipContent = when {
                    isBlacklisted -> scheme.error
                    isGeneral -> scheme.onSurface
                    else -> roles.onContainer
                }
                val chipIcon = if (isBlacklisted) Icons.Rounded.Block else cat.icon
                val iconTint = when {
                    isBlacklisted -> scheme.error
                    isGeneral -> roles.accent
                    else -> roles.onContainer
                }

                val chipPress = remember { MutableInteractionSource() }
                val chipShape = pressMorphShape(chipPress, pressedFraction = 0.3f)
                Surface(
                    shape = chipShape,
                    color = chipBg,
                    modifier = Modifier
                        .clip(chipShape)
                        .combinedClickable(
                            interactionSource = chipPress,
                            indication = ripple(),
                            onClick = { onTagClick(item.rawTag) },
                            onLongClick = { onTagLongClick(item.rawTag) }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = chipIcon,
                            contentDescription = cat.localizedName(lang),
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
        color = bg
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
    onToggleControls: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null
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
        val cacheDataSourceFactory = com.booru.app.BooruVideoCache.playerDataSourceFactory(context, videoUrl)

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
                setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
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

    LaunchedEffect(exoPlayer, isActive) {
        if (isActive) com.booru.app.BooruVideoCache.prefetchParallel(context, videoUrl)
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
                shape = ShapeTokens.ExtraLarge,
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
            LoadingIndicator(modifier = Modifier.size(44.dp), color = MaterialTheme.colorScheme.primary)
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
                        .size(46.dp),
                    shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
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
                        .size(60.dp),
                    shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
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
                        .size(46.dp),
                    shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
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

                ExpressiveSlider(
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
                            shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .size(34.dp)
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
                            shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onFullscreen != null) {
                            FilledTonalIconButton(
                                onClick = onFullscreen,
                                shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = Strings.fullscreen(lang),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

@Composable
private fun FolderPickTile(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    outerTopStart: Boolean,
    outerTopEnd: Boolean,
    outerBottomStart: Boolean,
    outerBottomEnd: Boolean,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = Motion.effectsDefault(),
        label = "folderTileColor"
    )
    val badge by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = Motion.effectsDefault(),
        label = "folderTileBadge"
    )
    val badgeContent by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = Motion.effectsDefault(),
        label = "folderTileBadgeContent"
    )
    @Composable
    fun corner(outer: Boolean, label: String): androidx.compose.ui.unit.Dp {
        val value by animateDpAsState(
            targetValue = if (selected) 32.dp else if (outer) 24.dp else 6.dp,
            animationSpec = Motion.spatialDefault(),
            label = label
        )
        return value
    }
    val shape = RoundedCornerShape(
        topStart = corner(outerTopStart, "tileTS"),
        topEnd = corner(outerTopEnd, "tileTE"),
        bottomStart = corner(outerBottomStart, "tileBS"),
        bottomEnd = corner(outerBottomEnd, "tileBE")
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 14.dp)
        ) {
            Surface(shape = CircleShape, color = badge, contentColor = badgeContent, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (selected) Icons.Rounded.Check else icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailAction(
    position: GroupPosition,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val outer by animateDpAsState(if (pressed) 16.dp else 28.dp, Motion.spatialFast(), label = "detailOuter")
    val inner by animateDpAsState(if (pressed) 4.dp else 8.dp, Motion.spatialFast(), label = "detailInner")
    val shape = when (position) {
        GroupPosition.Leading -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
        GroupPosition.Trailing -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
        GroupPosition.Middle -> RoundedCornerShape(inner)
    }
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxHeight()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            content = content
        )
    }
}
