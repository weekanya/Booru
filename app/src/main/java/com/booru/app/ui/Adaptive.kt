package com.booru.app.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass(val extraGridColumns: Int) {
    Compact(0),
    Medium(1),
    Expanded(2);

    val usesNavigationRail: Boolean get() = this != Compact

    companion object {
        fun fromWidth(width: Dp): WindowWidthClass = when {
            width >= 840.dp -> Expanded
            width >= 600.dp -> Medium
            else -> Compact
        }
    }
}

val LocalWindowWidthClass = compositionLocalOf { WindowWidthClass.Compact }

val ReadableContentMaxWidth = 840.dp

fun Modifier.readableContentWidth(): Modifier = widthIn(max = ReadableContentMaxWidth)

@Composable
fun adaptiveColumns(base: Int): Int = base + LocalWindowWidthClass.current.extraGridColumns
