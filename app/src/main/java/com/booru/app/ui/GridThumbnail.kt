package com.booru.app.ui

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.GestureType

@Composable
internal fun GridThumbnail(
    model: ImageRequest,
    tiled: Boolean,
    contentDescription: String?,
    contentScale: ContentScale,
    onState: (AsyncImagePainter.State) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiled) {
        val zoomState = rememberCoilZoomState()
        SideEffect {
            zoomState.zoomable.setReadMode(null)
            zoomState.zoomable.setDisabledGestureTypes(GestureType.values.fold(0) { mask, type -> mask or type })
        }
        Box(modifier) {
            CoilZoomAsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                zoomState = zoomState,
                scrollBar = null,
                onState = onState,
            )
            // Shield ZoomImage's gestures without consuming the parent card's clicks.
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            })
        }
    } else {
        AsyncImage(model = model, contentDescription = contentDescription, modifier = modifier,
            contentScale = contentScale, onState = onState)
    }
}
