package com.booru.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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

    LaunchedEffect(Unit) {
        vm.updateCacheSize()
    }

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
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var blacklistFilterQuery by remember { mutableStateOf("") }
    var editingCustomSource by remember { mutableStateOf<CustomBooruSource?>(null) }

    if (showRule34Dialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRule34Dialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Rule34.xxx API Keys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Strings.rule34DialogDesc(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { showRule34Dialog = false },
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

                OutlinedTextField(
                    value = rule34User,
                    onValueChange = { rule34User = it },
                    label = { Text("User ID") },
                    placeholder = { Text("123456") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (rule34User.isNotEmpty()) {
                            IconButton(onClick = { rule34User = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rule34Key,
                    onValueChange = { rule34Key = it },
                    label = { Text("API Key") },
                    placeholder = { Text("API Key") },
                    leadingIcon = {
                        Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (rule34Key.isNotEmpty()) {
                            IconButton(onClick = { rule34Key = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                FilledTonalButton(
                    onClick = {
                        val url = "https://rule34.xxx/index.php?page=account&s=options"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        vm.saveRule34Keys(rule34User, rule34Key)
                        showRule34Dialog = false
                        Toast.makeText(context, Strings.keysSavedToast(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showGelbooruDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGelbooruDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.VpnKey,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Gelbooru API Keys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Strings.gelbooruDialogDesc(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { showGelbooruDialog = false },
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

                OutlinedTextField(
                    value = gelbooruUser,
                    onValueChange = { gelbooruUser = it },
                    label = { Text("User ID") },
                    placeholder = { Text("User ID") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (gelbooruUser.isNotEmpty()) {
                            IconButton(onClick = { gelbooruUser = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gelbooruKey,
                    onValueChange = { gelbooruKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("API Key") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (gelbooruKey.isNotEmpty()) {
                            IconButton(onClick = { gelbooruKey = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                FilledTonalButton(
                    onClick = {
                        val url = "https://gelbooru.com/index.php?page=account&s=options"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        vm.saveGelbooruKeys(gelbooruUser, gelbooruKey)
                        showGelbooruDialog = false
                        Toast.makeText(context, Strings.keysSavedToast(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQualityDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQualityDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.imageQualityTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val options = listOf(
                    Triple(
                        ImageQuality.SAMPLE,
                        Strings.qualitySample(lang),
                        if (lang == AppLanguage.RUSSIAN) "Баланс качества и быстрой загрузки фото/видео" else "Balanced quality and fast loading for media"
                    ),
                    Triple(
                        ImageQuality.ORIGINAL,
                        Strings.qualityOriginal(lang),
                        if (lang == AppLanguage.RUSSIAN) "Исходное максимальное разрешение без сжатия" else "Full uncompressed resolution and source video"
                    ),
                    Triple(
                        ImageQuality.SAVER,
                        Strings.qualitySaver(lang),
                        if (lang == AppLanguage.RUSSIAN) "Экономия трафика и облегченные превью" else "Compressed previews to reduce data usage"
                    )
                )

                options.forEach { (q, title, subtitle) ->
                    val isSelected = (vm.imageQuality == q)
                    val icon = when (q) {
                        ImageQuality.SAMPLE -> Icons.Rounded.Speed
                        ImageQuality.ORIGINAL -> Icons.Rounded.HighQuality
                        ImageQuality.SAVER -> Icons.Rounded.DataSaverOn
                    }

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "qualityBg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "qualityContent"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        label = "qualityIcon"
                    )

                    Surface(
                        onClick = {
                            vm.updateImageQuality(q)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = containerColor,
                        contentColor = contentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .bouncyPress()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = contentColor
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Crossfade(
                                    targetState = isSelected,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                    label = "qualityCheck"
                                ) { checked ->
                                    if (checked) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
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

    if (showAddCustomSourceDialog) {
        val isEditing = editingCustomSource != null
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showAddCustomSourceDialog = false
                editingCustomSource = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isEditing) Icons.Rounded.Edit else Icons.Rounded.AddCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isEditing) Strings.editSourceTitle(lang) else Strings.addSourceTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(Strings.sourceNameHint(lang)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Badge, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text(Strings.sourceUrlHint(lang)) },
                    placeholder = { Text("https://example.booru.org") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = Strings.sourceEngineLabel(lang),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                MD3SegmentedChoiceRow(
                    options = BooruEngine.entries,
                    selectedOption = customEngine,
                    onOptionSelected = { customEngine = it },
                    labelProvider = { engine ->
                        when (engine) {
                            BooruEngine.GELBOORU -> "Gelbooru"
                            BooruEngine.MOEBOORU -> "Moebooru"
                            BooruEngine.DANBOORU -> "Danbooru"
                        }
                    }
                )

                OutlinedTextField(
                    value = customUserId,
                    onValueChange = { customUserId = it },
                    label = { Text("User ID / Login (Optional)") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("API Key (Optional)") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cleanName = customName.trim()
                        val cleanUrl = sanitizeBooruBaseUrl(customUrl.trim())
                        if (cleanName.isBlank()) {
                            Toast.makeText(context, Strings.emptySourceNameError(lang), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!cleanUrl.startsWith("https://", ignoreCase = true)) {
                            Toast.makeText(context, Strings.invalidHttpsUrlError(lang), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val targetId = editingCustomSource?.id ?: cleanName.lowercase().replace(" ", "_")
                        val newSource = CustomBooruSource(
                            id = targetId,
                            name = cleanName,
                            baseUrl = cleanUrl,
                            engine = customEngine
                        )
                        val wasEditingName = editingCustomSource != null && editingCustomSource?.name == vm.source && cleanName != editingCustomSource?.name
                        val success = vm.addCustomSource(newSource, customApiKey.trim(), customUserId.trim())
                        if (success) {
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
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bouncyPress()
                ) {
                    Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
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

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showBlacklistDialog = false
                blacklistFilterQuery = ""
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = Strings.tagBlacklistTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${vm.tagBlacklist.size} ${if (lang == AppLanguage.RUSSIAN) "тегов" else "tags"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
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

                    IconButton(
                        onClick = addTagAction,
                        enabled = newBlacklistTag.isNotBlank(),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (newBlacklistTag.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = Strings.addTagBtn(lang),
                            tint = if (newBlacklistTag.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                                tint = MaterialTheme.colorScheme.primary,
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
                        shape = RoundedCornerShape(12.dp),
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
                            .heightIn(min = 60.dp, max = 240.dp)
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
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.bouncyPress()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "#",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable { vm.removeBlacklistedTag(tag) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (vm.tagBlacklist.isNotEmpty()) {
                        AnimatedConfirmDeleteButton(
                            onConfirmed = { vm.clearBlacklist() },
                            lang = lang,
                            initialIcon = Icons.Rounded.DeleteSweep,
                            initialText = Strings.clearAllBlacklist(lang),
                            confirmText = if (lang == AppLanguage.RUSSIAN) "Удалить всё?" else Strings.confirmDeleteAction(lang)
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            showBlacklistDialog = false
                            blacklistFilterQuery = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Done, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.closeBtn(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                if (isDark) Color(0xFFD0BCFF) else Color(0xFF7E5260)
            }
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPaletteDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.colorPaletteTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

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
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .bouncyPress()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(swatchBrush)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = pal.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLanguageBottomSheet) {
        LanguageSelectionBottomSheet(
            currentLanguage = vm.language,
            onLanguageSelected = { vm.updateLanguage(it) },
            onDismiss = { showLanguageBottomSheet = false }
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
            SettingRowItem(
                title = Strings.languageTitle(lang),
                subtitle = "${lang.displayName} (${lang.englishName})",
                icon = Icons.Rounded.Translate,
                onClick = { showLanguageBottomSheet = true }
            )
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
                                customApiKey = vm.getCustomSourceApiKey(customSource.id)
                                customUserId = vm.getCustomSourceUserId(customSource.id)
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
                                customApiKey = vm.getCustomSourceApiKey(customSource.id)
                                customUserId = vm.getCustomSourceUserId(customSource.id)
                                showAddCustomSourceDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedConfirmDeleteButton(
                            onConfirmed = {
                                vm.removeCustomSource(customSource.id)
                                Toast.makeText(context, Strings.sourceRemovedSuccess(lang), Toast.LENGTH_SHORT).show()
                            },
                            lang = lang,
                            initialIcon = Icons.Rounded.DeleteOutline,
                            confirmText = Strings.confirmDeleteAction(lang),
                            compact = true
                        )
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
                subtitle = "${Strings.clearCacheDesc(lang)} • ${Strings.favoritesStorageDesc(lang, vm.favoritesStorageSizeFormatted)}",
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
                title = Strings.clearRecommendationsTitle(lang),
                subtitle = Strings.clearRecommendationsDesc(lang),
                icon = Icons.Rounded.AutoAwesome,
                trailing = {
                    FilledTonalButton(
                        onClick = {
                            vm.clearRecommendationMemory {
                                Toast.makeText(context, Strings.clearRecommendationsSuccess(lang), Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.bouncyPress()
                    ) {
                        Text(
                            text = Strings.resetFilters(lang),
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
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "segmentedBg"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "segmentedContent"
            )

            Surface(
                onClick = { onOptionSelected(option) },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
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
                        enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                            expandHorizontally(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Start
                            ),
                        exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                            shrinkHorizontally(
                                animationSpec = tween(200, easing = FastOutLinearInEasing),
                                shrinkTowards = Alignment.Start
                            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionBottomSheet(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = Strings.chooseLanguageTitle(currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Strings.languageTitle(currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { langOption ->
                    val isSelected = langOption == currentLanguage

                    Surface(
                        onClick = {
                            onLanguageSelected(langOption)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyPress()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = langOption.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = langOption.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
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
}
