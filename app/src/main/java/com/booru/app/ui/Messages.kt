package com.booru.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MessageRouter(private val fallback: (String) -> Unit) {
    private val sinks = ArrayList<(String) -> Unit>()

    fun show(message: String) {
        (sinks.lastOrNull() ?: fallback)(message)
    }

    fun register(sink: (String) -> Unit): () -> Unit {
        sinks.add(sink)
        return { sinks.remove(sink) }
    }
}

val LocalMessageRouter = staticCompositionLocalOf { MessageRouter {} }

@Composable
fun FlatSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    Surface(
        shape = ShapeTokens.ExtraSmall,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = modifier
            .padding(12.dp)
            .heightIn(min = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp)
            )
            data.visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = { data.performAction() },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (data.visuals.withDismissAction) {
                IconButton(
                    onClick = { data.dismiss() },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun FlatSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { FlatSnackbar(it) }
}

@Composable
fun SheetSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val router = LocalMessageRouter.current
    DisposableEffect(router, hostState) {
        val unregister = router.register { message ->
            scope.launch {
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(message)
            }
        }
        onDispose { unregister() }
    }
    FlatSnackbarHost(hostState = hostState, modifier = modifier)
}
