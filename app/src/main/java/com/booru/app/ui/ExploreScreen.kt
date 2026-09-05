package com.booru.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.booru.app.BooruRepository
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.SortOrder
import com.booru.app.data.AppLanguage
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
    var localQuery     by remember { mutableStateOf(vm.query) }
    var showSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(vm.query) {
        localQuery = vm.query
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        vm.clearTagSuggestions()
    }

    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            total > 0 && lastVisible >= total - 6 && !vm.loading && !vm.loadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
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

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                item {
                    FilledTonalButton(
                        onClick = { showSourceSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .bouncyPress()
                    ) {
                        Icon(
                            imageVector = when (vm.source) {
                                BooruRepository.SOURCE_ALL -> Icons.Rounded.Layers
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
                }

                item {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalButton(
                            onClick = { showSortMenu = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .bouncyPress()
                        ) {
                            Icon(
                                Icons.Rounded.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            val sortLabel = when (vm.sortOrder) {
                                SortOrder.NEWEST -> Strings.sortNewest(lang)
                                SortOrder.SCORE -> Strings.sortScore(lang)
                                SortOrder.RANDOM -> Strings.sortRandom(lang)
                            }
                            Text(sortLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 8.dp
                        ) {
                            SortOrder.entries.forEach { order ->
                                val itemLabel = when (order) {
                                    SortOrder.NEWEST -> Strings.sortNewest(lang)
                                    SortOrder.SCORE -> Strings.sortScore(lang)
                                    SortOrder.RANDOM -> Strings.sortRandom(lang)
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            itemLabel,
                                            fontWeight = if (vm.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        vm.applySort(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = if (vm.sortOrder == order) {
                                        { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }
                }

                item {
                    var showRatingMenu by remember { mutableStateOf(false) }
                    Box {
                        val isCustomRating = vm.safeMode || vm.excludeSafe
                        FilledTonalButton(
                            onClick = { showRatingMenu = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = when {
                                    vm.excludeSafe -> MaterialTheme.colorScheme.errorContainer
                                    vm.safeMode -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                contentColor = when {
                                    vm.excludeSafe -> MaterialTheme.colorScheme.onErrorContainer
                                    vm.safeMode -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .bouncyPress()
                        ) {
                            Icon(
                                imageVector = when {
                                    vm.excludeSafe -> Icons.Rounded.Explicit
                                    vm.safeMode -> Icons.Rounded.Shield
                                    else -> Icons.Rounded.Tune
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            val ratingLabel = when {
                                vm.excludeSafe -> Strings.only18Badge(lang)
                                vm.safeMode -> Strings.safeModeBadge(lang)
                                else -> Strings.allRatings(lang)
                            }
                            Text(
                                ratingLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isCustomRating) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showRatingMenu,
                            onDismissRequest = { showRatingMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 8.dp
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.allRatings(lang), fontWeight = if (!vm.safeMode && !vm.excludeSafe) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    vm.setSafeModeEnabled(false)
                                    vm.setExcludeSafeEnabled(false)
                                    showRatingMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        null,
                                        tint = if (!vm.safeMode && !vm.excludeSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (!vm.safeMode && !vm.excludeSafe) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.safeModeBadge(lang), fontWeight = if (vm.safeMode) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    vm.setSafeModeEnabled(true)
                                    showRatingMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Shield,
                                        null,
                                        tint = if (vm.safeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (vm.safeMode) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.tertiary) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.only18Badge(lang), fontWeight = if (vm.excludeSafe) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    vm.setExcludeSafeEnabled(true)
                                    showRatingMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Explicit,
                                        null,
                                        tint = if (vm.excludeSafe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (vm.excludeSafe) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                        }
                    }
                }

                item {
                    FilterChip(
                        selected = vm.noAi,
                        onClick = { vm.setNoAiEnabled(!vm.noAi) },
                        modifier = Modifier
                            .height(36.dp)
                            .bouncyPress(),
                        label = {
                            Text(
                                Strings.noAiBadge(lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (vm.noAi) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (vm.noAi) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
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
                    if (vm.results.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "${vm.results.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
                val spinRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spinRotation"
                )

                FilledTonalIconButton(
                    onClick = { vm.refresh() },
                    modifier = Modifier
                        .size(36.dp)
                        .bouncyPress(),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = Strings.refreshBtn(lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = if (vm.loading) spinRotation else 0f }
                    )
                }
            }

            vm.error?.let { rawErr ->
                val displayMessage = remember(rawErr, vm.isAuthError, vm.authErrorSource, vm.authErrorCode, lang) {
                    when {
                        vm.isAuthError -> {
                            val srcName = vm.authErrorSource?.let { vm.getSourceDisplayName(it) } ?: vm.source
                            Strings.authErrorDesc(srcName, vm.authErrorCode, lang)
                        }
                        vm.authErrorCode != null -> {
                            val srcName = vm.authErrorSource?.let { vm.getSourceDisplayName(it) } ?: vm.source
                            Strings.httpErrorDesc(srcName, vm.authErrorCode!!, lang)
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
                                imageVector = if (vm.isAuthError) Icons.Rounded.Key else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (vm.isAuthError)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (vm.isAuthError) Strings.authErrorTitle(lang) else Strings.genericErrorTitle(lang),
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

            Box(modifier = Modifier.weight(1f)) {
                if (vm.loading && vm.results.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                Strings.loadingText(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!vm.loading && vm.results.isEmpty() && vm.error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            Text(
                                Strings.nothingFoundDesc(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            FilledTonalButton(
                                onClick = { vm.refresh() },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.refreshBtn(lang))
                            }
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 86.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = vm.results,
                            key = { _, m -> "${m.source}_${m.id.ifBlank { m.url }}" }
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
                                onFavoriteClick = { vm.toggleFavorite(media) },
                                onClick = { vm.openFullscreen(vm.results, index) }
                            )
                        }

                        if (vm.loadingMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
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
                    text = if (localQuery.isNotBlank()) localQuery else Strings.searchPlaceholder(lang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (localQuery.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (localQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            localQuery = ""
                            vm.clearTagSuggestions()
                            vm.search(vm.source, "", vm.safeMode)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)),
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
                                vm.clearTagSuggestions()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            BasicTextField(
                                value = localQuery,
                                onValueChange = {
                                    localQuery = it
                                    val lastToken = it.substringAfterLast(" ").trim()
                                    vm.fetchTagSuggestions(lastToken)
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
                                keyboardActions = KeyboardActions(onSearch = {
                                    localQuery = localQuery.trim()
                                    vm.search(vm.source, localQuery, vm.safeMode)
                                    searchExpanded = false
                                    vm.clearTagSuggestions()
                                }),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (localQuery.isEmpty()) {
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
                            if (localQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    localQuery = ""
                                    vm.clearTagSuggestions()
                                    vm.search(vm.source, "", vm.safeMode)
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                                            onClick = {
                                                val prefix = if (localQuery.contains(" ")) {
                                                    localQuery.substringBeforeLast(" ") + " "
                                                } else {
                                                    ""
                                                }
                                                val fullQuery = (prefix + suggestion.value).trim() + " "
                                                localQuery = fullQuery
                                                vm.search(vm.source, fullQuery.trim(), vm.safeMode)
                                                searchExpanded = false
                                                vm.clearTagSuggestions()
                                            },
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
                                                        Icons.Rounded.Tag,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(14.dp))
                                                    Text(
                                                        suggestion.value,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                if (suggestion.count > 0) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            "${suggestion.count}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        if (vm.searchHistory.isNotEmpty()) {
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

                                    vm.searchHistory.take(8).forEach { hist ->
                                        Surface(
                                            onClick = {
                                                localQuery = hist
                                                vm.search(vm.source, hist, vm.safeMode)
                                                searchExpanded = false
                                                vm.clearTagSuggestions()
                                            },
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
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { vm.removeFromHistory(hist) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = "Delete",
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
                lang = lang,
                onSelect = { selectedSource ->
                    vm.search(selectedSource, vm.query, vm.safeMode)
                },
                onDismiss = { showSourceSheet = false }
            )
        }
    }
}

@Composable
private fun MediaCard(
    media: RemoteMedia,
    aspectRatio: Float,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mediaCardScale"
    )

    var loadError by remember(media.id, media.url) { mutableStateOf(false) }

    val imageModel = remember(media.sample, media.preview, media.url, loadError) {
        val targetUrl = if (loadError) {
            media.preview.ifBlank { media.url }
        } else {
            media.sample.ifBlank { media.preview.ifBlank { media.url } }
        }
        ImageRequest.Builder(context)
            .data(targetUrl)
            .crossfade(true)
            .allowHardware(true)
            .listener(
                onError = { _, _ ->
                    if (!loadError && targetUrl != media.preview && media.preview.isNotBlank()) {
                        loadError = true
                    }
                }
            )
            .build()
    }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        ) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = media.tags,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                } else if (state is coil.compose.AsyncImagePainter.State.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.BrokenImage,
                            contentDescription = "Failed to load",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            if (media.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (media.isGif) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Gif,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = onFavoriteClick,
                shape = CircleShape,
                color = if (isFavorite)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (media.score > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${media.score}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionSheet(
    currentSource: String,
    sources: List<String> = BooruRepository.AVAILABLE_SOURCES,
    lang: AppLanguage,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                val isSelected = currentSource == src
                val (icon, desc) = when (src) {
                    BooruRepository.SOURCE_ALL -> Pair(Icons.Rounded.Layers, "Search all available boorus")
                    BooruRepository.SOURCE_RULE34 -> Pair(Icons.Rounded.Explicit, "Rule34 imageboard database")
                    BooruRepository.SOURCE_GELBOORU -> Pair(Icons.Rounded.Image, "Huge anime & art collection")
                    BooruRepository.SOURCE_REALBOORU -> Pair(Icons.Rounded.VideoLibrary, "Realbooru media board")
                    BooruRepository.SOURCE_XBOORU -> Pair(Icons.Rounded.PhotoLibrary, "Massive anime & game gallery")
                    BooruRepository.SOURCE_TBIB -> Pair(Icons.Rounded.Public, "The Big ImageBoard (28M+ posts)")
                    BooruRepository.SOURCE_YANDE -> Pair(Icons.Rounded.Collections, "High-resolution wallpapers & art")
                    BooruRepository.SOURCE_KONACHAN -> Pair(Icons.Rounded.Wallpaper, "Wallpaper anime board")
                    BooruRepository.SOURCE_SAFEBOORU -> Pair(Icons.Rounded.Shield, "Safe-for-work anime art")
                    else -> Pair(Icons.Rounded.Language, "Custom Booru source")
                }

                Surface(
                    onClick = {
                        onSelect(src)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .bouncyPress()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
