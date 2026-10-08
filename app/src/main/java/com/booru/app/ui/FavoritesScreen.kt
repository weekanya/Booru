@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.AppLanguage
import com.booru.app.data.ImageQuality
import com.booru.app.data.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FavoriteMediaTypeFilter {
    ALL,
    IMAGES,
    GIFS,
    VIDEOS
}

enum class FavoriteSortOrder {
    NEWEST,
    OLDEST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    vm: GalleryViewModel,
    onNavigateToExplore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = vm.language
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var filterText by rememberSaveable { mutableStateOf("") }
    var mediaTypeFilter by rememberSaveable { mutableStateOf(FavoriteMediaTypeFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(FavoriteSortOrder.NEWEST) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val folders = remember(vm.customFolders) {
        listOf<String?>(null) + vm.customFolders.sortedBy { it.lowercase() }
    }
    var pendingFolder by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { folders.size })
    val chipRowState = rememberLazyListState()

    LaunchedEffect(folders, pendingFolder) {
        val target = pendingFolder ?: return@LaunchedEffect
        val idx = folders.indexOf(target)
        if (idx >= 0) {
            pendingFolder = null
            pagerState.animateScrollToPage(idx)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in folders.indices) {
            chipRowState.animateScrollToItem(pagerState.currentPage)
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val allCount = vm.favoritesList.size
    val imagesCount = remember(vm.favoritesList) {
        vm.favoritesList.count { !it.isVideo && !it.isGif }
    }
    val gifsCount = remember(vm.favoritesList) {
        vm.favoritesList.count { it.isGif }
    }
    val videosCount = remember(vm.favoritesList) {
        vm.favoritesList.count { it.isVideo }
    }

    fun getFolderMediaList(folder: String?): List<RemoteMedia> {
        var list = vm.favoritesList.asSequence()

        if (folder != null) {
            list = list.filter {
                vm.getMediaFolder(it) == folder
            }
        }

        when (mediaTypeFilter) {
            FavoriteMediaTypeFilter.ALL -> {}
            FavoriteMediaTypeFilter.IMAGES -> {
                list = list.filter { !it.isVideo && !it.isGif }
            }
            FavoriteMediaTypeFilter.GIFS -> {
                list = list.filter { it.isGif }
            }
            FavoriteMediaTypeFilter.VIDEOS -> {
                list = list.filter { it.isVideo }
            }
        }

        val trimmed = filterText.trim().lowercase()
        if (trimmed.isNotBlank()) {
            val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val excluded = tokens.filter { it.length > 1 && it.startsWith("-") }.map { it.drop(1) }
            val included = tokens.filterNot { it.startsWith("-") }
            list = list.filter { media ->
                included.all { token ->
                    media.tagList.any { it.contains(token, ignoreCase = true) } ||
                        media.source.contains(token, ignoreCase = true)
                } && excluded.none { token ->
                    media.tagList.any { it.equals(token, ignoreCase = true) }
                }
            }
        }

        return when (sortOrder) {
            FavoriteSortOrder.NEWEST -> list.toList()
            FavoriteSortOrder.OLDEST -> list.toList().asReversed()
        }
    }

    if (showFilterSheet) {
        FavoritesFilterBottomSheet(
            currentSort = sortOrder,
            currentType = mediaTypeFilter,
            allCount = allCount,
            imagesCount = imagesCount,
            gifsCount = gifsCount,
            videosCount = videosCount,
            lang = lang,
            onSortSelected = { sortOrder = it },
            onTypeSelected = { mediaTypeFilter = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.navFavorites(lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (vm.favoritesList.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeFilterCount = (if (sortOrder != FavoriteSortOrder.NEWEST) 1 else 0) +
                            (if (mediaTypeFilter != FavoriteMediaTypeFilter.ALL) 1 else 0)

                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showFilterSheet = true
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
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

                        AnimatedConfirmDeleteButton(
                            onConfirmed = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                vm.clearFavorites()
                            },
                            lang = lang,
                            initialIcon = Icons.Rounded.DeleteSweep,
                            confirmText = if (lang == AppLanguage.RUSSIAN) "Удалить всё?" else Strings.confirmDeleteAction(lang)
                        )
                    }
                }
            }
        }

        if (vm.favoritesList.isNotEmpty()) {
            Surface(
                shape = ShapeTokens.LargeIncreased,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (filterText.isEmpty()) {
                            Text(
                                text = Strings.favSearchHint(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AnimatedVisibility(
                        visible = filterText.isNotEmpty(),
                        enter = fadeIn(tween(150)) + scaleIn(Motion.spatialDefault()),
                        exit = fadeOut(tween(150)) + scaleOut()
                    ) {
                        IconButton(
                            onClick = { filterText = "" },
                            modifier = Modifier
                                .size(28.dp)
                                .bouncyPress()
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

            if (vm.favoritesList.isNotEmpty() || vm.customFolders.isNotEmpty()) {
                val targetFolderPage = pagerState.currentPage
                LazyRow(
                    state = chipRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "all_folder_tab") {
                        val isAllSelected = targetFolderPage == 0
                        FolderTabPill(
                            title = Strings.allFavoritesFolder(lang),
                            count = vm.favoritesList.size,
                            isSelected = isAllSelected,
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 3) {
                                        pagerState.scrollToPage(2)
                                    }
                                    pagerState.animateScrollToPage(0, animationSpec = spring(dampingRatio = 1f, stiffness = 320f))
                                }
                            },
                            icon = Icons.Rounded.FolderSpecial
                        )
                    }

                    itemsIndexed(
                        items = folders.drop(1).filterNotNull(),
                        key = { _, folder -> folder }
                    ) { index, folder ->
                        val pageIndex = index + 1
                        val count = remember(vm.favoritesList, folder, vm.favoriteFolders) {
                            vm.favoritesList.count {
                                vm.getMediaFolder(it) == folder
                            }
                        }
                        val isSelected = targetFolderPage == pageIndex
                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(260, easing = FastOutSlowInEasing),
                                placementSpec = null,
                                fadeOutSpec = tween(180)
                            )
                        ) {
                        FolderTabPill(
                            title = folder,
                            count = count,
                            isSelected = isSelected,
                            onClick = {
                                scope.launch {
                                    if (kotlin.math.abs(pagerState.currentPage - pageIndex) > 3) {
                                        pagerState.scrollToPage(if (pageIndex > pagerState.currentPage) pageIndex - 2 else pageIndex + 2)
                                    }
                                    pagerState.animateScrollToPage(pageIndex, animationSpec = spring(dampingRatio = 1f, stiffness = 320f))
                                }
                            },
                            onDelete = {
                                folderToDelete = folder
                            },
                            icon = Icons.Rounded.Folder
                        )
                        }
                    }

                    item(key = "add_folder_pill") {
                        Surface(
                            onClick = {
                                newFolderName = ""
                                showCreateFolderDialog = true
                            },
                            shape = ShapeTokens.LargeIncreased,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .height(42.dp)
                                .bouncyPress()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = Strings.newFolder(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (filterText.isNotBlank()) {
                val currentFolder = folders.getOrNull(pagerState.currentPage)
                val currentCount = remember(vm.favoritesList, filterText, mediaTypeFilter, sortOrder, currentFolder, vm.favoriteFolders) {
                    getFolderMediaList(currentFolder).size
                }
                Surface(
                    shape = ShapeTokens.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = Strings.favFoundCount(currentCount, lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = { filterText = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                Strings.clearBtn(lang),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }

        if (folderToDelete != null) {
            DeleteFolderBottomSheet(
                folderName = folderToDelete!!,
                lang = lang,
                onConfirm = {
                    val toDelete = folderToDelete!!
                    val deleteIdx = folders.indexOf(toDelete)
                    if (pagerState.currentPage == deleteIdx) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    } else if (pagerState.currentPage > deleteIdx) {
                        scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
                    }
                    vm.removeCustomFolder(toDelete)
                    folderToDelete = null
                },
                onDismiss = { folderToDelete = null }
            )
        }

        if (showCreateFolderDialog) {
            CreateFolderBottomSheet(
                lang = lang,
                existingNames = vm.customFolders,
                onConfirm = { name ->
                    vm.addCustomFolder(name)
                    pendingFolder = name
                },
                onDismiss = { showCreateFolderDialog = false }
            )
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
            key = { page -> folders.getOrNull(page) ?: "all_favorites" }
        ) { page ->
            val currentFolder = folders.getOrNull(page)
            val pageList = remember(vm.favoritesList, filterText, mediaTypeFilter, sortOrder, currentFolder, vm.favoriteFolders) {
                getFolderMediaList(currentFolder)
            }
            val pageGridState = rememberLazyStaggeredGridState()
            LaunchedEffect(pageGridState.isScrollInProgress) {
                if (pageGridState.isScrollInProgress) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }

            if (vm.favoritesList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(92.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(46.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            Strings.favoritesEmptyTitle(lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Strings.favoritesEmptyDesc(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateToExplore,
                            shape = ShapeTokens.LargeIncreased,
                            modifier = Modifier.bouncyPress()
                        ) {
                            Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.goToExplore(lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (pageList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        if (currentFolder != null && filterText.isBlank() && mediaTypeFilter == FavoriteMediaTypeFilter.ALL) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (lang == AppLanguage.RUSSIAN) "В этой коллекции пока ничего нет" else "This collection is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (lang == AppLanguage.RUSSIAN) "Добавьте посты в коллекцию через меню деталей" else "Add posts to this collection via details menu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                },
                                shape = ShapeTokens.LargeIncreased,
                                modifier = Modifier.bouncyPress()
                            ) {
                                Icon(Icons.Rounded.FolderSpecial, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.allFavoritesFolder(lang), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                Strings.nothingFound(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    filterText = ""
                                    mediaTypeFilter = FavoriteMediaTypeFilter.ALL
                                },
                                shape = ShapeTokens.LargeIncreased,
                                modifier = Modifier.bouncyPress()
                            ) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.clearBtn(lang), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                val gridCells = when (vm.gridColumnsCount) {
                    1 -> StaggeredGridCells.Fixed(adaptiveColumns(1))
                    2 -> StaggeredGridCells.Fixed(adaptiveColumns(2))
                    3 -> StaggeredGridCells.Fixed(adaptiveColumns(3))
                    4 -> StaggeredGridCells.Fixed(adaptiveColumns(4))
                    else -> StaggeredGridCells.Adaptive(minSize = 175.dp)
                }
                LazyVerticalStaggeredGrid(
                    state = pageGridState,
                    columns = gridCells,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 86.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = pageList,
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
                        val mediaFolder = remember(media.id, media.mediaKey, vm.favoriteFolders) {
                            vm.getMediaFolder(media)
                        }
                        FavoriteCard(
                            media = media,
                            aspectRatio = ratio,
                            quality = vm.imageQuality,
                            folder = mediaFolder,
                            onRemove = {
                                vm.toggleFavorite(media)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = Strings.favoriteRemoved(lang),
                                        actionLabel = Strings.undoBtn(lang),
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed && !vm.isFavorite(media)) {
                                        vm.toggleFavorite(media)
                                    }
                                }
                            },
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                vm.openFullscreen(pageList, index)
                            }
                        )
                    }
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 84.dp, start = 12.dp, end = 12.dp)
    )
    }
}

@Composable
private fun FolderTabPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    icon: ImageVector = Icons.Rounded.Folder
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "folderPillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "folderPillContent"
    )

    Surface(
        onClick = onClick,
        shape = ShapeTokens.LargeIncreased,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .height(42.dp)
            .bouncyPress()
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = if (onDelete != null) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(
                targetState = isSelected,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "folderPillIcon",
                modifier = Modifier.size(18.dp)
            ) { selected ->
                Icon(
                    imageVector = if (selected) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) contentColor else contentColor.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(6.dp))

            Surface(
                shape = CircleShape,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() }
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(14.dp),
                        tint = contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesFilterBottomSheet(
    currentSort: FavoriteSortOrder,
    currentType: FavoriteMediaTypeFilter,
    allCount: Int,
    imagesCount: Int,
    gifsCount: Int,
    videosCount: Int,
    lang: AppLanguage,
    onSortSelected: (FavoriteSortOrder) -> Unit,
    onTypeSelected: (FavoriteMediaTypeFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var draftSort by remember { mutableStateOf(currentSort) }
    var draftType by remember { mutableStateOf(currentType) }

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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
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
                            text = if (lang == AppLanguage.RUSSIAN) "Сортировка и фильтры" else "Sort & Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
    
                    TextButton(
                        onClick = {
                            draftSort = FavoriteSortOrder.NEWEST
                            draftType = FavoriteMediaTypeFilter.ALL
                        },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
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
                    shape = ShapeTokens.LargeIncreased,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            FilterSectionIcon(Icons.AutoMirrored.Rounded.Sort)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = Strings.favSortTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            val sortOptions = listOf(
                                Triple(FavoriteSortOrder.NEWEST, Strings.favSortNewest(lang), Icons.Rounded.Schedule),
                                Triple(FavoriteSortOrder.OLDEST, Strings.favSortOldest(lang), Icons.Rounded.History)
                            )
                            sortOptions.forEachIndexed { orderIndex, (order, label, icon) ->
                                FilterOptionButton(
                                    selected = draftSort == order,
                                    onClick = { draftSort = order },
                                    position = groupPosition(orderIndex, sortOptions.size),
                                    label = label,
                                    icon = icon,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
    
                Spacer(Modifier.height(14.dp))
    
                Surface(
                    shape = ShapeTokens.LargeIncreased,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            FilterSectionIcon(Icons.Rounded.Category)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = Strings.contentTypeTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            FilterOptionButton(
                                selected = draftType == FavoriteMediaTypeFilter.ALL,
                                position = GroupPosition.Leading,
                                onClick = { draftType = FavoriteMediaTypeFilter.ALL },
                                label = "${Strings.favFilterAll(lang)} ($allCount)",
                                modifier = Modifier.weight(1f)
                            )
                            FilterOptionButton(
                                selected = draftType == FavoriteMediaTypeFilter.IMAGES,
                                position = GroupPosition.Trailing,
                                onClick = { draftType = FavoriteMediaTypeFilter.IMAGES },
                                label = "${Strings.favFilterImages(lang)} ($imagesCount)",
                                icon = Icons.Rounded.Image,
                                modifier = Modifier.weight(1f)
                            )
                        }
    
                        Spacer(Modifier.height(8.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            FilterOptionButton(
                                selected = draftType == FavoriteMediaTypeFilter.GIFS,
                                position = GroupPosition.Leading,
                                onClick = { draftType = FavoriteMediaTypeFilter.GIFS },
                                label = "${Strings.favFilterGifs(lang)} ($gifsCount)",
                                icon = Icons.Rounded.Gif,
                                modifier = Modifier.weight(1f)
                            )
                            FilterOptionButton(
                                selected = draftType == FavoriteMediaTypeFilter.VIDEOS,
                                position = GroupPosition.Trailing,
                                onClick = { draftType = FavoriteMediaTypeFilter.VIDEOS },
                                label = "${Strings.favFilterVideos(lang)} ($videosCount)",
                                icon = Icons.Rounded.Videocam,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
    
                Spacer(Modifier.height(20.dp))
    
                Button(
                    onClick = {
                        onSortSelected(draftSort)
                        onTypeSelected(draftType)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) "Готово" else "Done",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFolderBottomSheet(
    lang: AppLanguage,
    existingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var folderName by remember { mutableStateOf("") }
    val trimmedName = folderName.trim()
    val reserved = remember(lang) { setOf("all", "все", Strings.allFavoritesFolder(lang).lowercase()) }
    val isDuplicate = trimmedName.isNotEmpty() &&
        (existingNames.any { it.equals(trimmedName, ignoreCase = true) } || trimmedName.lowercase() in reserved)
    val canCreate = trimmedName.isNotEmpty() && !isDuplicate
    val submit: () -> Unit = {
        if (canCreate) {
            onConfirm(trimmedName)
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.newFolder(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
    
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it.take(40) },
                    placeholder = { Text(Strings.folderNamePlaceholder(lang)) },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text(Strings.folderNameExists(lang)) }
                    } else null,
                    shape = ShapeTokens.Large,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
    
                Spacer(Modifier.height(20.dp))
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = ShapeTokens.Large,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(Strings.cancelBtn(lang))
                    }
    
                    Button(
                        onClick = submit,
                        enabled = canCreate,
                        shape = ShapeTokens.Large,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(Strings.create(lang))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteFolderBottomSheet(
    folderName: String,
    lang: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (lang == AppLanguage.RUSSIAN) "Удалить коллекцию?" else "Delete collection?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
    
                Text(
                    text = if (lang == AppLanguage.RUSSIAN)
                        "Коллекция «$folderName» будет удалена. Медиафайлы останутся в общем избранном."
                    else
                        "Collection \"$folderName\" will be removed. Media items will remain in favorites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
    
                Spacer(Modifier.height(24.dp))
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = ShapeTokens.Large,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(Strings.cancelBtn(lang))
                    }
    
                    Button(
                        onClick = {
                            onConfirm()
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = ShapeTokens.Large,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(if (lang == AppLanguage.RUSSIAN) "Удалить" else "Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteCard(
    media: RemoteMedia,
    aspectRatio: Float,
    quality: ImageQuality,
    folder: String?,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = Motion.spatialFast(),
        label = "favCardScale"
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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(ShapeTokens.LargeIncreased)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (isError) {
                        isError = false
                        retryKey++
                    } else {
                        onClick()
                    }
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
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.65f))
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

            FilledTonalIconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .bouncyPress(),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (folder != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = folder,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
