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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
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
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.request.allowHardware
import coil3.compose.AsyncImagePainter
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.AppLanguage
import com.booru.app.data.ImageQuality
import com.booru.app.data.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    var appliedFilter by remember { mutableStateOf(filterText) }
    LaunchedEffect(Unit) {
        snapshotFlow { filterText }.collectLatest { text ->
            if (text.isNotBlank()) delay(200)
            appliedFilter = text
        }
    }
    var mediaTypeFilter by rememberSaveable { mutableStateOf(FavoriteMediaTypeFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(FavoriteSortOrder.NEWEST) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val folders = remember(vm.customFolders) {
        listOf<String?>(null) + vm.customFolders.sortedBy { it.lowercase() }
    }
    var pendingFolder by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { folders.size })
    val folderTabController = rememberTabPagerController(pagerState)

    LaunchedEffect(folders, pendingFolder) {
        val target = pendingFolder ?: return@LaunchedEffect
        val idx = folders.indexOf(target)
        if (idx >= 0) {
            pendingFolder = null
            pagerState.animateScrollToPage(idx, animationSpec = Motion.pageSwitch())
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

        val trimmed = appliedFilter.trim().lowercase()
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

    if (showFolderSheet) {
        val folderCounts = remember(vm.favoritesList, vm.favoriteFolders) {
            vm.favoritesList.groupingBy { vm.getMediaFolder(it) }.eachCount()
        }
        FolderPickerSheet(
            folders = folders,
            selectedIndex = folderTabController.selectedPage,
            countFor = { folder ->
                if (folder == null) vm.favoritesList.size
                else folderCounts[folder] ?: 0
            },
            lang = lang,
            onSelect = { index ->
                folderTabController.select(index)
                showFolderSheet = false
            },
            onCreate = {
                showFolderSheet = false
                newFolderName = ""
                showCreateFolderDialog = true
            },
            onDelete = { folder ->
                showFolderSheet = false
                folderToDelete = folder
            },
            onDismiss = { showFolderSheet = false }
        )
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
            onClearAll = {
                vm.clearFavorites()
                showFilterSheet = false
            },
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

                if (vm.favoritesList.isNotEmpty() || vm.customFolders.isNotEmpty()) {
                    val currentFolder = folders.getOrNull(folderTabController.selectedPage)
                    FilledTonalButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showFolderSheet = true
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .widthIn(max = 200.dp)
                    ) {
                        Icon(
                            imageVector = if (currentFolder == null) Icons.Rounded.FolderSpecial else Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = currentFolder ?: Strings.allFavoritesFolder(lang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (vm.favoritesList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = ShapeTokens.ConnectedStart,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 18.dp, end = 4.dp),
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                            enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialDefault()),
                            exit = fadeOut(Motion.effectsFast()) + scaleOut()
                        ) {
                            IconButton(
                                shapes = IconButtonDefaults.shapes(),
                                onClick = { filterText = "" }
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = Strings.clearBtn(lang),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                val activeFilterCount = (if (sortOrder != FavoriteSortOrder.NEWEST) 1 else 0) +
                    (if (mediaTypeFilter != FavoriteMediaTypeFilter.ALL) 1 else 0)
                val filterInteraction = remember { MutableInteractionSource() }
                val filterPressed by filterInteraction.collectIsPressedAsState()
                val filterOuter by animateDpAsState(if (filterPressed) 12.dp else 28.dp, Motion.spatialFast(), label = "filterOuter")
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showFilterSheet = true
                    },
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = filterOuter, bottomEnd = filterOuter),
                    color = if (activeFilterCount > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    interactionSource = filterInteraction,
                    modifier = Modifier.size(width = 64.dp, height = 56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge { Text("$activeFilterCount") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = Strings.filtersButton(lang),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (filterText.isNotBlank()) {
                val currentFolder = folders.getOrNull(pagerState.currentPage)
                val currentCount = remember(vm.favoritesList, appliedFilter, mediaTypeFilter, sortOrder, currentFolder, vm.favoriteFolders) {
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
                    val toDelete = folderToDelete ?: return@DeleteFolderBottomSheet
                    val deleteIdx = folders.indexOf(toDelete)
                    if (pagerState.currentPage == deleteIdx) {
                        scope.launch { pagerState.animateScrollToPage(0, animationSpec = Motion.pageSwitch()) }
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
            modifier = Modifier.fillMaxSize().tabTransition(folderTabController),
            key = { page -> folders.getOrNull(page)?.let { "folder:$it" } ?: "all" }
        ) { page ->
            val currentFolder = folders.getOrNull(page)
            val pageList = remember(vm.favoritesList, appliedFilter, mediaTypeFilter, sortOrder, currentFolder, vm.favoriteFolders) {
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
                            shapes = ButtonDefaults.shapes(shape = ShapeTokens.LargeIncreased, pressedShape = ButtonDefaults.pressedShape),
                            modifier = Modifier
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
                        if (currentFolder != null && appliedFilter.isBlank() && mediaTypeFilter == FavoriteMediaTypeFilter.ALL) {
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
                                Strings.tr(lang, "This collection is empty", "В этой коллекции пока ничего нет", "このコレクションは空です", "此收藏夹为空", "이 컬렉션은 비어 있습니다", "هذه المجموعة فارغة"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                Strings.tr(lang, "Add posts to this collection via details menu", "Добавьте посты в коллекцию через меню деталей", "詳細メニューから投稿を追加できます", "通过详情菜单将帖子添加到此收藏夹", "상세 메뉴에서 게시물을 추가하세요", "أضف منشورات عبر قائمة التفاصيل"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(0, animationSpec = Motion.pageSwitch()) }
                                },
                                shapes = ButtonDefaults.shapes(shape = ShapeTokens.LargeIncreased, pressedShape = ButtonDefaults.pressedShape),
                                modifier = Modifier
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
                                shapes = ButtonDefaults.shapes(shape = ShapeTokens.LargeIncreased, pressedShape = ButtonDefaults.pressedShape),
                                modifier = Modifier
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
                            quality = vm.effectiveImageQuality,
                            folder = mediaFolder,
                            lang = lang,
                            modifier = Modifier.edgeReveal(pageGridState, media.mediaKey),
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
                                    if (result == SnackbarResult.ActionPerformed) {
                                        vm.restoreFavorite(media)
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
    FlatSnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 84.dp, start = 12.dp, end = 12.dp)
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPickerSheet(
    folders: List<String?>,
    selectedIndex: Int,
    countFor: (String?) -> Int,
    lang: AppLanguage,
    onSelect: (Int) -> Unit,
    onCreate: () -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberExpandedSheetState()
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val closeThen: (() -> Unit) -> Unit = { action ->
        if (!closing) {
            closing = true
            scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
        }
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            ) {
                ExpressiveSheetHeader(
                    icon = Icons.Rounded.FolderOpen,
                    title = Strings.tr(lang, "Folders", "Папки", "フォルダ", "文件夹", "폴더", "المجلدات"),
                    polygon = MaterialShapes.Clover4Leaf,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    action = {
                        FilledTonalButton(
                            onClick = { closeThen(onCreate) },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.newFolder(lang), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    folders.forEachIndexed { index, folder ->
                        val selected = index == selectedIndex
                        val outer = 20.dp
                        val inner = 4.dp
                        val shape = when {
                            folders.size == 1 -> RoundedCornerShape(outer)
                            index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
                            index == folders.lastIndex -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
                            else -> RoundedCornerShape(inner)
                        }
                        Surface(
                            onClick = { closeThen { onSelect(index) } },
                            shape = shape,
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        selected -> Icons.Rounded.Check
                                        folder == null -> Icons.Rounded.FolderSpecial
                                        else -> Icons.Rounded.Folder
                                    },
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = folder ?: Strings.allFavoritesFolder(lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = countFor(folder).toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                if (folder != null) {
                                    IconButton(
                                        onClick = { closeThen { onDelete(folder) } },
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = Strings.tr(lang, "Delete folder", "Удалить папку", "フォルダを削除", "删除文件夹", "폴더 삭제", "حذف المجلد"),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(48.dp))
                                }
                            }
                        }
                    }
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
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberExpandedSheetState()
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ExpressiveSheetHeader(
                    icon = Icons.Rounded.Tune,
                    title = Strings.tr(lang, "Sort & Filters", "Сортировка и фильтры", "並べ替えとフィルター", "排序与筛选", "정렬 및 필터", "الترتيب والفلاتر"),
                    polygon = MaterialShapes.Cookie6Sided,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    action = {
                        SheetResetButton(
                            enabled = draftSort != FavoriteSortOrder.NEWEST || draftType != FavoriteMediaTypeFilter.ALL,
                            contentDescription = Strings.resetFilters(lang)
                        ) {
                            draftSort = FavoriteSortOrder.NEWEST
                            draftType = FavoriteMediaTypeFilter.ALL
                        }
                    }
                )
    
                Surface(
                    shape = segmentedListShape(0, 2),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
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
    
                Spacer(Modifier.height(2.dp))
    
                Surface(
                    shape = segmentedListShape(1, 2),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
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
    
                Spacer(Modifier.height(14.dp))
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSortSelected(draftSort)
                            onTypeSelected(draftType)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = ShapeTokens.ConnectedStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = Strings.applyFilters(lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    AnimatedConfirmDeleteButton(
                        onConfirmed = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onClearAll() }
                        },
                        lang = lang,
                        initialIcon = Icons.Rounded.DeleteSweep,
                        initialText = Strings.tr(lang, "Clear all", "Очистить", "すべて消去", "全部清除", "모두 지우기", "مسح الكل"),
                        confirmText = Strings.confirmDeleteAction(lang),
                        height = 56.dp,
                        contentPadding = 18.dp,
                        idleContainerColor = MaterialTheme.colorScheme.errorContainer,
                        idleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = ShapeTokens.ConnectedEnd
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
    val keyboardSheet = rememberKeyboardSheetState()
    val sheetState = keyboardSheet.state
    val scope = rememberCoroutineScope()
    var folderName by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val trimmedName = folderName.trim()
    val reserved = remember(lang) { setOf("all", "все", Strings.allFavoritesFolder(lang).lowercase()) }
    val isDuplicate = !submitted && trimmedName.isNotEmpty() &&
        (existingNames.any { it.equals(trimmedName, ignoreCase = true) } || trimmedName.lowercase() in reserved)
    val canCreate = trimmedName.isNotEmpty() && !isDuplicate
    val submit: () -> Unit = {
        if (canCreate && !submitted) {
            submitted = true
            onConfirm(trimmedName)
            scope.launch { keyboardSheet.hide() }.invokeOnCompletion { onDismiss() }
        }
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { sheetState.currentValue }
            .first { it == androidx.compose.material3.SheetValue.Expanded }
        delay(60)
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
            KeyboardSheetEffects(keyboardSheet, onDismiss = onDismiss)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                ExpressiveSheetHeader(
                    icon = Icons.Rounded.CreateNewFolder,
                    title = Strings.newFolder(lang),
                    subtitle = Strings.tr(
                        lang,
                        "${existingNames.size} collections",
                        "Коллекций: ${existingNames.size}",
                        "コレクション: ${existingNames.size}",
                        "收藏夹：${existingNames.size}",
                        "컬렉션 ${existingNames.size}개",
                        "المجموعات: ${existingNames.size}"
                    ),
                    polygon = MaterialShapes.Puffy,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                val fieldColor by animateColorAsState(
                    targetValue = if (isDuplicate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = Motion.effectsDefault(),
                    label = "folderFieldColor"
                )
                val createCorner by animateDpAsState(
                    targetValue = if (canCreate) 28.dp else 8.dp,
                    animationSpec = Motion.spatialDefault(),
                    label = "folderCreateCorner"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = ShapeTokens.ConnectedStart,
                        color = fieldColor,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 18.dp, end = 6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isDuplicate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (folderName.isEmpty()) {
                                    Text(
                                        text = Strings.folderNamePlaceholder(lang),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                BasicTextField(
                                    value = folderName,
                                    onValueChange = { folderName = it.take(40) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (isDuplicate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submit() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }
                            AnimatedVisibility(
                                visible = folderName.isNotEmpty(),
                                enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast()),
                                exit = fadeOut(Motion.effectsFast()) + scaleOut(Motion.spatialFast())
                            ) {
                                IconButton(onClick = { folderName = "" }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = Strings.tr(lang, "Clear", "Очистить", "クリア", "清除", "지우기", "مسح"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    FilledIconButton(
                        onClick = submit,
                        enabled = canCreate,
                        shape = RoundedCornerShape(topStart = createCorner, bottomStart = createCorner, topEnd = 28.dp, bottomEnd = 28.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = Strings.create(lang), modifier = Modifier.size(24.dp))
                    }
                }

                AnimatedVisibility(
                    visible = isDuplicate,
                    enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.spatialDefault()),
                    exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.spatialDefault())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = Strings.folderNameExists(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
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
    val sheetState = rememberExpandedSheetState()
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
                ExpressiveSheetHeader(
                    icon = Icons.Rounded.DeleteOutline,
                    title = Strings.tr(lang, "Delete collection?", "Удалить коллекцию?", "コレクションを削除しますか？", "删除此收藏夹？", "컬렉션을 삭제할까요?", "حذف المجموعة؟"),
                    polygon = MaterialShapes.Burst,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
    
                Text(
                    text = Strings.tr(
                        lang,
                        "Collection \"$folderName\" will be removed. Media items will remain in favorites.",
                        "Коллекция «$folderName» будет удалена. Медиафайлы останутся в общем избранном.",
                        "コレクション「$folderName」を削除します。メディアはお気に入りに残ります。",
                        "将删除收藏夹「$folderName」。媒体仍保留在收藏中。",
                        "컬렉션 \"$folderName\"이(가) 삭제됩니다. 미디어는 즐겨찾기에 남습니다.",
                        "ستتم إزالة المجموعة \"$folderName\". ستبقى الوسائط في المفضلة."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(Strings.cancelBtn(lang))
                    }
    
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onConfirm()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(Strings.tr(lang, "Delete", "Удалить", "削除", "删除", "삭제", "حذف"))
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
    lang: AppLanguage,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            .apply { if (retryKey > 0) memoryCacheKeyExtra("retry", retryKey.toString()) }
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
                    .size(36.dp),
                shapes = IconButtonDefaults.shapes(shape = CircleShape, pressedShape = IconButtonDefaults.smallPressedShape),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = Strings.tr(lang, "Remove from favorites", "Убрать из избранного", "お気に入りから削除", "从收藏中移除", "즐겨찾기에서 제거", "إزالة من المفضلة"),
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
