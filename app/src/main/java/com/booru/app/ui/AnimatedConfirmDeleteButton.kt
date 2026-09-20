package com.booru.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.booru.app.data.AppLanguage
import com.booru.app.data.Strings
import kotlinx.coroutines.delay

@Composable
fun AnimatedConfirmDeleteButton(
    onConfirmed: () -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier,
    initialIcon: ImageVector = Icons.Rounded.DeleteOutline,
    initialText: String? = null,
    confirmText: String = Strings.confirmDeleteAction(lang),
    resetTimeoutMs: Long = 3500L,
    height: Dp = 38.dp,
    compact: Boolean = false,
    idleContainerColor: Color = if (compact) Color.Transparent else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
    idleContentColor: Color = MaterialTheme.colorScheme.error
) {
    var isConfirming by remember { mutableStateOf(false) }

    LaunchedEffect(isConfirming) {
        if (isConfirming) {
            delay(resetTimeoutMs)
            isConfirming = false
        }
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isConfirming) MaterialTheme.colorScheme.error else idleContainerColor,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "confirmDeleteContainer"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isConfirming) MaterialTheme.colorScheme.onError else idleContentColor,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "confirmDeleteContent"
    )

    Surface(
        onClick = {
            if (isConfirming) {
                isConfirming = false
                onConfirmed()
            } else {
                isConfirming = true
            }
        },
        shape = CircleShape,
        color = animatedContainerColor,
        contentColor = animatedContentColor,
        modifier = modifier
            .height(height)
            .bouncyPress()
    ) {
        AnimatedContent(
            targetState = isConfirming,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.90f, animationSpec = tween(160, easing = FastOutSlowInEasing))
                    ).using(
                        SizeTransform(clip = false) { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
                    )
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxHeight(),
            label = "confirmDeleteContentAnim"
        ) { confirming ->
            if (confirming) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = animatedContentColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = animatedContentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = if (initialText != null) 12.dp else if (compact) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = initialIcon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = animatedContentColor
                    )
                    if (initialText != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = initialText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = animatedContentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
