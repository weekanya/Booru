package com.booru.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.edgeReveal(state: LazyStaggeredGridState, key: Any): Modifier = graphicsLayer {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
    val zone = 140.dp.toPx()
    val top = item.offset.y.toFloat()
    val bottom = top + item.size.height
    val topBlend = if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / zone).coerceIn(0f, 1f)
    val fromTop = ((bottom - info.viewportStartOffset) / zone).coerceIn(0f, 1f)
    val fromBottom = ((info.viewportEndOffset - top) / zone).coerceIn(0f, 1f)
    val topEffect = 1f - (1f - fromTop) * topBlend
    val nearTop = topEffect < fromBottom
    val t = FastOutSlowInEasing.transform(if (nearTop) topEffect else fromBottom)
    if (t >= 1f) return@graphicsLayer
    val scale = 0.86f + 0.14f * t
    scaleX = scale
    scaleY = scale
    alpha = 0.15f + 0.85f * t
    translationY = (1f - t) * 24.dp.toPx() * if (nearTop) -1f else 1f
    transformOrigin = if (nearTop) TransformOrigin(0.5f, 1f) else TransformOrigin(0.5f, 0f)
}
