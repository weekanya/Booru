package com.booru.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booru.app.GalleryViewModel
import com.booru.app.data.ImageQuality
import com.booru.app.data.CustomBooruSource
import com.booru.app.data.BooruEngine
import com.booru.app.data.sanitizeBooruBaseUrl
import com.booru.app.R
import com.booru.app.data.AppLanguage
import com.booru.app.data.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = vm.language

    var rule34User by remember(vm.rule34UserId) { mutableStateOf(vm.rule34UserId) }
    var rule34Key by remember(vm.rule34ApiKey) { mutableStateOf(vm.rule34ApiKey) }

    var gelbooruUser by remember(vm.gelbooruUserId) { mutableStateOf(vm.gelbooruUserId) }
    var gelbooruKey by remember(vm.gelbooruApiKey) { mutableStateOf(vm.gelbooruApiKey) }

    var showRule34Dialog by remember { mutableStateOf(false) }
    var showGelbooruDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAddCustomSourceDialog by remember { mutableStateOf(false) }

    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var customEngine by remember { mutableStateOf(BooruEngine.GELBOORU) }
    var customApiKey by remember { mutableStateOf("") }
    var customUserId by remember { mutableStateOf("") }
    var newBlacklistTag by remember { mutableStateOf("") }
    var showClearBlacklistConfirm by remember { mutableStateOf(false) }
    var blacklistFilterQuery by remember { mutableStateOf("") }
    var editingCustomSource by remember { mutableStateOf<CustomBooruSource?>(null) }

    if (showRule34Dialog) {
        AlertDialog(
            onDismissRequest = { showRule34Dialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Rule34.xxx API Keys", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showRule34Dialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        Strings.rule34DialogDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rule34User,
                        onValueChange = { rule34User = it },
                        label = { Text("User ID") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rule34Key,
                        onValueChange = { rule34Key = it },
                        label = { Text("API Key") },
                        placeholder = { Text("API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val url = "https://rule34.xxx/index.php?page=account&s=options"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(14.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveRule34Keys(rule34User, rule34Key)
                        showRule34Dialog = false
                        Toast.makeText(context, Strings.keysSavedToast(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showGelbooruDialog) {
        AlertDialog(
            onDismissRequest = { showGelbooruDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Gelbooru API Keys", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showGelbooruDialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        Strings.gelbooruDialogDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = gelbooruUser,
                        onValueChange = { gelbooruUser = it },
                        label = { Text("User ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gelbooruKey,
                        onValueChange = { gelbooruKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val url = "https://gelbooru.com/index.php?page=account&s=options"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(14.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveGelbooruKeys(gelbooruUser, gelbooruKey)
                        showGelbooruDialog = false
                        Toast.makeText(context, Strings.keysSavedToast(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(Strings.imageQualityTitle(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showQualityDialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val options = listOf(
                        ImageQuality.SAMPLE to Strings.qualitySample(lang),
                        ImageQuality.ORIGINAL to Strings.qualityOriginal(lang),
                        ImageQuality.SAVER to Strings.qualitySaver(lang)
                    )
                    options.forEach { (q, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    vm.updateImageQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (vm.imageQuality == q),
                                onClick = {
                                    vm.updateImageQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAddCustomSourceDialog) {
        val isEditing = editingCustomSource != null
        AlertDialog(
            onDismissRequest = {
                showAddCustomSourceDialog = false
                editingCustomSource = null
            },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            if (isEditing) Icons.Rounded.Edit else Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (isEditing) Strings.editSourceTitle(lang) else Strings.addSourceTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            showAddCustomSourceDialog = false
                            editingCustomSource = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(Strings.sourceNameHint(lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text(Strings.sourceUrlHint(lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        Strings.sourceEngineLabel(lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BooruEngine.entries.forEach { engine ->
                            val selected = (customEngine == engine)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { customEngine = engine }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = when (engine) {
                                            BooruEngine.GELBOORU -> "Gelbooru"
                                            BooruEngine.MOEBOORU -> "Moebooru"
                                            BooruEngine.DANBOORU -> "Danbooru / e621"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customUserId,
                        onValueChange = { customUserId = it },
                        label = { Text("User ID / Login (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = { customApiKey = it },
                        label = { Text("API Key (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = customName.trim()
                        val cleanUrl = sanitizeBooruBaseUrl(customUrl.trim())
                        if (cleanName.isNotBlank() && cleanUrl.isNotBlank() && cleanUrl.startsWith("http")) {
                            val targetId = editingCustomSource?.id ?: cleanName.lowercase().replace(" ", "_")
                            val newSource = CustomBooruSource(
                                id = targetId,
                                name = cleanName,
                                baseUrl = cleanUrl,
                                engine = customEngine,
                                apiKey = customApiKey.trim(),
                                userId = customUserId.trim()
                            )
                            val wasEditingName = editingCustomSource != null && editingCustomSource?.name == vm.source && cleanName != editingCustomSource?.name
                            vm.addCustomSource(newSource)
                            if (wasEditingName) {
                                vm.selectSource(cleanName)
                            }
                            Toast.makeText(
                                context,
                                if (isEditing) Strings.sourceUpdatedSuccess(lang) else Strings.sourceAddedSuccess(lang),
                                Toast.LENGTH_SHORT
                            ).show()
                            customName = ""
                            customUrl = ""
                            customApiKey = ""
                            customUserId = ""
                            editingCustomSource = null
                            showAddCustomSourceDialog = false
                        } else {
                            Toast.makeText(context, "Invalid name or URL (must start with http/https)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBlacklistDialog) {
        val addTagAction = {
            if (newBlacklistTag.isNotBlank()) {
                val tagsToAdd = newBlacklistTag
                    .split(Regex("[\\s,]+"))
                    .map { it.trim().removePrefix("#").replace(' ', '_') }
                    .filter { it.isNotBlank() }
                tagsToAdd.forEach { tag ->
                    vm.addBlacklistedTag(tag)
                }
                newBlacklistTag = ""
            }
        }

        val filteredBlacklist = remember(vm.tagBlacklist, blacklistFilterQuery) {
            if (blacklistFilterQuery.isBlank()) {
                vm.tagBlacklist.toList().sorted()
            } else {
                vm.tagBlacklist.filter { it.contains(blacklistFilterQuery.trim(), ignoreCase = true) }.sorted()
            }
        }

        AlertDialog(
            onDismissRequest = {
                showBlacklistDialog = false
                blacklistFilterQuery = ""
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = Strings.tagBlacklistTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${vm.tagBlacklist.size} ${if (lang == AppLanguage.RUSSIAN) "тегов" else "tags"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            showBlacklistDialog = false
                            blacklistFilterQuery = ""
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Strings.tagBlacklistDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newBlacklistTag,
                            onValueChange = { newBlacklistTag = it },
                            placeholder = {
                                Text(
                                    Strings.addTagPlaceholder(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (newBlacklistTag.isNotEmpty()) {
                                    IconButton(
                                        onClick = { newBlacklistTag = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTagAction() }),
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = addTagAction,
                            enabled = newBlacklistTag.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = Strings.addTagBtn(lang),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }


                    if (vm.tagBlacklist.size > 6) {
                        OutlinedTextField(
                            value = blacklistFilterQuery,
                            onValueChange = { blacklistFilterQuery = it },
                            placeholder = {
                                Text(
                                    Strings.searchBlacklistPlaceholder(lang),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (blacklistFilterQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { blacklistFilterQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }

                    if (vm.tagBlacklist.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = Strings.noBlacklistedTags(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Strings.emptyBlacklistHint(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .animateContentSize()
                        ) {
                            if (filteredBlacklist.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.RUSSIAN) "Ничего не найдено" else "No matching tags",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    filteredBlacklist.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.bouncyPress()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "#$tag",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                                        .clickable { vm.removeBlacklistedTag(tag) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = "Remove",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (vm.tagBlacklist.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearBlacklistConfirm = true }
                        ) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Strings.clearAllBlacklist(lang),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    FilledTonalButton(
                        onClick = {
                            showBlacklistDialog = false
                            blacklistFilterQuery = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(Strings.closeBtn(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (showClearBlacklistConfirm) {
        AlertDialog(
            onDismissRequest = { showClearBlacklistConfirm = false },
            shape = RoundedCornerShape(22.dp),
            icon = {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = Strings.clearBlacklistConfirmTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = Strings.clearBlacklistConfirmDesc(vm.tagBlacklist.size, lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearBlacklist()
                        showClearBlacklistConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Strings.clearAllBlacklist(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearBlacklistConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Strings.cancelBtn(lang))
                }
            }
        )
    }

    if (showPaletteDialog) {
        val isDark = when (vm.themeMode) {
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        val monetDynamicPrimary = remember(isDark) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).primary
                else androidx.compose.material3.dynamicLightColorScheme(context).primary
            } else {
                Color(0xFF6750A4)
            }
        }
        val monetDynamicSecondary = remember(isDark) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).tertiary
                else androidx.compose.material3.dynamicLightColorScheme(context).tertiary
            } else {
                if (isDark) {
                    Color(0xFFD0BCFF)
                } else {
                    Color(0xFF7E5260)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(Strings.colorPaletteTitle(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showPaletteDialog = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    AppPalette.entries.forEach { pal ->
                        val isSelected = vm.palette == pal
                        val swatchBrush = remember(pal, monetDynamicPrimary, monetDynamicSecondary) {
                            if (pal == AppPalette.MONET) {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(monetDynamicPrimary, monetDynamicSecondary)
                                )
                            } else {
                                androidx.compose.ui.graphics.SolidColor(pal.primaryColor)
                            }
                        }

                        Surface(
                            onClick = {
                                vm.updatePalette(pal)
                                showPaletteDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyPress()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(swatchBrush)
                                        .then(
                                            if (isSelected)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pal.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        vm.updatePalette(pal)
                                        showPaletteDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
    ) {
        Text(
            text = Strings.navSettings(lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )

        SectionLabel(Strings.languageSection(lang))

        SettingsGroupCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = Strings.languageTitle(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (lang == AppLanguage.ENGLISH) "English (Default)" else "Русский язык",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                MD3SegmentedChoiceRow(
                    options = listOf(AppLanguage.ENGLISH, AppLanguage.RUSSIAN),
                    selectedOption = vm.language,
                    onOptionSelected = { vm.updateLanguage(it) },
                    labelProvider = { l ->
                        when (l) {
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.RUSSIAN -> "Русский"
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.authSection(lang))

        SettingsGroupCard {
            SettingRowItem(
                title = "Rule34.xxx API",
                subtitle = if (vm.rule34ApiKey.isNotBlank()) "Configured (User ID: ${vm.rule34UserId})" else Strings.tapToEnterKeys(lang),
                icon = Icons.Rounded.Key,
                onClick = { showRule34Dialog = true }
            )

            SettingsDivider()

            SettingRowItem(
                title = "Gelbooru API",
                subtitle = if (vm.gelbooruApiKey.isNotBlank()) "Configured (User ID: ${vm.gelbooruUserId})" else Strings.tapToEnterKeys(lang),
                icon = Icons.Rounded.VpnKey,
                onClick = { showGelbooruDialog = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.customSourcesTitle(lang))

        SettingsGroupCard {
            SettingRowItem(
                title = Strings.addSourceTitle(lang),
                subtitle = if (vm.customSources.isEmpty()) Strings.noCustomSources(lang) else "${vm.customSources.size} custom sources",
                icon = Icons.Rounded.AddCircleOutline,
                onClick = {
                    editingCustomSource = null
                    customName = ""
                    customUrl = ""
                    customEngine = BooruEngine.GELBOORU
                    customApiKey = ""
                    customUserId = ""
                    showAddCustomSourceDialog = true
                }
            )

            if (vm.customSources.isNotEmpty()) {
                vm.customSources.forEach { customSource ->
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingCustomSource = customSource
                                customName = customSource.name
                                customUrl = customSource.baseUrl
                                customEngine = customSource.engine
                                customApiKey = customSource.apiKey
                                customUserId = customSource.userId
                                showAddCustomSourceDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    customSource.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = customSource.engine.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                customSource.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                editingCustomSource = customSource
                                customName = customSource.name
                                customUrl = customSource.baseUrl
                                customEngine = customSource.engine
                                customApiKey = customSource.apiKey
                                customUserId = customSource.userId
                                showAddCustomSourceDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { vm.removeCustomSource(customSource.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.contentSection(lang))

        SettingsGroupCard {
            SettingRowItem(
                title = Strings.imageQualityTitle(lang),
                subtitle = when (vm.imageQuality) {
                    ImageQuality.ORIGINAL -> Strings.qualityOriginal(lang)
                    ImageQuality.SAVER    -> Strings.qualitySaver(lang)
                    ImageQuality.SAMPLE   -> Strings.qualitySample(lang)
                },
                icon = Icons.Rounded.HighQuality,
                onClick = { showQualityDialog = true }
            )

            SettingsDivider()

            SettingRowItem(
                title = Strings.tagBlacklistTitle(lang),
                subtitle = if (vm.tagBlacklist.isEmpty()) Strings.noBlacklistedTags(lang) else "${vm.tagBlacklist.size} tags blocked",
                icon = Icons.Rounded.Block,
                onClick = { showBlacklistDialog = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.appearanceSection(lang))

        SettingsGroupCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (vm.themeMode) {
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = Strings.darkThemeTitle(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (vm.themeMode) {
                                ThemeMode.SYSTEM -> Strings.themeModeSystem(lang)
                                ThemeMode.DARK -> Strings.themeModeDark(lang)
                                ThemeMode.LIGHT -> Strings.themeModeLight(lang)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                MD3SegmentedChoiceRow(
                    options = ThemeMode.entries,
                    selectedOption = vm.themeMode,
                    onOptionSelected = { vm.updateThemeMode(it) },
                    labelProvider = { mode ->
                        when (mode) {
                            ThemeMode.SYSTEM -> if (lang == AppLanguage.RUSSIAN) "Авто" else "Auto"
                            ThemeMode.DARK -> if (lang == AppLanguage.RUSSIAN) "Тёмная" else "Dark"
                            ThemeMode.LIGHT -> if (lang == AppLanguage.RUSSIAN) "Светлая" else "Light"
                        }
                    }
                )
            }

            SettingsDivider()

            val isDark = when (vm.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            val monetDynamicPrimary = remember(isDark) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).primary
                    else androidx.compose.material3.dynamicLightColorScheme(context).primary
                } else {
                    Color(0xFF6750A4)
                }
            }
            val monetDynamicSecondary = remember(isDark) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).tertiary
                    else androidx.compose.material3.dynamicLightColorScheme(context).tertiary
                } else {
                    Color(0xFF7E5260)
                }
            }
            val currentSwatchBrush = remember(vm.palette, monetDynamicPrimary, monetDynamicSecondary) {
                if (vm.palette == AppPalette.MONET) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(monetDynamicPrimary, monetDynamicSecondary)
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(vm.palette.primaryColor)
                }
            }

            SettingRowItem(
                title = Strings.colorPaletteTitle(lang),
                subtitle = vm.palette.title,
                icon = Icons.Rounded.Palette,
                onClick = { showPaletteDialog = true },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(currentSwatchBrush)
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.dataSection(lang))

        SettingsGroupCard {
            SettingRowItem(
                title = Strings.aboutAppTitle(lang),
                subtitle = Strings.aboutAppDesc(lang),
                icon = Icons.Rounded.Info,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/weekanya/Booru"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.bouncyPress()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Source code",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingRowItem(
                title = Strings.clearCacheTitle(lang),
                subtitle = Strings.clearCacheDesc(lang),
                icon = Icons.Rounded.CleaningServices,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            vm.clearCache {
                                Toast.makeText(context, Strings.clearCacheSuccess(lang), Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !vm.isClearingCache,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.bouncyPress()
                    ) {
                        if (vm.isClearingCache) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            Text(
                                text = "${Strings.clearBtn(lang)} (${vm.cacheSizeFormatted})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingRowItem(
                title = Strings.checkUpdatesTitle(lang),
                subtitle = Strings.checkUpdatesDesc(lang),
                icon = Icons.Rounded.SystemUpdate,
                trailing = {
                    if (vm.isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        FilledTonalButton(
                            onClick = { vm.checkForUpdates(isAutoCheck = false) },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.bouncyPress()
                        ) {
                            Text(
                                text = Strings.checkUpdatesTitle(lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(84.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun SettingRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDangerous: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDangerous && checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (isDangerous) {
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onError,
                    checkedTrackColor = MaterialTheme.colorScheme.error
                )
            } else {
                SwitchDefaults.colors()
            }
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun <T> MD3SegmentedChoiceRow(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption

            val containerColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "segmentedBg"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "segmentedContent"
            )

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.02f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "segmentedScale"
            )

            Surface(
                onClick = { onOptionSelected(option) },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .bouncyPress()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(200)) + expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Text(
                        text = labelProvider(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
