package com.booru.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

object Motion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    const val FADE_THROUGH_OUT_MS = 90
    const val FADE_THROUGH_IN_MS = 210

    fun <T> spatialDefault() = spring<T>(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> spatialFast() = spring<T>(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> spatialSlow() = spring<T>(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> effectsDefault() = spring<T>(dampingRatio = 1f, stiffness = 1600f)
    fun <T> effectsFast() = spring<T>(dampingRatio = 1f, stiffness = 3800f)

    fun <T> softSpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 800f
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = 0.76f,
        stiffness = 1400f
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = 600f
    )

    fun <T> enterTween(duration: Int = 220) = tween<T>(
        durationMillis = duration,
        easing = EmphasizedDecelerate
    )

    fun <T> exitTween(duration: Int = 160) = tween<T>(
        durationMillis = duration,
        easing = EmphasizedAccelerate
    )

    val TabTransition: AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)))
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
        animationSpec = Motion.snappySpring(),
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
    val animScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer {
            scaleX = animScale.value
            scaleY = animScale.value
        }
        .pointerInput(scaleDown) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch {
                    animScale.animateTo(
                        targetValue = scaleDown,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 1200f
                        )
                    )
                }
                waitForUpOrCancellation()
                scope.launch {
                    animScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.68f,
                            stiffness = 900f
                        )
                    )
                }
            }
        }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private object SmoothSheetMotionScheme : androidx.compose.material3.MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 560f)
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 240f)
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 700f)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 420f)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 280f)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SheetMotion(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialExpressiveTheme(
        motionScheme = SmoothSheetMotionScheme,
        content = content
    )
}
