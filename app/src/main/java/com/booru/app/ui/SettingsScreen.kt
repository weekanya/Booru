@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app.ui

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
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
import androidx.compose.ui.unit.sp
import com.booru.app.BooruRepository
import com.booru.app.GalleryViewModel
import com.booru.app.data.ImageQuality
import com.booru.app.data.CustomBooruSource
import com.booru.app.data.BooruEngine
import com.booru.app.data.sanitizeBooruBaseUrl
import com.booru.app.R
import com.booru.app.data.AppLanguage
import com.booru.app.data.Strings
import kotlinx.coroutines.launch

enum class SettingsCategory(
    val titleRu: String,
    val titleEn: String,
    val icon: ImageVector
) {
    APPEARANCE("Внешний вид", "Appearance", Icons.Rounded.Palette),
    SOURCES("Источники", "Sources", Icons.Rounded.Cloud),
    CONTENT("Контент", "Content", Icons.Rounded.Tune),
    SYSTEM("Система", "System", Icons.Rounded.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showMessage = LocalShowMessage.current
    val lang = vm.language
    val scope = rememberCoroutineScope()

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
    var customNameError by remember { mutableStateOf<String?>(null) }
    var customUrlError by remember { mutableStateOf<String?>(null) }
    var showCustomApiKey by remember { mutableStateOf(false) }
    var customEngine by remember { mutableStateOf(BooruEngine.GELBOORU) }
    var customApiKey by remember { mutableStateOf("") }
    var customUserId by remember { mutableStateOf("") }
    var newBlacklistTag by remember { mutableStateOf("") }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var showFeedSourcesSheet by remember { mutableStateOf(false) }
    var blacklistFilterQuery by remember { mutableStateOf("") }
    var editingCustomSource by remember { mutableStateOf<CustomBooruSource?>(null) }

    if (showFeedSourcesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showFeedSourcesSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        SettingIconBadge(
                            icon = Icons.Rounded.Public,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = Strings.feedSourcesTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val toggleable = vm.toggleableSources
                            Text(
                                text = Strings.feedSourcesCount(toggleable.count { vm.isSourceEnabled(it) }, toggleable.size, lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SettingsGroupCard {
                        vm.toggleableSources.forEachIndexed { index, src ->
                            if (index > 0) SettingsDivider()
                            val enabled = vm.isSourceEnabled(src)
                            SettingSwitchItem(
                                title = BooruRepository.getSourceDisplayName(src),
                                subtitle = null,
                                icon = feedSourceIcon(src),
                                checked = enabled,
                                onCheckedChange = { vm.setSourceEnabled(src, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRule34Dialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showRule34Dialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
                                    Icon(Icons.Rounded.Close, Strings.closeBtn(lang), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = ShapeTokens.Medium,
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
                                    Icon(Icons.Rounded.Close, Strings.closeBtn(lang), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
    
                    FilledTonalButton(
                        onClick = {
                            val url = "https://rule34.xxx/index.php?page=account&s=options"
                            context.openUrlSafely(url, lang)
                        },
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelLarge)
                    }
    
                    Button(
                        onClick = {
                            vm.saveRule34Keys(rule34User, rule34Key)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showRule34Dialog = false }
                            showMessage(Strings.keysSavedToast(lang))
                        },
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showGelbooruDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showGelbooruDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
                                    Icon(Icons.Rounded.Close, Strings.closeBtn(lang), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = ShapeTokens.Medium,
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
                                    Icon(Icons.Rounded.Close, Strings.closeBtn(lang), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
    
                    FilledTonalButton(
                        onClick = {
                            val url = "https://gelbooru.com/index.php?page=account&s=options"
                            context.openUrlSafely(url, lang)
                        },
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelLarge)
                    }
    
                    Button(
                        onClick = {
                            vm.saveGelbooruKeys(gelbooruUser, gelbooruKey)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showGelbooruDialog = false }
                            showMessage(Strings.keysSavedToast(lang))
                        },
                        shape = ShapeTokens.Medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.saveBtn(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showQualityDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showQualityDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
                        Pair(ImageQuality.SAMPLE, Strings.qualitySample(lang)),
                        Pair(ImageQuality.ORIGINAL, Strings.qualityOriginal(lang)),
                        Pair(ImageQuality.SAVER, Strings.qualitySaver(lang))
                    )
    
                    options.forEachIndexed { index, (q, title) ->
                        SegmentedOptionItem(
                            title = title,
                            selected = vm.imageQuality == q,
                            index = index,
                            count = options.size,
                            icon = when (q) {
                                ImageQuality.SAMPLE -> Icons.Rounded.Speed
                                ImageQuality.ORIGINAL -> Icons.Rounded.HighQuality
                                ImageQuality.SAVER -> Icons.Rounded.DataSaverOn
                            },
                            onClick = {
                                vm.updateImageQuality(q)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { showQualityDialog = false }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomSourceDialog) {
        val isEditing = editingCustomSource != null
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddCustomSourceDialog = false
                    editingCustomSource = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
                        onValueChange = {
                            customName = it
                            customNameError = null
                        },
                        label = { Text(Strings.sourceNameHint(lang)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Badge, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        isError = customNameError != null,
                        supportingText = customNameError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        shape = ShapeTokens.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
    
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = {
                            customUrl = it
                            customUrlError = null
                        },
                        label = { Text(Strings.sourceUrlHint(lang)) },
                        placeholder = { Text("https://example.booru.org") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        isError = customUrlError != null,
                        supportingText = customUrlError?.let { msg -> { Text(msg) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
                        singleLine = true,
                        shape = ShapeTokens.Large,
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
                        shape = ShapeTokens.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
    
                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = { customApiKey = it },
                        label = { Text("API Key (Optional)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCustomApiKey = !showCustomApiKey }) {
                                Icon(
                                    if (showCustomApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showCustomApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        visualTransformation = if (showCustomApiKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        shape = ShapeTokens.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
    
                    Button(
                        onClick = {
                            val cleanName = customName.trim()
                            val cleanUrl = sanitizeBooruBaseUrl(customUrl.trim())
                            customNameError = when {
                                cleanName.isBlank() -> Strings.emptySourceNameError(lang)
                                com.booru.app.data.isBuiltInSourceName(cleanName) -> Strings.customSourceNameError(lang)
                                else -> null
                            }
                            customUrlError = if (!com.booru.app.data.isHttpsBooruUrl(cleanUrl)) Strings.customSourceUrlError(lang) else null
                            if (customNameError != null || customUrlError != null) return@Button
                            val targetId = editingCustomSource?.id ?: java.util.UUID.randomUUID().toString()
                            val newSource = CustomBooruSource(
                                id = targetId,
                                name = cleanName,
                                baseUrl = cleanUrl,
                                engine = customEngine
                            )
                            val wasActiveSource = editingCustomSource != null && (editingCustomSource?.id == vm.source || editingCustomSource?.key == vm.source)
                            val success = vm.addCustomSource(newSource, customApiKey.trim(), customUserId.trim())
                            if (success) {
                                if (wasActiveSource) {
                                    vm.selectSource(newSource.id)
                                }
                                showMessage(if (isEditing) Strings.sourceUpdatedSuccess(lang) else Strings.sourceAddedSuccess(lang))
                                customName = ""
                                customUrl = ""
                                customApiKey = ""
                                customUserId = ""
                                editingCustomSource = null
                                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddCustomSourceDialog = false }
                            }
                        },
                        shape = ShapeTokens.LargeIncreased,
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
    }

    if (showBlacklistDialog) {
        val initialBlacklistTags = remember { vm.tagBlacklist.toSet() }
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

        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = {
                    showBlacklistDialog = false
                    blacklistFilterQuery = ""
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
                            }
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
                            shape = ShapeTokens.Medium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTagAction() }),
                            modifier = Modifier.weight(1f)
                        )
    
                        val isAddActive = newBlacklistTag.isNotBlank()
                        val addBtnBg by animateColorAsState(
                            targetValue = if (isAddActive) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.surfaceContainerHighest,
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            label = "addBtnBg"
                        )
                        val addBtnIconTint by animateColorAsState(
                            targetValue = if (isAddActive) MaterialTheme.colorScheme.onPrimary
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            label = "addBtnTint"
                        )
    
                        IconButton(
                            onClick = addTagAction,
                            enabled = isAddActive,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(ShapeTokens.Medium)
                                .background(addBtnBg)
                                .bouncyPress()
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = Strings.addTagBtn(lang),
                                tint = addBtnIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
    
                    AnimatedVisibility(
                        visible = vm.tagBlacklist.size > 4,
                        enter = fadeIn(tween(280, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = Motion.spatialSlow()),
                        exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = Motion.spatialSlow())
                    ) {
                        Surface(
                            shape = ShapeTokens.Medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = if (blacklistFilterQuery.isNotEmpty()) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (blacklistFilterQuery.isEmpty()) {
                                        Text(
                                            text = Strings.searchBlacklistPlaceholder(lang),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = blacklistFilterQuery,
                                        onValueChange = { blacklistFilterQuery = it },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (blacklistFilterQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { blacklistFilterQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
    
                    AnimatedContent(
                        targetState = vm.tagBlacklist.isEmpty(),
                        transitionSpec = {
                            fadeIn(tween(300, easing = FastOutSlowInEasing))
                                .togetherWith(fadeOut(tween(220, easing = FastOutSlowInEasing)))
                        },
                        label = "blacklistContentTransition"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Surface(
                                shape = ShapeTokens.Large,
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
                                            key(tag) {
                                                BlacklistTagChip(
                                                    tag = tag,
                                                    animateEnter = tag !in initialBlacklistTags,
                                                    onRemove = { vm.removeBlacklistedTag(tag) }
                                                )
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
                        AnimatedVisibility(
                            visible = vm.tagBlacklist.isNotEmpty(),
                            enter = fadeIn(tween(250, easing = FastOutSlowInEasing)) + expandHorizontally(animationSpec = Motion.spatialSlow()),
                            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + shrinkHorizontally(animationSpec = Motion.spatialSlow())
                        ) {
                            AnimatedConfirmDeleteButton(
                                onConfirmed = {
                                    vm.clearBlacklist()
                                    blacklistFilterQuery = ""
                                },
                                lang = lang,
                                initialIcon = Icons.Rounded.DeleteSweep,
                                initialText = Strings.clearAllBlacklist(lang),
                                confirmText = if (lang == AppLanguage.RUSSIAN) "Удалить всё?" else Strings.confirmDeleteAction(lang)
                            )
                        }
    
                        if (vm.tagBlacklist.isEmpty()) {
                            Spacer(Modifier.width(1.dp))
                        }
    
                        Button(
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showBlacklistDialog = false
                                    blacklistFilterQuery = ""
                                }
                            },
                            shape = ShapeTokens.Medium,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            modifier = Modifier.bouncyPress()
                        ) {
                            Icon(Icons.Rounded.Done, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.closeBtn(lang), fontWeight = FontWeight.Bold)
                        }
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
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showPaletteDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
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
    
                    AppPalette.entries.forEachIndexed { index, pal ->
                        val swatchBrush = remember(pal, monetDynamicPrimary, monetDynamicSecondary) {
                            if (pal == AppPalette.MONET) {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(monetDynamicPrimary, monetDynamicSecondary)
                                )
                            } else {
                                androidx.compose.ui.graphics.SolidColor(pal.primaryColor)
                            }
                        }
                        SegmentedOptionItem(
                            title = pal.title,
                            selected = vm.palette == pal,
                            index = index,
                            count = AppPalette.entries.size,
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(swatchBrush)
                                )
                            },
                            onClick = {
                                vm.updatePalette(pal)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { showPaletteDialog = false }
                            }
                        )
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

    val categories = remember { SettingsCategory.entries }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { categories.size })

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = Strings.navSettings(lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        SettingsTabBar(
            titles = categories.map { cat ->
                when (cat) {
                    SettingsCategory.APPEARANCE -> if (lang == AppLanguage.RUSSIAN) "Вид" else "Style"
                    SettingsCategory.SOURCES -> if (lang == AppLanguage.RUSSIAN) "Источники" else "Sources"
                    SettingsCategory.CONTENT -> if (lang == AppLanguage.RUSSIAN) "Контент" else "Content"
                    SettingsCategory.SYSTEM -> if (lang == AppLanguage.RUSSIAN) "Система" else "System"
                }
            },
            icons = categories.map { cat ->
                when (cat) {
                    SettingsCategory.APPEARANCE -> Icons.Rounded.Palette
                    SettingsCategory.SOURCES -> Icons.Rounded.Public
                    SettingsCategory.CONTENT -> Icons.Rounded.Tune
                    SettingsCategory.SYSTEM -> Icons.Rounded.Settings
                }
            },
            pagerState = pagerState,
            onSelect = { index ->
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = index,
                        animationSpec = Motion.spatialDefault()
                    )
                }
            }
        )

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .readableContentWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 88.dp)
            ) {
                when (categories[page]) {
                    SettingsCategory.APPEARANCE -> {
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
                                        ThemeSwatch(
                                            primary = MaterialTheme.colorScheme.primary,
                                            secondary = MaterialTheme.colorScheme.secondaryContainer,
                                            tertiary = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(32.dp)
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

                            SettingsDivider()

                            SettingRowItem(
                                title = Strings.languageTitle(lang),
                                subtitle = "${lang.displayName} (${lang.englishName})",
                                icon = Icons.Rounded.Translate,
                                onClick = { showLanguageBottomSheet = true }
                            )
                        }
                    }

                    SettingsCategory.SOURCES -> {
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
                                                showMessage(Strings.sourceRemovedSuccess(lang))
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

                        SectionLabel(Strings.feedSourcesTitle(lang))

                        SettingsGroupCard {
                            val toggleable = vm.toggleableSources
                            SettingRowItem(
                                title = Strings.feedSourcesTitle(lang),
                                subtitle = Strings.feedSourcesCount(toggleable.count { vm.isSourceEnabled(it) }, toggleable.size, lang),
                                icon = Icons.Rounded.Public,
                                onClick = { showFeedSourcesSheet = true }
                            )
                        }
                    }

                    SettingsCategory.CONTENT -> {
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

                            SettingsDivider()

                            SettingRowItem(
                                title = Strings.clearRecommendationsTitle(lang),
                                subtitle = Strings.clearRecommendationsDesc(lang),
                                icon = Icons.Rounded.AutoAwesome,
                                trailing = {
                                    FilledTonalButton(
                                        onClick = {
                                            vm.clearRecommendationMemory {
                                                showMessage(Strings.clearRecommendationsSuccess(lang))
                                            }
                                        },
                                        shape = ShapeTokens.Large,
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
                        }
                    }

                    SettingsCategory.SYSTEM -> {
                        SectionLabel(Strings.securitySection(lang))

                        SettingsGroupCard {
                            SettingSwitchItem(
                                title = Strings.biometricLockTitle(lang),
                                subtitle = Strings.biometricLockSubtitle(lang),
                                icon = Icons.Rounded.Fingerprint,
                                checked = vm.biometricLockEnabled,
                                onCheckedChange = { vm.setBiometricLock(it) }
                            )

                            AnimatedVisibility(visible = vm.biometricLockEnabled) {
                                Column {
                                    SettingsDivider()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    ) {
                                        val timeoutOptions = listOf(0, 1, 5, 15)
                                        MD3SegmentedChoiceRow(
                                            options = timeoutOptions,
                                            selectedOption = vm.biometricLockTimeoutMin,
                                            onOptionSelected = { vm.setBiometricLockTimeout(it) },
                                            labelProvider = { min ->
                                                if (min == 0) Strings.lockTimeoutImmediately(lang)
                                                else Strings.lockTimeoutMinutes(min, lang)
                                            }
                                        )
                                    }
                                }
                            }
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
                                            context.openUrlSafely("https://github.com/weekanya/Booru", lang)
                                        },
                                        shape = ShapeTokens.Large,
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

                            SettingsDivider()

                            SettingRowItem(
                                title = Strings.checkUpdatesTitle(lang),
                                subtitle = Strings.checkUpdatesDesc(lang),
                                icon = Icons.Rounded.SystemUpdate,
                                trailing = {
                                    FilledTonalButton(
                                        onClick = { vm.checkForUpdates(isAutoCheck = false) },
                                        enabled = !vm.isCheckingUpdate,
                                        shape = ShapeTokens.Large,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .bouncyPress()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = Strings.checkUpdatesTitle(lang),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.graphicsLayer {
                                                    alpha = if (vm.isCheckingUpdate) 0f else 1f
                                                }
                                            )
                                            if (vm.isCheckingUpdate) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabBar(
    titles: List<String>,
    icons: List<ImageVector>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onSelect: (Int) -> Unit
) {
    val selectedIndex = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .readableContentWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        titles.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(index) },
                shapes = groupPosition(index, titles.size).toggleShapes(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { role = Role.Tab }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.clip(CircleShape)) {
        drawArc(color = primary, startAngle = 180f, sweepAngle = 180f, useCenter = true)
        drawArc(color = secondary, startAngle = 90f, sweepAngle = 90f, useCenter = true)
        drawArc(color = tertiary, startAngle = 0f, sweepAngle = 90f, useCenter = true)
    }
}

private fun feedSourceIcon(source: String): ImageVector = when (source) {
    BooruRepository.SOURCE_RULE34 -> Icons.Rounded.Explicit
    BooruRepository.SOURCE_GELBOORU -> Icons.Rounded.Image
    BooruRepository.SOURCE_REALBOORU -> Icons.Rounded.VideoLibrary
    BooruRepository.SOURCE_XBOORU -> Icons.Rounded.PhotoLibrary
    BooruRepository.SOURCE_TBIB -> Icons.Rounded.Public
    BooruRepository.SOURCE_YANDE -> Icons.Rounded.Collections
    BooruRepository.SOURCE_KONACHAN -> Icons.Rounded.Wallpaper
    BooruRepository.SOURCE_SAFEBOORU -> Icons.Rounded.Shield
    else -> Icons.Rounded.Language
}

@Composable
private fun SettingIconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = ShapeTokens.Medium,
        color = containerColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = ShapeTokens.ExtraLarge,
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
            SettingIconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDangerous: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            SettingIconBadge(
                icon = icon,
                containerColor = if (isDangerous && checked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (isDangerous && checked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            thumbContent = if (checked) {
                {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
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
        modifier = Modifier.padding(start = 76.dp, end = 20.dp),
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
                animationSpec = Motion.effectsDefault(),
                label = "segmentedBg"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = Motion.effectsDefault(),
                label = "segmentedContent"
            )

            Surface(
                onClick = { onOptionSelected(option) },
                shape = ShapeTokens.Large,
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
                        .padding(horizontal = if (options.size >= 4) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(animationSpec = tween(260, easing = LinearOutSlowInEasing)) +
                            expandHorizontally(
                                animationSpec = Motion.spatialDefault(),
                                expandFrom = Alignment.Start
                            ),
                        exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                            shrinkHorizontally(
                                animationSpec = Motion.spatialDefault(),
                                shrinkTowards = Alignment.Start
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(if (options.size >= 4) 15.dp else 18.dp)
                            )
                            Spacer(Modifier.width(if (options.size >= 4) 3.dp else 6.dp))
                        }
                    }
                    Text(
                        text = labelProvider(option),
                        style = if (options.size >= 4) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    val scope = rememberCoroutineScope()

    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeTop
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
    
                Column {
                    AppLanguage.entries.forEachIndexed { index, langOption ->
                        SegmentedOptionItem(
                            title = langOption.displayName,
                            supporting = langOption.englishName,
                            selected = langOption == currentLanguage,
                            index = index,
                            count = AppLanguage.entries.size,
                            icon = Icons.Rounded.Translate,
                            onClick = {
                                onLanguageSelected(langOption)
                                scope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlacklistTagChip(
    tag: String,
    animateEnter: Boolean = false,
    onRemove: () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(!animateEnter).apply {
            targetState = true
        }
    }
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(160, easing = FastOutLinearInEasing)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(160)
        ) + shrinkHorizontally(
            animationSpec = tween(160)
        )
    ) {
        Surface(
            shape = ShapeTokens.Medium,
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
                        .clickable {
                            if (visibleState.targetState) {
                                visibleState.targetState = false
                                coroutineScope.launch {
                                    delay(180)
                                    onRemove()
                                }
                            }
                        },
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

private fun android.content.Context.openUrlSafely(url: String, lang: AppLanguage) {
    val opened = runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
    }.isSuccess
    if (!opened) {
        android.widget.Toast.makeText(this, Strings.noBrowserFound(lang), android.widget.Toast.LENGTH_SHORT).show()
    }
}
