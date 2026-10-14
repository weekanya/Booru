package com.booru.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class TabPagerController(val pagerState: PagerState, private val scope: CoroutineScope) {
    var pendingPage by mutableStateOf<Int?>(null)
        private set
    private var job: Job? = null
    private var generation = 0
    internal val enter = Animatable(1f)
    internal var direction by mutableIntStateOf(0)
        private set

    val selectedPage: Int
        get() = pendingPage ?: if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage

    fun select(page: Int) {
        if (page == pagerState.currentPage && pendingPage == null && !pagerState.isScrollInProgress) return
        val from = pagerState.currentPage
        pendingPage = page
        job?.cancel()
        val token = ++generation
        job = scope.launch {
            try {
                direction = if (page >= from) 1 else -1
                enter.snapTo(0f)
                pagerState.scrollToPage(page)
                pendingPage = null
                enter.animateTo(1f, tween(durationMillis = 320, easing = Motion.EmphasizedDecelerate))
            } finally {
                if (pendingPage == page) pendingPage = null
                // If the scroll was interrupted (e.g. user drag took priority), the content
                // would stay invisible at enter=0. Restore it unless a newer select() owns it.
                if (token == generation && enter.value < 1f) {
                    scope.launch { enter.snapTo(1f) }
                }
            }
        }
    }
}

fun Modifier.tabTransition(controller: TabPagerController): Modifier = graphicsLayer {
    val progress = controller.enter.value
    if (progress < 1f) {
        alpha = progress
        translationX = (1f - progress) * 40.dp.toPx() * controller.direction
    }
}

@Composable
fun rememberTabPagerController(pagerState: PagerState): TabPagerController {
    val scope = rememberCoroutineScope()
    return remember(pagerState, scope) { TabPagerController(pagerState, scope) }
}
