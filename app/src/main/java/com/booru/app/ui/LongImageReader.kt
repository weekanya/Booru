package com.booru.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ReadMode
import com.github.panpf.zoomimage.util.IntSizeCompat
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun LongImageReader(
    model: ImageRequest,
    contentDescription: String,
    isActive: Boolean,
    resetZoomKey: Int,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val zoomState = rememberCoilZoomState()
    val onZoomChangedLatest by rememberUpdatedState(onZoomChanged)
    SideEffect { zoomState.zoomable.setReadMode(ComicReadMode) }
    LaunchedEffect(zoomState, isActive, resetZoomKey) {
        zoomState.zoomable.reset()
    }
    LaunchedEffect(zoomState, isActive) {
        snapshotFlow {
            val state = zoomState.zoomable
            val readingScale = if (state.contentSize.width > 0) {
                state.containerSize.width.toFloat() / state.contentSize.width
            } else 0f
            isActive && readingScale > 0f && abs(state.transform.scaleX / readingScale - 1f) > 0.05f
        }.distinctUntilChanged().collect { onZoomChangedLatest(it) }
    }
    var imageState by remember(model) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(modifier, contentAlignment = Alignment.Center) {
        CoilZoomAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            zoomState = zoomState,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            onState = { imageState = it },
            onTap = onTap?.let { callback -> { _ -> callback() } },
        )
        when (imageState) {
            is AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(Modifier.size(44.dp))
            is AsyncImagePainter.State.Error ->
                Icon(Icons.Rounded.BrokenImage, contentDescription = null, modifier = Modifier.size(40.dp))
            else -> Unit
        }
    }
}

// Read Mode fills both axes; reject it when that would crop the image horizontally.
internal val ComicReadMode = ReadMode(
    sizeType = ReadMode.SIZE_TYPE_VERTICAL,
    decider = object : ReadMode.Decider {
        override fun should(contentSize: IntSizeCompat, containerSize: IntSizeCompat): Boolean {
            if (contentSize.width <= 0 || contentSize.height <= 0 ||
                containerSize.width <= 0 || containerSize.height <= 0
            ) return false
            return contentSize.height.toLong() * containerSize.width >=
                containerSize.height.toLong() * contentSize.width
        }
    },
)
