package com.booru.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.booru.app.data.TagCategory
import com.booru.app.data.TagClassifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.booru.app.BooruRepository
import com.booru.app.ContentType
import kotlinx.coroutines.launch
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.SortOrder
import com.booru.app.data.AppLanguage
import com.booru.app.data.CustomBooruSource
import com.booru.app.data.ImageQuality
import com.booru.app.data.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    vm: GalleryViewModel,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = vm.language
    var searchExpanded by remember { mutableStateOf(false) }
    var localQuery by remember { mutableStateOf(TextFieldValue(vm.query, TextRange(vm.query.length))) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm.query) {
        if (localQuery.text != vm.query) {
            localQuery = TextFieldValue(vm.query, TextRange(vm.query.length))
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = searchExpanded || vm.query.isNotBlank()) {
        if (searchExpanded) {
            searchExpanded = false
            localQuery = TextFieldValue(vm.query, TextRange(vm.query.length))
            vm.clearTagSuggestions()
        } else if (vm.query.isNotBlank()) {
            localQuery = TextFieldValue("")
            vm.search(vm.source, "", vm.safeMode)
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    var handledScrollTrigger by rememberSaveable { mutableLongStateOf(vm.scrollToTopTrigger) }
    var handledResultsEpoch by rememberSaveable { mutableIntStateOf(vm.resultsEpoch) }

    LaunchedEffect(vm.scrollToTopTrigger) {
        if (vm.scrollToTopTrigger == handledScrollTrigger) return@LaunchedEffect
        handledScrollTrigger = vm.scrollToTopTrigger
        if (gridState.firstVisibleItemIndex > 0) {
            if (gridState.firstVisibleItemIndex > 30) gridState.scrollToItem(12)
            gridState.animateScrollToItem(0)
        } else if (!vm.isRefreshing && !vm.loading) {
            vm.refresh(isPull = false)
        }
    }

    LaunchedEffect(vm.resultsEpoch) {
        if (vm.resultsEpoch == handledResultsEpoch) return@LaunchedEffect
        handledResultsEpoch = vm.resultsEpoch
        gridState.scrollToItem(0)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val visible = info.visibleItemsInfo
            val lastVisible = visible.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            val lanes = visible.map { it.lane }.distinct().size.coerceAtLeast(1)
            val notEnoughItemsToScroll = visible.size == total
            total > 0 && (lastVisible >= total - maxOf(12, lanes * 6) || notEnoughItemsToScroll) &&
                !vm.loading && !vm.isRefreshing && !vm.loadingMore && !vm.loadMoreError && vm.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    val context = LocalContext.current
    LaunchedEffect(gridState, vm.imageQuality) {
        val preferLarge = vm.imageQuality != ImageQuality.SAVER
        val prefetched = HashSet<String>()
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { last ->
                if (last < 0) return@collect
                val upcoming = vm.results.subList(
                    (last + 1).coerceAtMost(vm.results.size),
                    (last + 1 + PREFETCH_AHEAD).coerceAtMost(vm.results.size)
                )
                val loader = context.imageLoader
                for (media in upcoming) {
                    val url = media.gridImageUrl(preferLarge)
                    if (url.isBlank() || !prefetched.add(url)) continue
                    loader.enqueue(
                        ImageRequest.Builder(context)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                    )
                }
                if (prefetched.size > 600) prefetched.clear()
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { showSourceSheet = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .bouncyPress()
                ) {
                    Icon(
                        imageVector = when (vm.source) {
                            BooruRepository.SOURCE_ALL -> Icons.Rounded.AutoAwesome
                            BooruRepository.SOURCE_GELBOORU -> Icons.Rounded.Image
                            BooruRepository.SOURCE_RULE34 -> Icons.Rounded.Explicit
                            BooruRepository.SOURCE_REALBOORU -> Icons.Rounded.VideoLibrary
                            BooruRepository.SOURCE_XBOORU -> Icons.Rounded.PhotoLibrary
                            BooruRepository.SOURCE_TBIB -> Icons.Rounded.Public
                            BooruRepository.SOURCE_YANDE -> Icons.Rounded.Collections
                            BooruRepository.SOURCE_KONACHAN -> Icons.Rounded.Wallpaper
                            else -> Icons.Rounded.Shield
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = vm.getSourceDisplayName(vm.source),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val activeFilterCount = (if (vm.safeMode || vm.excludeSafe) 1 else 0) +
                        (if (vm.noAi) 1 else 0) +
                        (if (vm.sortOrder != SortOrder.NEWEST) 1 else 0) +
                        (if (vm.selectedContentTypes.isNotEmpty()) 1 else 0)

                FilledTonalButton(
                    onClick = { showFilterSheet = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .bouncyPress()
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Strings.filtersButton(lang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeFilterCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$activeFilterCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (vm.query.isBlank()) Strings.allPosts(lang) else "«${vm.query}»",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (vm.query.isNotBlank() && vm.results.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val countText = if (vm.activeTagCount > 0) {
                                "${vm.activeTagCount}"
                            } else if (vm.hasMore) {
                                "${vm.results.size}+"
                            } else {
                                "${vm.results.size}"
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

            }

            vm.error?.let { rawErr ->
                val displayMessage = remember(rawErr, vm.isAuthError, vm.authErrorSource, vm.authErrorCode, vm.isNetworkError, lang) {
                    val srcName = vm.authErrorSource?.let { vm.getSourceDisplayName(it) } ?: vm.getSourceDisplayName(vm.source)
                    when {
                        vm.isNetworkError -> Strings.offlineDesc(lang)
                        vm.isAuthError -> {
                            Strings.authErrorDesc(srcName, vm.authErrorCode, lang)
                        }
                        vm.authErrorCode == 429 -> {
                            Strings.rateLimitErrorDesc(srcName, null, lang)
                        }
                        vm.authErrorCode != null -> {
                            Strings.httpErrorDesc(srcName, vm.authErrorCode!!, lang)
                        }
                        rawErr.contains("timeout", ignoreCase = true) -> {
                            Strings.timeoutErrorDesc(srcName, lang)
                        }
                        rawErr.contains("Insecure HTTP", ignoreCase = true) -> {
                            Strings.insecureHttpWarning(srcName, lang)
                        }
                        rawErr.isBlank() || rawErr == "Failed to load data" || rawErr == "Load failed" -> {
                            Strings.failedToLoad(lang)
                        }
                        else -> rawErr
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (vm.isAuthError)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    vm.isAuthError -> Icons.Rounded.Key
                                    vm.isNetworkError -> Icons.Rounded.WifiOff
                                    else -> Icons.Rounded.Warning
                                },
                                contentDescription = null,
                                tint = if (vm.isAuthError)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = when {
                                    vm.isAuthError -> Strings.authErrorTitle(lang)
                                    vm.isNetworkError -> Strings.offlineTitle(lang)
                                    else -> Strings.genericErrorTitle(lang)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (vm.isAuthError)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = displayMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (vm.isAuthError)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (vm.isAuthError) {
                                Button(
                                    onClick = onNavigateToSettings,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text(Strings.enterApiKeyBtn(lang))
                                }
                                Spacer(Modifier.width(8.dp))
                            } else {
                                TextButton(onClick = { vm.search(vm.source, vm.query, vm.safeMode) }) {
                                    Text(Strings.retryBtn(lang), color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            TextButton(onClick = { vm.clearError() }) {
                                Text(Strings.closeBtn(lang))
                            }
                        }
                    }
                }
            }

            val pullRefreshState = rememberPullToRefreshState()
            val pullOffset = remember { Animatable(0f) }
            LaunchedEffect(pullRefreshState) {
                snapshotFlow {
                    if (vm.isRefreshing || pullRefreshState.isAnimating) 0f
                    else (pullRefreshState.distanceFraction * 40f).coerceIn(0f, 60f)
                }.collectLatest { target ->
                    if (target == 0f) {
                        pullOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy))
                    } else {
                        pullOffset.snapTo(target)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = vm.isRefreshing,
                    onRefresh = { vm.refresh(isPull = true) },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {}
                ) {
                    if (vm.loading && vm.results.isEmpty()) {
                        SkeletonGrid(columnsSetting = vm.gridColumnsCount)
                    } else if (!vm.loading && vm.results.isEmpty() && vm.error == null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.ImageSearch,
                                            null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    Strings.nothingFound(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                val isImageOnlyBoard = vm.source in listOf("Yande.re", "Konachan", "Safebooru", "TBIB")
                                val isVideoOnlyFilter = vm.selectedContentTypes.contains(ContentType.VIDEOS) && !vm.selectedContentTypes.contains(ContentType.PHOTOS) && !vm.selectedContentTypes.contains(ContentType.GIFS)
                                Text(
                                    if (isImageOnlyBoard && isVideoOnlyFilter) Strings.sourceNoVideosNotice(vm.source, lang) else Strings.nothingFoundDesc(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(14.dp))
                                EmptyResultActions(vm = vm, lang = lang)
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = { vm.refresh(isPull = false) },
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(Strings.refreshBtn(lang))
                                }
                            }
                        }
                    } else {
                        val gridCells = when (vm.gridColumnsCount) {
                            1 -> StaggeredGridCells.Fixed(1)
                            2 -> StaggeredGridCells.Fixed(2)
                            3 -> StaggeredGridCells.Fixed(3)
                            4 -> StaggeredGridCells.Fixed(4)
                            else -> StaggeredGridCells.Adaptive(minSize = 175.dp)
                        }

                        val pinchScaleAnim = remember { Animatable(1f) }
                        var pinchPivot by remember { mutableStateOf(TransformOrigin.Center) }

                        LazyVerticalStaggeredGrid(
                            columns = gridCells,
                            state = gridState,
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 86.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        var isPinching = false
                                        var currentZoom = 1f
                                        do {
                                            val event = awaitPointerEvent()
                                            val downCount = event.changes.count { it.pressed }
                                            if (downCount >= 2) {
                                                if (!isPinching) {
                                                    isPinching = true
                                                    currentZoom = 1f
                                                }
                                                val zoom = event.calculateZoom()
                                                currentZoom = (currentZoom * zoom).coerceIn(0.65f, 1.55f)
                                                scope.launch { pinchScaleAnim.snapTo(currentZoom) }
                                                val centroid = event.calculateCentroid()
                                                if (size.width > 0 && size.height > 0) {
                                                    pinchPivot = TransformOrigin(
                                                        (centroid.x / size.width).coerceIn(0f, 1f),
                                                        (centroid.y / size.height).coerceIn(0f, 1f)
                                                    )
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (isPinching) {
                                            val finalZoom = currentZoom
                                            val cur = if (vm.gridColumnsCount == 0) 2 else vm.gridColumnsCount
                                            if (finalZoom > 1.18f && cur > 1) {
                                                vm.setGridColumns(cur - 1)
                                            } else if (finalZoom < 0.84f && cur < 4) {
                                                vm.setGridColumns(cur + 1)
                                            }
                                            scope.launch {
                                                pinchScaleAnim.animateTo(
                                                    1f,
                                                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                                )
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = pinchScaleAnim.value
                                    scaleY = pinchScaleAnim.value
                                    transformOrigin = pinchPivot
                                    translationY = pullOffset.value.dp.toPx()
                                }
                        ) {
                        itemsIndexed(
                            items = vm.results,
                            key = { _, m -> m.mediaKey },
                            contentType = { _, m -> if (m.isVideo) 1 else 0 }
                        ) { index, media ->
                            val ratio = remember(media.id, media.width, media.height) {
                                if (media.width > 0 && media.height > 0) {
                                    (media.width.toFloat() / media.height.toFloat()).coerceIn(0.55f, 1.6f)
                                } else {
                                    when ((media.id.hashCode() and 0x7FFFFFFF) % 3) {
                                        0 -> 3f / 4f
                                        1 -> 2f / 3f
                                        else -> 1f
                                    }
                                }
                            }

                            MediaCard(
                                media = media,
                                aspectRatio = ratio,
                                isFavorite = vm.isFavorite(media),
                                quality = vm.imageQuality,
                                onFavoriteClick = { vm.toggleFavorite(media) },
                                onClick = { vm.openFullscreen(vm.results, index) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(220, easing = Motion.EmphasizedDecelerate),
                                    placementSpec = Motion.spatialDefault(),
                                    fadeOutSpec = null
                                )
                            )
                        }

                        item(span = StaggeredGridItemSpan.FullLine, key = "feed_footer", contentType = 2) {
                            FeedFooter(
                                loadingMore = vm.loadingMore,
                                loadMoreError = vm.loadMoreError,
                                reachedEnd = !vm.hasMore && vm.results.isNotEmpty(),
                                lang = lang,
                                onRetry = { vm.retryLoadMore() }
                            )
                        }
                    }
                }

                SleekTopProgressIndicator(
                    isRefreshing = vm.isRefreshing,
                    pullFraction = { if (pullRefreshState.isAnimating) 0f else pullRefreshState.distanceFraction },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
        }
    }

        Surface(
            onClick = { searchExpanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(56.dp)
                .bouncyPress()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = if (vm.query.isNotBlank()) vm.query else Strings.searchPlaceholder(lang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (vm.query.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (vm.isIncognito) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = Strings.incognitoMode(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { vm.toggleIncognito() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (vm.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Incognito",
                        tint = if (vm.isIncognito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (vm.query.isNotBlank()) {
                    IconButton(
                        onClick = {
                            localQuery = TextFieldValue("")
                            vm.clearTagSuggestions()
                            vm.search(vm.source, "", vm.safeMode)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = Strings.closeBtn(lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        fun submitSearch(raw: String) {
            val q = raw.trim().replace(Regex("\\s+"), " ")
            localQuery = TextFieldValue(q, TextRange(q.length))
            vm.search(vm.source, q, vm.safeMode)
            searchExpanded = false
            vm.clearTagSuggestions()
        }

        fun applySuggestion(value: String, submit: Boolean) {
            val text = localQuery.text
            val range = tokenRangeAt(text, localQuery.selection.start)
            val token = text.substring(range.first, range.last)
            val operator = token.takeWhile { it == '-' || it == '~' || it == '+' }
            val before = text.substring(0, range.first)
            val after = text.substring(range.last).trimStart()
            val replaced = before + operator + value
            val full = if (after.isEmpty()) "$replaced " else "$replaced $after"
            if (submit) {
                submitSearch(full)
            } else {
                localQuery = TextFieldValue(full, TextRange(replaced.length + 1))
                vm.clearTagSuggestions()
            }
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = fadeIn(tween(200, easing = Motion.EmphasizedDecelerate)) +
                slideInVertically(tween(260, easing = Motion.EmphasizedDecelerate)) { -it / 16 },
            exit = fadeOut(tween(160, easing = Motion.EmphasizedAccelerate)) +
                slideOutVertically(tween(160, easing = Motion.EmphasizedAccelerate)) { -it / 24 },
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                searchExpanded = false
                                localQuery = TextFieldValue(vm.query, TextRange(vm.query.length))
                                vm.clearTagSuggestions()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = Strings.closeBtn(lang),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            BasicTextField(
                                value = localQuery,
                                onValueChange = {
                                    val previous = localQuery
                                    localQuery = it
                                    if (it.text != previous.text || it.selection != previous.selection) {
                                        val range = tokenRangeAt(it.text, it.selection.start)
                                        val token = it.text.substring(range.first, range.last)
                                        val core = token.trimStart('-', '~', '+')
                                        if (core.length >= 2 && !core.contains(':')) {
                                            vm.fetchTagSuggestions(core)
                                        } else {
                                            vm.clearTagSuggestions()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { submitSearch(localQuery.text) }),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (localQuery.text.isEmpty()) {
                                            Text(
                                                text = Strings.searchPlaceholder(lang),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            IconButton(onClick = { vm.toggleIncognito() }) {
                                Icon(
                                    imageVector = if (vm.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Incognito",
                                    tint = if (vm.isIncognito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (localQuery.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    localQuery = TextFieldValue("")
                                    vm.clearTagSuggestions()
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = Strings.closeBtn(lang),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { submitSearch(localQuery.text) }) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = vm.suggestionsLoading,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(160))
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(3.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (vm.tagSuggestions.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            Strings.tagSuggestions(lang),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    vm.tagSuggestions.forEach { suggestion ->
                                        Surface(
                                            onClick = { applySuggestion(suggestion.value, submit = false) },
                                            color = Color.Transparent,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val classified = remember(suggestion.value, suggestion.type) { TagClassifier.classify(suggestion.value, suggestion.type) }
                                                val category = classified.category
                                                val isDark = LocalIsDarkTheme.current
                                                val catColor = category.contentColor(isDark) ?: MaterialTheme.colorScheme.primary
                                                val catBg = category.containerColor(isDark) ?: MaterialTheme.colorScheme.surfaceContainerHighest

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = catBg,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                category.icon,
                                                                null,
                                                                tint = catColor,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                                        Text(
                                                            text = suggestion.value,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = catColor,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (suggestion.count > 0) "${category.displayName} · ${formatCompactCount(suggestion.count)}" else category.displayName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { applySuggestion(suggestion.value, submit = true) },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Search,
                                                        contentDescription = "Search",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        val historyFilter = localQuery.text.trim().lowercase()
                        val visibleHistory = remember(vm.searchHistory, historyFilter) {
                            if (historyFilter.isEmpty()) vm.searchHistory
                            else vm.searchHistory.filter { it.lowercase().contains(historyFilter) && !it.equals(historyFilter, ignoreCase = true) }
                        }
                        if (visibleHistory.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                Strings.recentSearches(lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        TextButton(
                                            onClick = { vm.clearHistory() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(Strings.clearAll(lang), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    visibleHistory.take(8).forEach { hist ->
                                        Surface(
                                            onClick = { submitSearch(hist) },
                                            color = Color.Transparent,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.History,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(14.dp))
                                                    Text(
                                                        hist,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val text = hist + " "
                                                        localQuery = TextFieldValue(text, TextRange(text.length))
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.NorthWest,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { vm.removeFromHistory(hist) },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = Strings.closeBtn(lang),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }

        if (showSourceSheet) {
            SourceSelectionSheet(
                currentSource = vm.source,
                sources = vm.availableSources,
                customSources = vm.customSources.filter { it.enabled },
                lang = lang,
                onSelect = { selectedSource ->
                    vm.selectSource(selectedSource)
                },
                onDismiss = { showSourceSheet = false }
            )
        }

        if (showFilterSheet) {
            FilterSelectionBottomSheet(
                vm = vm,
                lang = lang,
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun HeartBurstOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return
    val animScale = remember { Animatable(0.2f) }
    val animAlpha = remember { Animatable(1f) }
    LaunchedEffect(visible) {
        animScale.animateTo(
            targetValue = 1.35f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        animAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220)
        )
        onAnimationEnd()
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    alpha = animAlpha.value
                }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    media: RemoteMedia,
    aspectRatio: Float,
    isFavorite: Boolean,
    quality: ImageQuality,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showHeartBurst by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = Motion.spatialFast(),
        label = "mediaCardScale"
    )

    var useFallback by remember(media.mediaKey) { mutableStateOf(false) }
    var isError by remember(media.mediaKey) { mutableStateOf(false) }
    var retryKey by remember(media.mediaKey) { mutableIntStateOf(0) }

    val imageModel = remember(media.mediaKey, useFallback, quality, retryKey) {
        val targetUrl = if (useFallback) {
            media.preview.ifBlank { media.gridImageUrl(false) }
        } else {
            media.gridImageUrl(quality != ImageQuality.SAVER)
        }
        ImageRequest.Builder(context)
            .data(targetUrl)
            .crossfade(180)
            .allowHardware(true)
            .setParameter("retry", retryKey, memoryCacheKey = null)
            .build()
    }
    val description = remember(media.mediaKey) {
        "${media.source}: ${media.tagList.take(4).joinToString(", ")}"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = null,
                onClick = {
                    if (isError) {
                        isError = false
                        retryKey++
                    } else {
                        onClick()
                    }
                },
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!isFavorite) {
                        onFavoriteClick()
                    }
                    showHeartBurst = true
                }
            )
            .semantics { contentDescription = description }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Error -> {
                            if (!useFallback && media.preview.isNotBlank() && media.preview != media.gridImageUrl(quality != ImageQuality.SAVER)) {
                                useFallback = true
                            } else {
                                isError = true
                            }
                        }
                        is AsyncImagePainter.State.Success -> isError = false
                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )

            if (media.isVideo || media.isGif) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.78f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    if (media.isVideo) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "VIDEO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            Icons.Rounded.Gif,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            val favScale = remember { Animatable(1f) }
            var lastFavorite by remember(media.mediaKey) { mutableStateOf(isFavorite) }
            LaunchedEffect(isFavorite) {
                if (isFavorite != lastFavorite) {
                    lastFavorite = isFavorite
                    if (isFavorite) {
                        favScale.snapTo(0.7f)
                        favScale.animateTo(1f, Motion.spatialFast())
                    }
                }
            }
            val favContainer by animateColorAsState(
                if (isFavorite) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                Motion.effectsDefault(),
                label = "favContainer"
            )
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onFavoriteClick()
                },
                shape = CircleShape,
                color = favContainer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = favScale.value
                        scaleY = favScale.value
                    }
                    .semantics { stateDescription = if (isFavorite) "favorite" else "" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val isRealbooru = media.sourceId.equals("realbooru", ignoreCase = true) || media.url.contains("realbooru.com")
            if (media.score > 0 && !isRealbooru) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatCompactCount(media.score),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = media.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            HeartBurstOverlay(
                visible = showHeartBurst,
                onAnimationEnd = { showHeartBurst = false }
            )
        }
    }
}

@Composable
private fun SkeletonGrid(columnsSetting: Int) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = Motion.StandardEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonPulse"
    )
    val cells = when (columnsSetting) {
        in 1..4 -> StaggeredGridCells.Fixed(columnsSetting)
        else -> StaggeredGridCells.Adaptive(minSize = 175.dp)
    }
    val ratios = remember { listOf(0.75f, 1f, 0.66f, 0.8f, 1.2f, 0.7f, 0.9f, 0.62f, 1f, 0.78f, 0.7f, 1.1f) }
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    LazyVerticalStaggeredGrid(
        columns = cells,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 86.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "loading" }
    ) {
        items(ratios.size) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratios[index])
                    .graphicsLayer { alpha = pulse }
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun FeedFooter(
    loadingMore: Boolean,
    loadMoreError: Boolean,
    reachedEnd: Boolean,
    lang: AppLanguage,
    onRetry: () -> Unit
) {
    AnimatedContent(
        targetState = when {
            loadMoreError -> 2
            loadingMore -> 1
            reachedEnd -> 3
            else -> 0
        },
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        contentAlignment = Alignment.Center,
        label = "feedFooter",
        modifier = Modifier.fillMaxWidth()
    ) { state ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                1 -> CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Strings.loadMoreFailed(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onRetry, shape = CircleShape) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.retryBtn(lang))
                    }
                }
                3 -> Text(
                    text = Strings.endOfResults(lang),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyResultActions(vm: GalleryViewModel, lang: AppLanguage) {
    val query = vm.query.trim()
    val plainTokens = remember(query) {
        query.split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    val canJoin = plainTokens.size in 2..4 && plainTokens.none { it.contains(':') || it.startsWith("-") || it.startsWith("~") || it.contains('*') }
    val hasFilters = vm.safeMode || vm.excludeSafe || vm.noAi || vm.selectedContentTypes.isNotEmpty()
    val canWiden = vm.source != BooruRepository.SOURCE_ALL && query.isNotEmpty()
    if (!canJoin && !hasFilters && !canWiden) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (canJoin) {
            val joined = plainTokens.joinToString("_")
            SuggestionChip(
                onClick = { vm.search(vm.source, joined, vm.safeMode) },
                label = { Text(Strings.searchAsOneTag(lang).format(joined), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.JoinInner, null, modifier = Modifier.size(SuggestionChipDefaults.IconSize)) },
                shape = CircleShape
            )
        }
        if (canWiden) {
            SuggestionChip(
                onClick = { vm.selectSource(BooruRepository.SOURCE_ALL) },
                label = { Text(Strings.searchInAllSources(lang)) },
                icon = { Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(SuggestionChipDefaults.IconSize)) },
                shape = CircleShape
            )
        }
        if (hasFilters) {
            SuggestionChip(
                onClick = {
                    vm.applyAllFilters(
                        contentTypes = emptySet(),
                        sortOrder = vm.sortOrder,
                        safeMode = false,
                        excludeSafe = false,
                        noAi = false
                    )
                },
                label = { Text(Strings.resetFilters(lang)) },
                icon = { Icon(Icons.Rounded.FilterAltOff, null, modifier = Modifier.size(SuggestionChipDefaults.IconSize)) },
                shape = CircleShape
            )
        }
    }
}

private const val PREFETCH_AHEAD = 14

internal fun tokenRangeAt(text: String, cursor: Int): IntRange {
    val c = cursor.coerceIn(0, text.length)
    var start = c
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    var end = c
    while (end < text.length && !text[end].isWhitespace()) end++
    return start..end
}

internal fun formatCompactCount(value: Int): String = when {
    value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000f).replace(".0M", "M")
    value >= 10_000 -> "${value / 1000}k"
    value >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", value / 1000f).replace(".0k", "k")
    else -> value.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionSheet(
    currentSource: String,
    sources: List<String> = BooruRepository.AVAILABLE_SOURCES,
    customSources: List<CustomBooruSource> = emptyList(),
    lang: AppLanguage,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = Strings.selectSourceTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            sources.forEach { src ->
                val isSelected = currentSource == src || (customSources.find { it.key == src || it.id == src }?.let { it.key == currentSource || it.id == currentSource } ?: false)
                val icon = when (src) {
                    BooruRepository.SOURCE_ALL -> Icons.Rounded.AutoAwesome
                    BooruRepository.SOURCE_RULE34 -> Icons.Rounded.Explicit
                    BooruRepository.SOURCE_GELBOORU -> Icons.Rounded.Image
                    BooruRepository.SOURCE_REALBOORU -> Icons.Rounded.VideoLibrary
                    BooruRepository.SOURCE_XBOORU -> Icons.Rounded.PhotoLibrary
                    BooruRepository.SOURCE_TBIB -> Icons.Rounded.Public
                    BooruRepository.SOURCE_YANDE -> Icons.Rounded.Collections
                    BooruRepository.SOURCE_KONACHAN -> Icons.Rounded.Wallpaper
                    BooruRepository.SOURCE_SAFEBOORU -> Icons.Rounded.Shield
                    else -> Icons.Rounded.Language
                }

                Surface(
                    onClick = {
                        onSelect(src)
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .bouncyPress()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = if (src == BooruRepository.SOURCE_ALL) Strings.sourceRecommendations(lang) else BooruRepository.getSourceDisplayName(src, customSources),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterOptionButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "filterBtnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "filterBtnContent"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .height(46.dp)
            .bouncyPress()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(260, easing = LinearOutSlowInEasing)) +
                    expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        expandFrom = Alignment.Start
                    ),
                exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                    shrinkHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        shrinkTowards = Alignment.Start
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = contentColor
                    )
                    Spacer(Modifier.width(5.dp))
                }
            }
            if (icon != null && !selected) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = contentColor
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSelectionBottomSheet(
    vm: GalleryViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var tempContentTypes by remember { mutableStateOf(vm.selectedContentTypes) }
    val isRealbooru = vm.source.equals("realbooru", ignoreCase = true) || vm.source.equals(BooruRepository.SOURCE_REALBOORU, ignoreCase = true)
    var tempSortOrder by remember {
        val initial = if (isRealbooru && vm.sortOrder == SortOrder.SCORE) SortOrder.NEWEST else vm.sortOrder
        mutableStateOf(initial)
    }
    var tempSafeMode by remember { mutableStateOf(vm.safeMode) }
    var tempExcludeSafe by remember { mutableStateOf(vm.excludeSafe) }
    var tempNoAi by remember { mutableStateOf(vm.noAi) }
    var tempRecRatio by remember { mutableFloatStateOf(vm.recommendationRatio) }
    val filterPagerState = rememberPagerState(initialPage = 0) { 2 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.filtersAndSorting(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = {
                        tempContentTypes = emptySet()
                        tempSortOrder = SortOrder.NEWEST
                        tempSafeMode = false
                        tempExcludeSafe = false
                        tempNoAi = false
                        tempRecRatio = 0.5f
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = Strings.resetFilters(lang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Pair(if (lang == AppLanguage.RUSSIAN) "Контент и рейтинг" else "Content & Rating", Icons.Rounded.Category),
                        Pair(if (lang == AppLanguage.RUSSIAN) "Сортировка и лента" else "Sorting & Feed", Icons.Rounded.AutoAwesome)
                    )
                    val targetFilterIndex = if (filterPagerState.isScrollInProgress) filterPagerState.targetPage else filterPagerState.currentPage
                    tabs.forEachIndexed { index, (title, icon) ->
                        val isSelected = targetFilterIndex == index
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            label = "filterTabBg"
                        )
                        val fg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            label = "filterTabFg"
                        )
                        Surface(
                            onClick = {
                                scope.launch {
                                    filterPagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            contentColor = fg,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .bouncyPress()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = tween(260, easing = LinearOutSlowInEasing)) +
                                        expandHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = 0.78f,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            expandFrom = Alignment.Start
                                        ),
                                    exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                        shrinkHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = 0.88f,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            shrinkTowards = Alignment.Start
                                        )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                    }
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            HorizontalPager(
                state = filterPagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) { page ->
                if (page == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PermMedia, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = Strings.contentTypeTitle(lang),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val types = listOf(
                                            Triple(ContentType.PHOTOS, Strings.contentTypePhotos(lang), Icons.Rounded.Image),
                                            Triple(ContentType.VIDEOS, Strings.contentTypeVideos(lang), Icons.Rounded.Videocam),
                                            Triple(ContentType.GIFS, Strings.contentTypeGifs(lang), Icons.Rounded.Gif)
                                        )
                                        types.forEach { (type, label, icon) ->
                                            val selected = tempContentTypes.contains(type)
                                            FilterOptionButton(
                                                selected = selected,
                                                onClick = {
                                                    tempContentTypes = if (selected) tempContentTypes - type else tempContentTypes + type
                                                },
                                                label = label,
                                                icon = icon,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = Strings.allRatings(lang),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isAllRating = !tempSafeMode && !tempExcludeSafe
                                        FilterOptionButton(
                                            selected = isAllRating,
                                            onClick = {
                                                tempSafeMode = false
                                                tempExcludeSafe = false
                                            },
                                            label = Strings.allRatings(lang),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterOptionButton(
                                            selected = tempExcludeSafe,
                                            onClick = {
                                                tempExcludeSafe = !tempExcludeSafe
                                                if (tempExcludeSafe) tempSafeMode = false
                                            },
                                            label = Strings.only18Badge(lang),
                                            icon = Icons.Rounded.Explicit,
                                            selectedContainerColor = MaterialTheme.colorScheme.error,
                                            selectedContentColor = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterOptionButton(
                                            selected = tempSafeMode,
                                            onClick = {
                                                tempSafeMode = !tempSafeMode
                                                if (tempSafeMode) tempExcludeSafe = false
                                            },
                                            label = Strings.safeModeBadge(lang),
                                            icon = Icons.Rounded.Shield,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tempNoAi = !tempNoAi }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (tempNoAi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (tempNoAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = Strings.noAiBadge(lang),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.RUSSIAN) "Скрывать арты, созданные нейросетями" else "Hide AI-generated artworks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = tempNoAi,
                                        onCheckedChange = { tempNoAi = it }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == AppLanguage.RUSSIAN) "Сортировка" else "Sort by",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val sortOrders = listOfNotNull(
                                            Triple(SortOrder.NEWEST, Strings.sortNewest(lang), Icons.Rounded.Schedule),
                                            if (!isRealbooru) Triple(SortOrder.SCORE, Strings.sortScore(lang), Icons.Rounded.Star) else null,
                                            Triple(SortOrder.RANDOM, Strings.sortRandom(lang), Icons.Rounded.Shuffle)
                                        )
                                        sortOrders.forEach { (order, label, icon) ->
                                            val selected = (tempSortOrder == order)
                                            FilterOptionButton(
                                                selected = selected,
                                                onClick = { tempSortOrder = order },
                                                label = label,
                                                icon = icon,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            if (vm.query.isBlank()) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = Strings.feedRecommendedOnly(lang),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                Triple(0.0f, Strings.feedNewestOnly(lang), tempRecRatio <= 0.15f),
                                                Triple(0.5f, Strings.feedBalanced(lang), tempRecRatio in 0.35f..0.65f),
                                                Triple(1.0f, Strings.feedRecommendedOnly(lang), tempRecRatio >= 0.85f)
                                            ).forEach { (presetVal, label, active) ->
                                                FilterOptionButton(
                                                    selected = active,
                                                    onClick = { tempRecRatio = presetVal },
                                                    label = label,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Slider(
                                            value = tempRecRatio,
                                            onValueChange = { tempRecRatio = it },
                                            valueRange = 0f..1f,
                                            steps = 3,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val ratioChanged = kotlin.math.abs(tempRecRatio - vm.recommendationRatio) > 0.01f
                    val filtersChanged = tempContentTypes != vm.selectedContentTypes || tempSortOrder != vm.sortOrder ||
                        tempSafeMode != vm.safeMode || tempExcludeSafe != vm.excludeSafe || tempNoAi != vm.noAi
                    vm.updateRecommendationRatio(tempRecRatio)
                    if (ratioChanged && !filtersChanged && vm.query.isBlank()) vm.refresh(isPull = false)
                    vm.applyAllFilters(
                        contentTypes = tempContentTypes,
                        sortOrder = tempSortOrder,
                        safeMode = tempSafeMode,
                        excludeSafe = tempExcludeSafe,
                        noAi = tempNoAi
                    )
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bouncyPress()
            ) {
                Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.applyFilters(lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SleekTopProgressIndicator(
    isRefreshing: Boolean,
    pullFraction: () -> Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val currentPullFraction by rememberUpdatedState(pullFraction)
    val pull = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { currentPullFraction().coerceIn(0f, 1f) }.collectLatest { target ->
            if (target == 0f) {
                pull.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            } else {
                pull.snapTo(target)
            }
        }
    }
    val refreshAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0f,
        animationSpec = tween(if (isRefreshing) 260 else 420, easing = FastOutSlowInEasing),
        label = "refreshAlpha"
    )
    val isVisible by remember { derivedStateOf { pull.value > 0.005f || refreshAlpha > 0.005f } }
    if (!isVisible) return

    val sweepEasing = remember { CubicBezierEasing(0.65f, 0f, 0.35f, 1f) }
    val sweep = rememberInfiniteTransition(label = "refreshSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .height(2.dp)
            .drawBehind {
                val width = size.width
                val height = size.height
                val cornerRadius = CornerRadius(height / 2f, height / 2f)

                val p = pull.value
                val pullAlpha = (1f - refreshAlpha) * (0.25f + 0.75f * p)
                if (p > 0f && pullAlpha > 0f) {
                    val activeWidth = (width * LinearOutSlowInEasing.transform(p)).coerceAtLeast(height)
                    drawRoundRect(
                        color = primary.copy(alpha = pullAlpha),
                        topLeft = Offset((width - activeWidth) / 2f, 0f),
                        size = Size(activeWidth, height),
                        cornerRadius = cornerRadius
                    )
                }

                if (refreshAlpha > 0f) {
                    drawRoundRect(
                        color = primary.copy(alpha = 0.12f * refreshAlpha),
                        cornerRadius = cornerRadius
                    )
                    val t = sweep.value
                    val segment = 0.18f + 0.22f * kotlin.math.sin(t * Math.PI.toFloat())
                    val start = sweepEasing.transform(t) * (1f + segment) - segment
                    val left = (start * width).coerceIn(0f, width)
                    val right = ((start + segment) * width).coerceIn(0f, width)
                    if (right - left > 0.5f) {
                        drawRoundRect(
                            color = primary.copy(alpha = refreshAlpha),
                            topLeft = Offset(left, 0f),
                            size = Size(right - left, height),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
    )
}
