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
        (fadeIn(animationSpec = Motion.effectsDefault()))
            .togetherWith(
                fadeOut(animationSpec = Motion.effectsFast())
            )
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

private class FractionCornerShape(private val fraction: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val radius = minOf(size.width, size.height) * fraction.coerceIn(0f, 0.5f)
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
        )
    }
}

@Composable
fun pressMorphShape(
    interactionSource: androidx.compose.foundation.interaction.InteractionSource,
    restingFraction: Float = 0.5f,
    pressedFraction: Float = 0.25f
): androidx.compose.ui.graphics.Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val fraction by animateFloatAsState(
        targetValue = if (pressed) pressedFraction else restingFraction,
        animationSpec = Motion.spatialFast(),
        label = "pressMorphFraction"
    )
    return FractionCornerShape(fraction)
}

@Composable
fun pressMorphShape(
    interactionSource: androidx.compose.foundation.interaction.InteractionSource,
    resting: androidx.compose.ui.unit.Dp,
    pressed: androidx.compose.ui.unit.Dp = resting * 0.5f
): androidx.compose.ui.graphics.Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val radius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) pressed else resting,
        animationSpec = Motion.spatialFast(),
        label = "pressMorphRadius"
    )
    return androidx.compose.foundation.shape.RoundedCornerShape(radius.coerceAtLeast(androidx.compose.ui.unit.Dp(0f)))
}

class MorphShape(
    private val morph: androidx.graphics.shapes.Morph,
    private val progress: Float
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        morph.asCubics(progress).forEachIndexed { i, c ->
            if (i == 0) path.moveTo(c.anchor0X * size.width, c.anchor0Y * size.height)
            path.cubicTo(
                c.control0X * size.width, c.control0Y * size.height,
                c.control1X * size.width, c.control1Y * size.height,
                c.anchor1X * size.width, c.anchor1Y * size.height
            )
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
