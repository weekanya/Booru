@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.launch

@Composable
fun ExpressiveSheetHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    polygon: RoundedPolygon = MaterialShapes.Cookie9Sided,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    badgeSize: Dp = 52.dp,
    action: (@Composable () -> Unit)? = null
) {
    val appear = remember { Animatable(0f) }
    val spin = remember { Animatable(-40f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, Motion.spatialFast()) }
        spin.animateTo(0f, Motion.spatialDefault())
    }
    val shape = polygon.toShape()
    val idle = androidx.compose.animation.core.rememberInfiniteTransition(label = "sheetBadge")
    val turn by idle.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(16000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "sheetBadgeTurn"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(badgeSize)
                .graphicsLayer {
                    val s = 0.6f + 0.4f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = spin.value + turn }
                    .background(containerColor, shape)
            )
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(badgeSize * 0.46f))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            action()
        }
    }
}

@Composable
fun SheetResetButton(enabled: Boolean, contentDescription: String, onReset: () -> Unit) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    androidx.compose.material3.FilledTonalIconButton(
        onClick = {
            onReset()
            scope.launch {
                rotation.snapTo(0f)
                rotation.animateTo(-360f, Motion.spatialDefault())
            }
        },
        enabled = enabled,
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Rounded.RestartAlt,
            contentDescription = contentDescription,
            modifier = Modifier.graphicsLayer { rotationZ = rotation.value }
        )
    }
}
