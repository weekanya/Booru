package com.booru.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.Strings

enum class FavoriteMediaTypeFilter {
    ALL,
    IMAGES,
    GIFS,
    VIDEOS
}

enum class FavoriteSortOrder {
    NEWEST,
    OLDEST,
    SOURCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    vm: GalleryViewModel,
    onNavigateToExplore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = vm.language
    var filterText by rememberSaveable { mutableStateOf("") }
    var mediaTypeFilter by rememberSaveable { mutableStateOf(FavoriteMediaTypeFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(FavoriteSortOrder.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

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

    val topTags = remember(vm.favoritesList) {
        val tagCounts = mutableMapOf<String, Int>()
        vm.favoritesList.forEach { media ->
            media.tagList.forEach { rawTag ->
                val tag = rawTag.trim().lowercase()
                if (tag.length > 2 && !tag.startsWith("rating:") && !tag.startsWith("score:") && !tag.startsWith("source:")) {
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }
        }
        tagCounts.entries
            .sortedByDescending { it.value }
            .take(12)
            .map { it.key }
    }

    val filteredList = remember(vm.favoritesList, filterText, mediaTypeFilter, sortOrder) {
        var list = vm.favoritesList.asSequence()

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
            list = list.filter { media ->
                tokens.all { token ->
                    media.tagList.any { it.contains(token, ignoreCase = true) } ||
                        media.source.contains(token, ignoreCase = true)
                }
            }
        }

        when (sortOrder) {
            FavoriteSortOrder.NEWEST -> list.toList()
            FavoriteSortOrder.OLDEST -> list.toList().asReversed()
            FavoriteSortOrder.SOURCE -> list.sortedWith(
                compareBy<RemoteMedia> { it.source.lowercase() }.thenByDescending { it.id.toLongOrNull() ?: 0L }
            ).toList()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            Strings.clearFavoritesConfirm(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { showClearDialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = { Text(Strings.clearFavoritesDesc(vm.favoritesList.size, lang), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearFavorites()
                        showClearDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.clearBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        Strings.navFavorites(lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        Strings.savedPostsCount(vm.favoritesList.size, lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (vm.favoritesList.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        FilledTonalIconButton(
                            onClick = { showSortMenu = true },
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Rounded.SwapVert,
                                contentDescription = Strings.favSortTitle(lang),
                                tint = if (sortOrder != FavoriteSortOrder.NEWEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Strings.favSortNewest(lang),
                                        fontWeight = if (sortOrder == FavoriteSortOrder.NEWEST) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOrder = FavoriteSortOrder.NEWEST
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOrder == FavoriteSortOrder.NEWEST) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Strings.favSortOldest(lang),
                                        fontWeight = if (sortOrder == FavoriteSortOrder.OLDEST) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOrder = FavoriteSortOrder.OLDEST
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOrder == FavoriteSortOrder.OLDEST) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        Strings.favSortSource(lang),
                                        fontWeight = if (sortOrder == FavoriteSortOrder.SOURCE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOrder = FavoriteSortOrder.SOURCE
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOrder == FavoriteSortOrder.SOURCE) {
                                    { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { showClearDialog = true },
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (vm.favoritesList.isNotEmpty()) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text(Strings.favSearchHint(lang)) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { filterText = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = (mediaTypeFilter == FavoriteMediaTypeFilter.ALL),
                        onClick = { mediaTypeFilter = FavoriteMediaTypeFilter.ALL },
                        label = { Text("${Strings.favFilterAll(lang)} ($allCount)") },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    )
                }
                item {
                    FilterChip(
                        selected = (mediaTypeFilter == FavoriteMediaTypeFilter.IMAGES),
                        onClick = {
                            mediaTypeFilter = if (mediaTypeFilter == FavoriteMediaTypeFilter.IMAGES) {
                                FavoriteMediaTypeFilter.ALL
                            } else {
                                FavoriteMediaTypeFilter.IMAGES
                            }
                        },
                        label = { Text("${Strings.favFilterImages(lang)} ($imagesCount)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Image, null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    )
                }
                item {
                    FilterChip(
                        selected = (mediaTypeFilter == FavoriteMediaTypeFilter.GIFS),
                        onClick = {
                            mediaTypeFilter = if (mediaTypeFilter == FavoriteMediaTypeFilter.GIFS) {
                                FavoriteMediaTypeFilter.ALL
                            } else {
                                FavoriteMediaTypeFilter.GIFS
                            }
                        },
                        label = { Text("${Strings.favFilterGifs(lang)} ($gifsCount)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Gif, null, modifier = Modifier.size(18.dp))
                        },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    )
                }
                item {
                    FilterChip(
                        selected = (mediaTypeFilter == FavoriteMediaTypeFilter.VIDEOS),
                        onClick = {
                            mediaTypeFilter = if (mediaTypeFilter == FavoriteMediaTypeFilter.VIDEOS) {
                                FavoriteMediaTypeFilter.ALL
                            } else {
                                FavoriteMediaTypeFilter.VIDEOS
                            }
                        },
                        label = { Text("${Strings.favFilterVideos(lang)} ($videosCount)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    )
                }

                if (topTags.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                    }

                    items(topTags) { tag ->
                        val isSelected = filterText.contains(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    val regex = "(?i)\\b${Regex.escape(tag)}\\b".toRegex()
                                    filterText = filterText.replace(regex, "").replace("\\s+".toRegex(), " ").trim()
                                } else {
                                    filterText = if (filterText.isBlank()) tag else "${filterText.trim()} $tag"
                                }
                            },
                            label = { Text("#$tag") },
                            shape = CircleShape,
                            modifier = Modifier.bouncyPress()
                        )
                    }
                }
            }

            val isFiltered = filterText.isNotBlank() || mediaTypeFilter != FavoriteMediaTypeFilter.ALL || sortOrder != FavoriteSortOrder.NEWEST
            if (isFiltered) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.favFoundCount(filteredList.size, lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            filterText = ""
                            mediaTypeFilter = FavoriteMediaTypeFilter.ALL
                            sortOrder = FavoriteSortOrder.NEWEST
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            Strings.resetFilters(lang),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        Strings.favoritesEmptyTitle(lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
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
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.goToExplore(lang))
                    }
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Strings.nothingFound(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            filterText = ""
                            mediaTypeFilter = FavoriteMediaTypeFilter.ALL
                        }
                    ) {
                        Text(Strings.clearBtn(lang))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 86.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = filteredList,
                    key = { _, m -> "${m.source}_${m.id.ifBlank { m.url }}" }
                ) { index, media ->
                    FavoriteCard(
                        media = media,
                        onRemove = { vm.toggleFavorite(media) },
                        onClick = { vm.openFullscreen(filteredList, index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    media: RemoteMedia,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var loadError by remember(media.id, media.url) { mutableStateOf(false) }

    val imageModel = remember(media.sample, media.preview, media.url, loadError) {
        val targetUrl = if (loadError) {
            media.sample.ifBlank { media.url }
        } else {
            media.preview.ifBlank { media.sample.ifBlank { media.url } }
        }
        ImageRequest.Builder(context)
            .data(targetUrl)
            .crossfade(true)
            .allowHardware(true)
            .listener(
                onError = { _, _ ->
                    if (!loadError && targetUrl != media.sample && media.sample.isNotBlank()) {
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
            .bouncyPress()
            .clickable(onClick = onClick)
    ) {
        Box {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
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
                    .size(36.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                )
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
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
        }
    }
}
