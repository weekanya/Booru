package com.booru.app.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class TabPagerController(val pagerState: PagerState, private val scope: CoroutineScope) {
    var pendingPage by mutableStateOf<Int?>(null)
        private set
    private var job: Job? = null

    val selectedPage: Int
        get() = pendingPage ?: if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage

    fun select(page: Int) {
        if (page == pagerState.currentPage && pendingPage == null && !pagerState.isScrollInProgress) return
        pendingPage = page
        job?.cancel()
        job = scope.launch {
            try {
                pagerState.animateScrollToPage(page, animationSpec = Motion.pageSwitch())
            } finally {
                if (pendingPage == page) pendingPage = null
            }
        }
    }
}

@Composable
fun rememberTabPagerController(pagerState: PagerState): TabPagerController {
    val scope = rememberCoroutineScope()
    return remember(pagerState, scope) { TabPagerController(pagerState, scope) }
}
