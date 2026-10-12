package com.booru.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FloatingTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0
)

/** Floating MD3E toolbar: the selected tab expands into a labelled pill, the others stay icons. */
@Composable
fun FloatingTabToolbar(
    tabs: List<FloatingTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                val container by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tween(200),
                    label = "tabContainer"
                )
                val content by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(200),
                    label = "tabContent"
                )
                Surface(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    color = container,
                    contentColor = content,
                    modifier = Modifier
                        .height(48.dp)
                        .semantics { role = Role.Tab; this.selected = isSelected }
                ) {
                    Row(
                        modifier = Modifier
                            .animateContentSize(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
                            .padding(horizontal = if (isSelected) 18.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = if (isSelected) tab.selectedIcon else tab.icon
                        val description = if (isSelected) null else tab.title
                        if (tab.badgeCount > 0 && !isSelected) {
                            BadgedBox(badge = { Badge { Text(if (tab.badgeCount > 99) "99+" else "${tab.badgeCount}") } }) {
                                Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
                            }
                        } else {
                            Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
