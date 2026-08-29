package com.example.boorugallery.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

object Motion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> softSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = 350f
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 500f
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.92f,
        stiffness = 260f
    )

    fun <T> enterTween(duration: Int = 320) = tween<T>(
        durationMillis = duration,
        easing = EmphasizedDecelerate
    )

    fun <T> exitTween(duration: Int = 220) = tween<T>(
        durationMillis = duration,
        easing = EmphasizedAccelerate
    )

    val TabTransition: AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)))
            .togetherWith(
                fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))
            )
    }
}

fun Modifier.bouncyClick(
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = Motion.softSpring(),
        label = "bouncyClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

fun Modifier.bouncyPress(scaleDown: Float = 0.94f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = Motion.softSpring(),
        label = "bouncyPressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
