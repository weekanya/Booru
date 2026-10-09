@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.foundation.border
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
            var maxLane = 0
            for (item in visible) if (item.lane > maxLane) maxLane = item.lane
            val lanes = maxLane + 1
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
                .padding(top = 64.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectedBarButton(
                    position = GroupPosition.Leading,
                    onClick = { showSourceSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedContent(
                        targetState = vm.source,
                        transitionSpec = {
                            (fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.92f))
                                .togetherWith(fadeOut(Motion.effectsFast()))
                        },
                        modifier = Modifier.weight(1f),
                        label = "sourceButtonLabel"
                    ) { src ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = sourceIcon(src),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = vm.getSourceDisplayName(src),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                val activeFilterCount = (if (vm.safeMode || vm.excludeSafe) 1 else 0) +
                        (if (vm.noAi) 1 else 0) +
                        (if (vm.sortOrder != SortOrder.NEWEST) 1 else 0) +
                        (if (vm.selectedContentTypes.isNotEmpty()) 1 else 0)

                val filterBtnContainer by animateColorAsState(
                    targetValue = if (activeFilterCount > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = Motion.effectsDefault(),
                    label = "filterBtnContainer"
                )
                val filterBtnContent by animateColorAsState(
                    targetValue = if (activeFilterCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = Motion.effectsDefault(),
                    label = "filterBtnContent"
                )
                ConnectedBarButton(
                    position = GroupPosition.Trailing,
                    onClick = { showFilterSheet = true },
                    containerColor = filterBtnContainer,
                    contentColor = filterBtnContent
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Strings.filtersButton(lang),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AnimatedVisibility(
                        visible = activeFilterCount > 0,
                        enter = fadeIn(Motion.effectsDefault()) + expandHorizontally(Motion.spatialDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.4f),
                        exit = fadeOut(Motion.effectsFast()) + shrinkHorizontally(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)) + scaleOut(Motion.effectsFast(), targetScale = 0.4f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = activeFilterCount,
                                    transitionSpec = {
                                        val up = targetState > initialState
                                        (slideInVertically(Motion.spatialFast()) { if (up) it else -it } + fadeIn(Motion.effectsDefault()))
                                            .togetherWith(slideOutVertically(Motion.effectsFast()) { if (up) -it else it } + fadeOut(Motion.effectsFast()))
                                    },
                                    label = "filterCount"
                                ) { count ->
                                    Text(
                                        text = "${count.coerceAtLeast(1)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
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
                    shape = ShapeTokens.LargeIncreased,
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
                                Text(
                                    Strings.closeBtn(lang),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (vm.isAuthError)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
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
                            1 -> StaggeredGridCells.Fixed(adaptiveColumns(1))
                            2 -> StaggeredGridCells.Fixed(adaptiveColumns(2))
                            3 -> StaggeredGridCells.Fixed(adaptiveColumns(3))
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
                                                val visualScale = (1f + (currentZoom - 1f) * 0.45f).coerceIn(0.88f, 1.12f)
                                                scope.launch { pinchScaleAnim.snapTo(visualScale) }
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
                                            } else if (finalZoom < 0.84f && cur < 3) {
                                                vm.setGridColumns(cur + 1)
                                            }
                                            scope.launch {
                                                pinchScaleAnim.animateTo(
                                                    1f,
                                                    Motion.spatialDefault()
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
                                    fadeInSpec = Motion.effectsDefault(),
                                    placementSpec = null,
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

        val pressSource1 = remember { MutableInteractionSource() }
        val searchPressed by pressSource1.collectIsPressedAsState()
        val searchOuter by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (searchPressed) 16.dp else 28.dp,
            animationSpec = Motion.spatialFast(),
            label = "searchOuter"
        )
        val searchInner by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (searchPressed) 4.dp else 8.dp,
            animationSpec = Motion.spatialFast(),
            label = "searchInner"
        )
        Surface(
            onClick = { searchExpanded = true },
            shape = RoundedCornerShape(topStart = searchOuter, topEnd = searchOuter, bottomStart = searchInner, bottomEnd = searchInner),
            interactionSource = pressSource1,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 8.dp),
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
                IconButton(
                    onClick = { vm.toggleIncognito() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (vm.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Incognito",
                        tint = if (vm.isIncognito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
            enter = fadeIn(Motion.effectsDefault()),
            exit = fadeOut(Motion.effectsFast()),
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
                    val hasHistory = vm.searchHistory.isNotEmpty()
                    val fieldBottom by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (hasHistory) 6.dp else 28.dp,
                        animationSpec = Motion.spatialDefault(),
                        label = "searchFieldBottom"
                    )
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = fieldBottom, bottomEnd = fieldBottom),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 6.dp, end = 8.dp),
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
                            IconButton(onClick = { vm.toggleIncognito() }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = if (vm.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Incognito",
                                    tint = if (vm.isIncognito) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = hasHistory,
                        enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.spatialDefault()),
                        exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.spatialDefault())
                    ) {
                        AnimatedConfirmDeleteButton(
                            onConfirmed = { vm.clearHistory() },
                            lang = lang,
                            initialIcon = Icons.Rounded.DeleteSweep,
                            initialText = Strings.tr(lang, "Clear history", "Очистить историю", "履歴を消去", "清除历史", "기록 지우기", "مسح السجل"),
                            confirmText = Strings.confirmDeleteAction(lang),
                            height = 44.dp,
                            contentPadding = 16.dp,
                            fillContentWidth = true,
                            idleContainerColor = MaterialTheme.colorScheme.errorContainer,
                            idleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    androidx.compose.animation.AnimatedVisibility(
                        visible = vm.suggestionsLoading,
                        enter = fadeIn(Motion.effectsDefault()),
                        exit = fadeOut(Motion.effectsFast())
                    ) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
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
                            SearchSectionHeader(
                                icon = Icons.Rounded.AutoAwesome,
                                title = Strings.tagSuggestions(lang)
                            )
                            val suggestionCount = vm.tagSuggestions.size
                            vm.tagSuggestions.forEachIndexed { index, suggestion ->
                                val classified = remember(suggestion.value, suggestion.type) { TagClassifier.classify(suggestion.value, suggestion.type) }
                                val category = classified.category
                                val isDark = LocalIsDarkTheme.current
                                val catColor = category.contentColor(isDark) ?: MaterialTheme.colorScheme.primary
                                val catBg = category.containerColor(isDark) ?: MaterialTheme.colorScheme.secondaryContainer
                                SearchListItem(
                                    shape = segmentedListShape(index, suggestionCount),
                                    onClick = { applySuggestion(suggestion.value, submit = false) },
                                    icon = category.icon,
                                    iconContainer = catBg,
                                    iconTint = catColor,
                                    title = suggestion.value,
                                    titleColor = catColor,
                                    supporting = if (suggestion.count > 0) "${category.displayName} · ${formatCompactCount(suggestion.count)}" else category.displayName
                                ) {
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
                            Spacer(Modifier.height(16.dp))
                        }

                        val historyFilter = localQuery.text.trim().lowercase()
                        val visibleHistory = remember(vm.searchHistory, historyFilter) {
                            if (historyFilter.isEmpty()) vm.searchHistory
                            else vm.searchHistory.filter { it.lowercase().contains(historyFilter) && !it.equals(historyFilter, ignoreCase = true) }
                        }
                        if (visibleHistory.isNotEmpty()) {
                            SearchSectionHeader(
                                icon = Icons.Rounded.History,
                                title = Strings.recentSearches(lang)
                            )
                            val shownHistory = visibleHistory.take(8)
                            shownHistory.forEachIndexed { index, hist ->
                                SearchListItem(
                                    shape = segmentedListShape(index, shownHistory.size),
                                    onClick = { submitSearch(hist) },
                                    icon = Icons.Rounded.History,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = hist
                                ) {
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
private fun SearchSectionHeader(
    icon: ImageVector,
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(start = 4.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun SearchListItem(
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    supporting: String? = null,
    trailing: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ShapeTokens.Medium,
                color = iconContainer,
                contentColor = iconTint,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing()
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
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(1f) }
    val heartLift = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.5f) }
    val ringAlpha = remember { Animatable(0.55f) }
    val tilt = remember { (-14..14).random().toFloat() }
    val ringColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(visible) {
        launch {
            ringScale.animateTo(1.9f, tween(420, easing = Motion.EmphasizedDecelerate))
        }
        launch {
            ringAlpha.animateTo(0f, tween(420, easing = LinearOutSlowInEasing))
        }
        heartScale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        kotlinx.coroutines.delay(140)
        launch { heartLift.animateTo(-36f, tween(300, easing = Motion.EmphasizedAccelerate)) }
        launch { heartScale.animateTo(1.18f, tween(300, easing = Motion.EmphasizedAccelerate)) }
        heartAlpha.animateTo(0f, tween(300, easing = Motion.EmphasizedAccelerate))
        onAnimationEnd()
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = ringScale.value
                    scaleY = ringScale.value
                    alpha = ringAlpha.value
                }
                .border(3.dp, ringColor, CircleShape)
        )
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                    alpha = heartAlpha.value
                    rotationZ = tilt * (1f - heartScale.value.coerceIn(0f, 1f) * 0.6f)
                    translationY = heartLift.value.dp.toPx()
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
        shape = ShapeTokens.LargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(ShapeTokens.LargeIncreased)
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
        in 1..3 -> StaggeredGridCells.Fixed(columnsSetting)
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
                    .clip(ShapeTokens.LargeIncreased)
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
        transitionSpec = { fadeIn(Motion.effectsDefault()) togetherWith fadeOut(Motion.effectsFast()) },
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
                1 -> LoadingIndicator(modifier = Modifier.size(36.dp))
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
    val sheetState = rememberExpandedSheetState()
    val scope = rememberCoroutineScope()
    var pendingSource by remember { mutableStateOf<String?>(null) }
    val columns = if (LocalWindowWidthClass.current == WindowWidthClass.Compact) 2 else 3

    fun isCustom(src: String): Boolean = customSources.any { it.id == src || it.key == src }

    fun isSelected(src: String): Boolean = pendingSource?.let { it == src }
        ?: (currentSource == src || (customSources.find { it.key == src || it.id == src }?.let { it.key == currentSource || it.id == currentSource } ?: false))

    fun select(src: String) {
        if (pendingSource == null) {
            pendingSource = src
            onSelect(src)
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    fun label(src: String): String =
        if (src == BooruRepository.SOURCE_ALL) Strings.sourceRecommendations(lang) else BooruRepository.getSourceDisplayName(src, customSources)

    val builtInRows = buildList {
        sources.firstOrNull { it == BooruRepository.SOURCE_ALL }?.let { add(listOf(it)) }
        addAll(sources.filter { it != BooruRepository.SOURCE_ALL && !isCustom(it) }.chunked(columns))
    }
    val customRows = sources.filter { isCustom(it) }.chunked(columns)

    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = Strings.selectSourceTitle(lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp)
                )

                val grid: @Composable (List<List<String>>) -> Unit = { rows ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.selectableGroup()
                    ) {
                        rows.forEachIndexed { r, row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEachIndexed { c, src ->
                                    val featured = src == BooruRepository.SOURCE_ALL
                                    SourceTile(
                                        label = label(src),
                                        icon = sourceIcon(src),
                                        selected = isSelected(src),
                                        onClick = { select(src) },
                                        supporting = if (featured) Strings.sourceRecommendationsDesc(lang) else null,
                                        featured = featured,
                                        outerTopStart = r == 0 && c == 0,
                                        outerTopEnd = r == 0 && c == row.lastIndex,
                                        outerBottomStart = r == rows.lastIndex && c == 0,
                                        outerBottomEnd = r == rows.lastIndex && c == row.lastIndex,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                grid(builtInRows)

                if (customRows.isNotEmpty()) {
                    SegmentedSectionHeader(
                        title = Strings.customSourcesTitle(lang),
                        icon = Icons.Rounded.Language,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    grid(customRows)
                }
            }
        }
    }
}

private fun sourceIcon(src: String): ImageVector = when (src) {
    BooruRepository.SOURCE_ALL -> Icons.Rounded.AutoAwesome
    BooruRepository.SOURCE_RULE34 -> Icons.Rounded.Explicit
    BooruRepository.SOURCE_GELBOORU -> Icons.Rounded.Image
    BooruRepository.SOURCE_REALBOORU -> Icons.Rounded.VideoLibrary
    BooruRepository.SOURCE_TBIB -> Icons.Rounded.Public
    BooruRepository.SOURCE_YANDE -> Icons.Rounded.Collections
    BooruRepository.SOURCE_KONACHAN -> Icons.Rounded.Wallpaper
    BooruRepository.SOURCE_SAFEBOORU -> Icons.Rounded.Shield
    else -> Icons.Rounded.Language
}

@Composable
private fun SourceTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    featured: Boolean = false,
    outerTopStart: Boolean = true,
    outerTopEnd: Boolean = true,
    outerBottomStart: Boolean = true,
    outerBottomEnd: Boolean = true
) {
    val height = if (featured) 72.dp else 56.dp
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    @Composable
    fun corner(outer: Boolean, name: String): androidx.compose.ui.unit.Dp {
        val target = when {
            selected -> height / 2
            pressed -> 12.dp
            outer -> 20.dp
            else -> 4.dp
        }
        val value by androidx.compose.animation.core.animateDpAsState(
            targetValue = target,
            animationSpec = Motion.spatialDefault(),
            label = name
        )
        return value
    }

    val shape = RoundedCornerShape(
        topStart = corner(outerTopStart, "sourceTileTopStart"),
        topEnd = corner(outerTopEnd, "sourceTileTopEnd"),
        bottomEnd = corner(outerBottomEnd, "sourceTileBottomEnd"),
        bottomStart = corner(outerBottomStart, "sourceTileBottomStart")
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = Motion.effectsDefault(),
        label = "sourceTileColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = Motion.effectsDefault(),
        label = "sourceTileContent"
    )
    val badgeColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            featured -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = Motion.effectsDefault(),
        label = "sourceTileBadge"
    )
    val badgeContent by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            featured -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = Motion.effectsDefault(),
        label = "sourceTileBadgeContent"
    )
    val morphProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.spatialDefault(),
        label = "sourceTileBadgeMorph"
    )
    val badgeMorph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val badgeSize = if (featured) 44.dp else 36.dp

    Surface(
        selected = selected,
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier.height(height)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(badgeSize)
                    .background(badgeColor, MorphShape(badgeMorph, morphProgress.coerceIn(0f, 1f)))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeContent,
                    modifier = Modifier.size(if (featured) 22.dp else 18.dp)
                )
            }
            Spacer(Modifier.width(if (featured) 14.dp else 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = if (featured) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.5f),
                exit = fadeOut(Motion.effectsFast()) + scaleOut(Motion.effectsFast(), targetScale = 0.5f)
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun ConnectedBarButton(
    position: GroupPosition,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val outer by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (pressed) 12.dp else 20.dp,
        animationSpec = Motion.spatialFast(),
        label = "barButtonOuter"
    )
    val inner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (pressed) 4.dp else 8.dp,
        animationSpec = Motion.spatialFast(),
        label = "barButtonInner"
    )
    val shape = when (position) {
        GroupPosition.Leading -> RoundedCornerShape(topStart = inner, bottomStart = outer, topEnd = inner, bottomEnd = inner)
        GroupPosition.Trailing -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = inner, bottomEnd = outer)
        GroupPosition.Middle -> RoundedCornerShape(inner)
    }
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

enum class GroupPosition { Leading, Middle, Trailing }

fun groupPosition(index: Int, count: Int): GroupPosition = when {
    index == 0 -> GroupPosition.Leading
    index == count - 1 -> GroupPosition.Trailing
    else -> GroupPosition.Middle
}

@Composable
fun GroupPosition.toggleShapes(): ToggleButtonShapes = when (this) {
    GroupPosition.Leading -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    GroupPosition.Middle -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    GroupPosition.Trailing -> ButtonGroupDefaults.connectedTrailingButtonShapes()
}

@Composable
fun FilterSectionIcon(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
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
    position: GroupPosition = GroupPosition.Middle,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        animationSpec = Motion.effectsDefault(),
        label = "filterBtnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = Motion.effectsDefault(),
        label = "filterBtnContent"
    )

    ToggleGroupColors {
        ToggleButton(
            checked = selected,
            onCheckedChange = { onClick() },
            shapes = position.toggleShapes(),
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(animationSpec = Motion.effectsDefault()) +
                        expandHorizontally(
                            animationSpec = Motion.spatialDefault(),
                            expandFrom = Alignment.Start
                        ),
                    exit = fadeOut(animationSpec = Motion.effectsFast()) +
                        shrinkHorizontally(
                            animationSpec = Motion.spatialDefault(),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSelectionBottomSheet(
    vm: GalleryViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberExpandedSheetState()
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

    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
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
                            shape = ShapeTokens.Medium,
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
    
                ToggleGroupColors {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        val tabs = listOf(
                            Pair(Strings.tr(lang, "Content & Rating", "Контент и рейтинг", "コンテンツと評価", "内容与分级", "콘텐츠 및 등급", "المحتوى والتصنيف"), Icons.Rounded.Category),
                            Pair(Strings.tr(lang, "Sorting & Feed", "Сортировка и лента", "並べ替えとフィード", "排序与推送", "정렬 및 피드", "الترتيب والخلاصة"), Icons.Rounded.AutoAwesome)
                        )
                        val targetFilterIndex = if (filterPagerState.isScrollInProgress) filterPagerState.targetPage else filterPagerState.currentPage
                        tabs.forEachIndexed { index, (title, icon) ->
                            ToggleButton(
                                checked = targetFilterIndex == index,
                                onCheckedChange = {
                                    scope.launch {
                                        filterPagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = Motion.spatialDefault()
                                        )
                                    }
                                },
                                shapes = groupPosition(index, tabs.size).toggleShapes(),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
    
                HorizontalPager(
                    state = filterPagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(266.dp)
                ) { page ->
                    if (page == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                                Card(
                                    shape = segmentedListShape(0, 3),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FilterSectionIcon(Icons.Rounded.PermMedia)
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = Strings.contentTypeTitle(lang),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                        ) {
                                            val types = listOf(
                                                Triple(ContentType.PHOTOS, Strings.contentTypePhotos(lang), Icons.Rounded.Image),
                                                Triple(ContentType.VIDEOS, Strings.contentTypeVideos(lang), Icons.Rounded.Videocam),
                                                Triple(ContentType.GIFS, Strings.contentTypeGifs(lang), Icons.Rounded.Gif)
                                            )
                                            FilterOptionButton(
                                                selected = tempContentTypes.isEmpty(),
                                                onClick = { tempContentTypes = emptySet() },
                                                label = Strings.favFilterAll(lang),
                                                position = GroupPosition.Leading,
                                                modifier = Modifier.weight(1f)
                                            )
                                            types.forEachIndexed { typeIndex, (type, label, icon) ->
                                                val selected = tempContentTypes.contains(type)
                                                FilterOptionButton(
                                                    selected = selected,
                                                    onClick = {
                                                        val updated = if (selected) tempContentTypes - type else tempContentTypes + type
                                                        tempContentTypes = if (updated.size == types.size) emptySet() else updated
                                                    },
                                                    position = groupPosition(typeIndex + 1, types.size + 1),
                                                    label = label,
                                                    icon = icon,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
    
                                Card(
                                    shape = segmentedListShape(1, 3),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FilterSectionIcon(Icons.Rounded.Shield)
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = Strings.allRatings(lang),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                        ) {
                                            val isAllRating = !tempSafeMode && !tempExcludeSafe
                                            FilterOptionButton(
                                                selected = isAllRating,
                                                onClick = {
                                                    tempSafeMode = false
                                                    tempExcludeSafe = false
                                                },
                                                label = Strings.allRatings(lang),
                                                position = GroupPosition.Leading,
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
                                                position = GroupPosition.Middle,
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
                                                position = GroupPosition.Trailing,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
    
                                Card(
                                    shape = segmentedListShape(2, 3),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { tempNoAi = !tempNoAi }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                            Text(
                                                text = Strings.noAiBadge(lang),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Switch(
                                            checked = tempNoAi,
                                            onCheckedChange = { tempNoAi = it },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (tempNoAi) Icons.Rounded.Check else Icons.Rounded.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Card(
                                    shape = if (vm.query.isBlank()) segmentedListShape(0, 2) else ShapeTokens.LargeIncreased,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FilterSectionIcon(Icons.AutoMirrored.Rounded.Sort)
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = Strings.tr(lang, "Sort by", "Сортировка", "並べ替え", "排序", "정렬", "ترتيب حسب"),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                        ) {
                                            val sortOrders = listOfNotNull(
                                                Triple(SortOrder.NEWEST, Strings.sortNewest(lang), Icons.Rounded.Schedule),
                                                if (!isRealbooru) Triple(SortOrder.SCORE, Strings.sortScore(lang), Icons.Rounded.Star) else null,
                                                Triple(SortOrder.RANDOM, Strings.sortRandom(lang), Icons.Rounded.Shuffle)
                                            )
                                            sortOrders.forEachIndexed { orderIndex, (order, label, icon) ->
                                                val selected = (tempSortOrder == order)
                                                FilterOptionButton(
                                                    selected = selected,
                                                    onClick = { tempSortOrder = order },
                                                    position = groupPosition(orderIndex, sortOrders.size),
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
                                        shape = segmentedListShape(1, 2),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FilterSectionIcon(Icons.Rounded.AutoAwesome)
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = Strings.feedMixTitle(lang),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                            ) {
                                                listOf(
                                                    Triple(0.0f, Strings.feedNewestOnly(lang), tempRecRatio < 0.25f),
                                                    Triple(0.5f, Strings.feedBalanced(lang), tempRecRatio >= 0.25f && tempRecRatio <= 0.75f),
                                                    Triple(1.0f, Strings.feedRecommendedOnly(lang), tempRecRatio > 0.75f)
                                                ).forEachIndexed { presetIndex, (presetVal, label, active) ->
                                                    FilterOptionButton(
                                                        selected = active,
                                                        onClick = { tempRecRatio = presetVal },
                                                        position = groupPosition(presetIndex, 3),
                                                        label = label,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            var draggingRatio by remember { mutableStateOf(false) }
                                            val shownRatio by animateFloatAsState(
                                                targetValue = tempRecRatio,
                                                animationSpec = if (draggingRatio) snap() else Motion.spatialDefault(),
                                                label = "recRatio"
                                            )
                                            ExpressiveSlider(
                                                value = shownRatio,
                                                onValueChange = {
                                                    draggingRatio = true
                                                    tempRecRatio = it
                                                },
                                                onValueChangeFinished = { draggingRatio = false },
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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
                if (pull.value != 0f) pull.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            } else {
                pull.snapTo(target)
            }
        }
    }
    val refresh = remember { Animatable(0f) }
    LaunchedEffect(isRefreshing) {
        refresh.animateTo(
            targetValue = if (isRefreshing) 1f else 0f,
            animationSpec = tween(if (isRefreshing) 220 else 360, easing = FastOutSlowInEasing)
        )
    }
    val showSweep by remember { derivedStateOf { refresh.value > 0f } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .height(2.dp)
            .graphicsLayer {}
    ) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val p = pull.value
                    val alpha = (1f - refresh.value) * (0.25f + 0.75f * p)
                    if (p <= 0f || alpha <= 0f) return@drawBehind
                    val activeWidth = (size.width * LinearOutSlowInEasing.transform(p)).coerceAtLeast(size.height)
                    drawRoundRect(
                        color = primary.copy(alpha = alpha),
                        topLeft = Offset((size.width - activeWidth) / 2f, 0f),
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
        )
        if (showSweep) {
            RefreshSweep(
                color = primary,
                alpha = { refresh.value },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun RefreshSweep(
    color: Color,
    alpha: () -> Float,
    modifier: Modifier = Modifier
) {
    val easing = remember { CubicBezierEasing(0.65f, 0f, 0.35f, 1f) }
    val phase = rememberInfiniteTransition(label = "refreshSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPhase"
    )
    Box(
        modifier.drawBehind {
            val a = alpha()
            if (a <= 0f) return@drawBehind
            val width = size.width
            val height = size.height
            val cornerRadius = CornerRadius(height / 2f, height / 2f)
            drawRoundRect(color = color.copy(alpha = 0.12f * a), cornerRadius = cornerRadius)
            val t = phase.value
            val segment = 0.18f + 0.22f * kotlin.math.sin(t * Math.PI.toFloat())
            val start = easing.transform(t) * (1f + segment) - segment
            val left = (start * width).coerceIn(0f, width)
            val right = ((start + segment) * width).coerceIn(0f, width)
            if (right - left > 0.5f) {
                drawRoundRect(
                    color = color.copy(alpha = a),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, height),
                    cornerRadius = cornerRadius
                )
            }
        }
    )
}
