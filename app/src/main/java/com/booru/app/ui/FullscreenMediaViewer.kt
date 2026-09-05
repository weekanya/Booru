package com.booru.app.ui

import androidx.compose.runtime.Composable
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia

@Composable
fun FullscreenMediaViewer(
    initialIndex: Int,
    mediaList: List<RemoteMedia>,
    vm: GalleryViewModel,
    onDismiss: () -> Unit,
    onLoadMore: (() -> Unit)? = null,
    onNavigateToExplore: (() -> Unit)? = null
) {
    MediaDetailSheet(
        initialIndex = initialIndex,
        mediaList = mediaList,
        vm = vm,
        onDismiss = onDismiss,
        onLoadMore = onLoadMore,
        onNavigateToExplore = onNavigateToExplore
    )
}
