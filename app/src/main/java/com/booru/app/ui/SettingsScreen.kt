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
    var customAccessError by remember { mutableStateOf<String?>(null) }
    var customEngine by remember { mutableStateOf(BooruEngine.GELBOORU) }
    var customApiKey by remember { mutableStateOf("") }
    var customUserId by remember { mutableStateOf("") }
    var newBlacklistTag by remember { mutableStateOf("") }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var showFeedSourcesSheet by remember { mutableStateOf(false) }
    var blacklistFilterQuery by remember { mutableStateOf("") }
    var editingCustomSource by remember { mutableStateOf<CustomBooruSource?>(null) }

    if (showFeedSourcesSheet) {
        val sheetState = rememberExpandedSheetState()
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
                    val toggleableSources = vm.toggleableSources
                    ChoiceGrid(count = toggleableSources.size) { index, corners, modifier ->
                        val src = toggleableSources[index]
                        ChoiceGridTile(
                            title = BooruRepository.getSourceDisplayName(src),
                            supporting = null,
                            selected = vm.isSourceEnabled(src),
                            corners = corners,
                            onClick = { vm.setSourceEnabled(src, !vm.isSourceEnabled(src)) },
                            modifier = modifier
                        ) { selected ->
                            Icon(
                                imageVector = feedSourceIcon(src),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRule34Dialog) {
        val keyboardSheet = rememberKeyboardSheetState()
        val sheetState = keyboardSheet.state
        LaunchedEffect(Unit) {
            rule34User = vm.rule34UserId
            rule34Key = vm.rule34ApiKey
        }
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showRule34Dialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
            ) {
                KeyboardSheetEffects(keyboardSheet, onDismiss = { showRule34Dialog = false })
                ApiKeyForm(
                    title = "Rule34 API",
                    description = Strings.rule34DialogDesc(lang),
                    user = rule34User,
                    onUserChange = { rule34User = it },
                    key = rule34Key,
                    onKeyChange = { rule34Key = it },
                    lang = lang,
                    hasSavedKeys = vm.rule34ApiKey.isNotBlank() || vm.rule34UserId.isNotBlank(),
                    onGetKey = { context.openUrlSafely("https://rule34.xxx/index.php?page=account&s=options", lang, showMessage) },
                    onSave = {
                        vm.saveRule34Keys(rule34User.trim(), rule34Key.trim())
                        scope.launch { keyboardSheet.hide() }.invokeOnCompletion { showRule34Dialog = false }
                    },
                    onRemove = {
                        rule34User = ""
                        rule34Key = ""
                        vm.saveRule34Keys("", "")
                        scope.launch { keyboardSheet.hide() }.invokeOnCompletion { showRule34Dialog = false }
                    }
                )
            }
        }
    }

    if (showGelbooruDialog) {
        val keyboardSheet = rememberKeyboardSheetState()
        val sheetState = keyboardSheet.state
        LaunchedEffect(Unit) {
            gelbooruUser = vm.gelbooruUserId
            gelbooruKey = vm.gelbooruApiKey
        }
        SheetMotion {
            ModalBottomSheet(
                onDismissRequest = { showGelbooruDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ShapeTokens.ExtraLargeTop
            ) {
                KeyboardSheetEffects(keyboardSheet, onDismiss = { showGelbooruDialog = false })
                ApiKeyForm(
                    title = "Gelbooru API",
                    description = Strings.gelbooruDialogDesc(lang),
                    user = gelbooruUser,
                    onUserChange = { gelbooruUser = it },
                    key = gelbooruKey,
                    onKeyChange = { gelbooruKey = it },
                    lang = lang,
                    hasSavedKeys = vm.gelbooruApiKey.isNotBlank() || vm.gelbooruUserId.isNotBlank(),
                    onGetKey = { context.openUrlSafely("https://gelbooru.com/index.php?page=account&s=options", lang, showMessage) },
                    onSave = {
                        vm.saveGelbooruKeys(gelbooruUser.trim(), gelbooruKey.trim())
                        scope.launch { keyboardSheet.hide() }.invokeOnCompletion { showGelbooruDialog = false }
                    },
                    onRemove = {
                        gelbooruUser = ""
                        gelbooruKey = ""
                        vm.saveGelbooruKeys("", "")
                        scope.launch { keyboardSheet.hide() }.invokeOnCompletion { showGelbooruDialog = false }
                    }
                )
            }
        }
    }

    if (showQualityDialog) {
        val sheetState = rememberExpandedSheetState()
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
        val keyboardSheet = rememberKeyboardSheetState()
        val sheetState = keyboardSheet.state
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
                KeyboardSheetEffects(keyboardSheet, onDismiss = {
                    showAddCustomSourceDialog = false
                    editingCustomSource = null
                })
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
                    SheetHeader(
                        icon = if (isEditing) Icons.Rounded.Edit else Icons.Rounded.AddLink,
                        title = if (isEditing) Strings.editSourceTitle(lang) else Strings.addSourceTitle(lang),
                        subtitle = Strings.tr(lang, "HTTPS only", "Только HTTPS", "HTTPSのみ", "仅限 HTTPS", "HTTPS만", "HTTPS فقط")
                    )

                    Column {
                        GroupedField(
                            value = customName,
                            onValueChange = { customName = it; customNameError = null },
                            label = Strings.sourceNameHint(lang),
                            icon = Icons.Rounded.Badge,
                            shape = segmentedListShape(0, 2),
                            error = customNameError
                        )
                        GroupedField(
                            value = customUrl,
                            onValueChange = { customUrl = it; customUrlError = null },
                            label = Strings.sourceUrlHint(lang),
                            icon = Icons.Rounded.Link,
                            shape = segmentedListShape(1, 2),
                            placeholder = "https://example.booru.org",
                            error = customUrlError,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = Strings.sourceEngineLabel(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
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
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = Strings.tr(lang, "Access (optional)", "Доступ (необязательно)", "アクセス（任意）", "访问（可选）", "접근 (선택)", "الوصول (اختياري)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Column {
                            GroupedField(
                                value = customUserId,
                                onValueChange = { customUserId = it; customAccessError = null },
                                label = Strings.tr(lang, "User ID / Login", "ID / Логин", "ユーザーID / ログイン", "用户 ID / 登录名", "사용자 ID / 로그인", "المعرّف / اسم الدخول"),
                                icon = Icons.Rounded.Person,
                                shape = segmentedListShape(0, 2)
                            )
                            GroupedField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it; customAccessError = null },
                                label = Strings.tr(lang, "API key", "API-ключ", "APIキー", "API 密钥", "API 키", "مفتاح API"),
                                icon = Icons.Rounded.Key,
                                shape = segmentedListShape(1, 2),
                                secret = true,
                                error = customAccessError
                            )
                        }
                    }

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
                            customAccessError = if (customUserId.isBlank() != customApiKey.isBlank()) {
                                Strings.tr(lang, "Fill in both login and key, or leave both empty", "Укажите и логин, и ключ, или оставьте оба пустыми", "ログインとキーの両方を入力するか、両方を空にしてください", "请同时填写登录名和密钥，或都留空", "로그인과 키를 모두 입력하거나 둘 다 비워 두세요", "املأ اسم الدخول والمفتاح معًا أو اتركهما فارغين")
                            } else null
                            if (customNameError != null || customUrlError != null || customAccessError != null) return@Button
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
                                customName = ""
                                customUrl = ""
                                customApiKey = ""
                                customUserId = ""
                                editingCustomSource = null
                                scope.launch { keyboardSheet.hide() }.invokeOnCompletion { showAddCustomSourceDialog = false }
                            }
                        },
                        shapes = ButtonDefaults.shapes(shape = CircleShape, pressedShape = ButtonDefaults.pressedShape),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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

        val keyboardSheet = rememberKeyboardSheetState()
        val sheetState = keyboardSheet.state

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
                KeyboardSheetEffects(keyboardSheet, onDismiss = {
                    showBlacklistDialog = false
                    blacklistFilterQuery = ""
                })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetHeader(
                        icon = Icons.Rounded.Block,
                        title = Strings.tagBlacklistTitle(lang),
                        subtitle = Strings.tagBlacklistDesc(lang),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 18.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (newBlacklistTag.isEmpty()) {
                                        Text(
                                            text = Strings.addTagPlaceholder(lang),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = newBlacklistTag,
                                        onValueChange = { newBlacklistTag = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { addTagAction() }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                AnimatedVisibility(
                                    visible = newBlacklistTag.isNotEmpty(),
                                    enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialDefault()),
                                    exit = fadeOut(Motion.effectsFast()) + scaleOut(Motion.effectsFast())
                                ) {
                                    IconButton(onClick = { newBlacklistTag = "" }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        val isAddActive = newBlacklistTag.isNotBlank()
                        val addLead by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (isAddActive) 28.dp else 6.dp,
                            animationSpec = Motion.spatialDefault(),
                            label = "addLead"
                        )
                        FilledIconButton(
                            onClick = addTagAction,
                            enabled = isAddActive,
                            shape = RoundedCornerShape(topStart = addLead, bottomStart = addLead, topEnd = 28.dp, bottomEnd = 28.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = Strings.addTagBtn(lang), modifier = Modifier.size(24.dp))
                        }
                    }

                    if (vm.tagBlacklist.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 16.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = if (blacklistFilterQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (blacklistFilterQuery.isEmpty()) {
                                            Text(
                                                text = Strings.searchBlacklistPlaceholder(lang),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = blacklistFilterQuery,
                                            onValueChange = { blacklistFilterQuery = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (blacklistFilterQuery.isNotEmpty()) {
                                        IconButton(onClick = { blacklistFilterQuery = "" }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            AnimatedConfirmDeleteButton(
                                onConfirmed = {
                                    vm.clearBlacklist()
                                    blacklistFilterQuery = ""
                                },
                                lang = lang,
                                initialIcon = Icons.Rounded.DeleteSweep,
                                initialText = Strings.tr(lang, "Clear all", "Очистить", "すべて消去", "全部清除", "모두 지우기", "مسح الكل"),
                                confirmText = Strings.confirmDeleteAction(lang),
                                height = 48.dp,
                                contentPadding = 16.dp,
                                idleContainerColor = MaterialTheme.colorScheme.errorContainer,
                                idleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                            )
                        }
                    }

                    Surface(
                        shape = ShapeTokens.LargeIncreased,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (vm.tagBlacklist.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingIconBadge(
                                    icon = Icons.Rounded.Shield,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = Strings.noBlacklistedTags(lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Strings.emptyBlacklistHint(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                                ) {
                                    Text(
                                        text = Strings.tr(lang, "Hidden tags", "Скрытые теги", "非表示タグ", "隐藏的标签", "숨긴 태그", "الوسوم المخفية"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Text(
                                            text = vm.tagBlacklist.size.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp, max = 220.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    if (filteredBlacklist.isEmpty()) {
                                        Text(
                                            text = Strings.tr(lang, "No matching tags", "Ничего не найдено", "一致するタグはありません", "没有匹配的标签", "일치하는 태그 없음", "لا توجد وسوم مطابقة"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(vertical = 12.dp)
                                        )
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
                    }

                    Button(
                        onClick = {
                            scope.launch { keyboardSheet.hide() }.invokeOnCompletion {
                                showBlacklistDialog = false
                                blacklistFilterQuery = ""
                            }
                        },
                        shapes = ButtonDefaults.shapes(shape = CircleShape, pressedShape = ButtonDefaults.pressedShape),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = Strings.tr(lang, "Done", "Готово", "完了", "完成", "완료", "تم"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showPaletteDialog) {
        val isDark = LocalIsDarkTheme.current
        val sheetState = rememberExpandedSheetState()
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
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                    ) {
                        SettingIconBadge(
                            icon = Icons.Rounded.Palette,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.colorPaletteTitle(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = vm.palette.localizedTitle(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = ShapeTokens.ExtraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val columns = 4
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPalette.entries.chunked(columns).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { pal ->
                                        PaletteTile(
                                            palette = pal,
                                            lang = lang,
                                            isDark = isDark,
                                            selected = vm.palette == pal,
                                            onClick = { vm.updatePalette(pal) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showPaletteDialog = false }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = Strings.tr(lang, "Done", "Готово", "完了", "完成", "완료", "تم"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
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
                    SettingsCategory.APPEARANCE -> Strings.tr(lang, "Style", "Вид", "スタイル", "样式", "스타일", "المظهر")
                    SettingsCategory.SOURCES -> Strings.tr(lang, "Sources", "Источники", "ソース", "来源", "소스", "المصادر")
                    SettingsCategory.CONTENT -> Strings.tr(lang, "Content", "Контент", "コンテンツ", "内容", "콘텐츠", "المحتوى")
                    SettingsCategory.SYSTEM -> Strings.tr(lang, "System", "Система", "システム", "系统", "시스템", "النظام")
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
                                    SettingIconBadge(
                                        icon = when (vm.themeMode) {
                                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                                    iconProvider = { mode ->
                                        when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        }
                                    },
                                    labelProvider = { mode ->
                                        when (mode) {
                                            ThemeMode.SYSTEM -> Strings.tr(lang, "Auto", "Авто", "自動", "自动", "자동", "تلقائي")
                                            ThemeMode.DARK -> Strings.tr(lang, "Dark", "Тёмная", "ダーク", "深色", "다크", "داكن")
                                            ThemeMode.LIGHT -> Strings.tr(lang, "Light", "Светлая", "ライト", "浅色", "라이트", "فاتح")
                                        }
                                    }
                                )
                            }

                            SettingsDivider()

                            SettingRowItem(
                                title = Strings.colorPaletteTitle(lang),
                                subtitle = vm.palette.localizedTitle(lang),
                                icon = Icons.Rounded.Palette,
                                onClick = { showPaletteDialog = true },
                                trailing = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ThemeSwatch(
                                            primary = MaterialTheme.colorScheme.primary,
                                            secondary = MaterialTheme.colorScheme.secondary,
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
                                subtitle = if (vm.rule34ApiKey.isNotBlank() && vm.rule34UserId.isNotBlank()) (Strings.tr(lang, "Connected · ID ${vm.rule34UserId}", "Подключено · ID ${vm.rule34UserId}", "接続済み · ID ${vm.rule34UserId}", "已连接 · ID ${vm.rule34UserId}", "연결됨 · ID ${vm.rule34UserId}", "متصل · ID ${vm.rule34UserId}")) else Strings.tapToEnterKeys(lang),
                                icon = Icons.Rounded.Key,
                                onClick = { showRule34Dialog = true }
                            )

                            SettingsDivider()

                            SettingRowItem(
                                title = "Gelbooru API",
                                subtitle = if (vm.gelbooruApiKey.isNotBlank() && vm.gelbooruUserId.isNotBlank()) (Strings.tr(lang, "Connected · ID ${vm.gelbooruUserId}", "Подключено · ID ${vm.gelbooruUserId}", "接続済み · ID ${vm.gelbooruUserId}", "已连接 · ID ${vm.gelbooruUserId}", "연결됨 · ID ${vm.gelbooruUserId}", "متصل · ID ${vm.gelbooruUserId}")) else Strings.tapToEnterKeys(lang),
                                icon = Icons.Rounded.VpnKey,
                                onClick = { showGelbooruDialog = true }
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        SectionLabel(Strings.customSourcesTitle(lang))

                        SettingsGroupCard {
                            SettingRowItem(
                                title = Strings.addSourceTitle(lang),
                                subtitle = if (vm.customSources.isEmpty()) Strings.noCustomSources(lang) else Strings.tr(lang, "${vm.customSources.size} custom sources", "Своих источников: ${vm.customSources.size}", "カスタムソース: ${vm.customSources.size}", "自定义来源：${vm.customSources.size}", "사용자 소스: ${vm.customSources.size}", "مصادر مخصصة: ${vm.customSources.size}"),
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
                                            }
                                        },
                                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier
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
                                            context.openUrlSafely("https://github.com/weekanya/Booru", lang, showMessage)
                                        },
                                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_github),
                                            contentDescription = "GitHub",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = Strings.tr(lang, "Source code", "Исходный код", "ソースコード", "源代码", "소스 코드", "الشيفرة المصدرية"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )

                            SettingsDivider()

                            SettingRowItem(
                                title = Strings.tr(lang, "Website", "Сайт", "ウェブサイト", "网站", "웹사이트", "الموقع"),
                                subtitle = "booru.weebio.ru",
                                icon = Icons.Rounded.Language,
                                trailing = {
                                    FilledTonalButton(
                                        onClick = {
                                            context.openUrlSafely("https://booru.weebio.ru", lang, showMessage)
                                        },
                                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = Strings.tr(lang, "Open", "Открыть", "開く", "打开", "열기", "فتح"),
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
                                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.Large, pressedShape = ButtonDefaults.pressedShape),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier
                                            .height(36.dp)
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
                                                LoadingIndicator(
                                                    modifier = Modifier.size(24.dp),
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
    ToggleGroupColors {
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
}

@Composable
private fun PaletteTile(
    palette: AppPalette,
    lang: AppLanguage,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = remember(palette, isDark) { paletteColorScheme(context, palette, isDark) }
    val corner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 20.dp else 32.dp,
        animationSpec = Motion.spatialDefault(),
        label = "paletteCorner"
    )
    val ringColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = Motion.effectsDefault(),
        label = "paletteRing"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.spatialFast(),
        label = "paletteCheck"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(ShapeTokens.Large)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .border(BorderStroke(2.dp, ringColor), RoundedCornerShape(corner))
                .padding(5.dp)
                .clip(RoundedCornerShape(corner - 3.dp))
                .background(scheme.surfaceContainerHighest)
        ) {
            ThemeSwatch(
                primary = scheme.primary,
                secondary = scheme.secondary,
                tertiary = scheme.tertiary,
                modifier = Modifier.size(40.dp)
            )
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = palette.localizedTitle(lang),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
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
    iconProvider: (T) -> ImageVector? = { null },
    labelProvider: (T) -> String
) {
    ToggleGroupColors {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selectedOption
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onOptionSelected(option) },
                    shapes = groupPosition(index, options.size).toggleShapes(),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .semantics { role = Role.RadioButton }
                ) {
                    val icon = iconProvider(option)
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = labelProvider(option),
                        style = MaterialTheme.typography.labelLarge,
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
    val sheetState = rememberExpandedSheetState()
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
    
                ChoiceGrid(count = AppLanguage.entries.size) { index, corners, modifier ->
                    val langOption = AppLanguage.entries[index]
                    ChoiceGridTile(
                        title = langOption.displayName,
                        supporting = langOption.englishName,
                        selected = langOption == currentLanguage,
                        corners = corners,
                        onClick = {
                            onLanguageSelected(langOption)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        modifier = modifier
                    ) { selected ->
                        Text(
                            text = langOption.code.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun gridCorners(index: Int, count: Int): BooleanArray {
    val rows = (count + 1) / 2
    val row = index / 2
    val col = index % 2
    val rowSize = minOf(2, count - row * 2)
    return booleanArrayOf(
        row == 0 && col == 0,
        row == 0 && col == rowSize - 1,
        row == rows - 1 && col == 0,
        row == rows - 1 && col == rowSize - 1
    )
}

@Composable
private fun ChoiceGrid(
    count: Int,
    content: @Composable (index: Int, corners: BooleanArray, modifier: Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        (0 until count).chunked(2).forEach { rowIndices ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                rowIndices.forEach { i -> content(i, gridCorners(i, count), Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChoiceGridTile(
    title: String,
    supporting: String?,
    selected: Boolean,
    corners: BooleanArray,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (selected: Boolean) -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = Motion.effectsDefault(),
        label = "gridTileColor"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = Motion.effectsDefault(),
        label = "gridTileBadge"
    )
    @Composable
    fun corner(outer: Boolean, label: String): androidx.compose.ui.unit.Dp {
        val v by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (selected) 28.dp else if (outer) 24.dp else 6.dp,
            animationSpec = Motion.spatialDefault(),
            label = label
        )
        return v
    }
    val shape = RoundedCornerShape(
        topStart = corner(corners[0], "gTS"),
        topEnd = corner(corners[1], "gTE"),
        bottomStart = corner(corners[2], "gBS"),
        bottomEnd = corner(corners[3], "gBE")
    )
    Surface(onClick = onClick, shape = shape, color = container, modifier = modifier.height(60.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 12.dp)) {
            Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { badge(selected) }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.5f),
                exit = fadeOut(Motion.effectsFast()) + scaleOut(Motion.effectsFast(), targetScale = 0.5f)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
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
        enter = fadeIn(Motion.effectsDefault()) + scaleIn(
            initialScale = 0.85f,
            animationSpec = Motion.effectsDefault()
        ),
        exit = fadeOut(Motion.effectsFast()) + scaleOut(
            targetScale = 0.8f,
            animationSpec = Motion.effectsDefault()
        ) + shrinkHorizontally(
            animationSpec = Motion.effectsDefault()
        )
    ) {
        Surface(
            shape = ShapeTokens.Small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .padding(start = 12.dp, end = 2.dp),
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
                        .size(32.dp)
                        .clip(CircleShape)
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
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun android.content.Context.openUrlSafely(url: String, lang: AppLanguage, showMessage: (String) -> Unit) {
    val opened = runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
    }.isSuccess
    if (!opened) showMessage(Strings.noBrowserFound(lang))
}

@Composable
private fun SheetHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        SettingIconBadge(icon = icon, containerColor = containerColor, contentColor = contentColor)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    shape: androidx.compose.ui.graphics.Shape,
    placeholder: String? = null,
    error: String? = null,
    secret: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    var reveal by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { p -> { Text(p) } },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = when {
            secret -> {
                {
                    IconButton(onClick = { reveal = !reveal }) {
                        Icon(
                            if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (reveal) "Hide" else "Show",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            value.isNotEmpty() -> {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            }
            else -> null
        },
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
        visualTransformation = if (secret && !reveal) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) androidx.compose.ui.text.input.KeyboardType.Password else keyboardType),
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    )
}

@Composable
private fun ApiKeyForm(
    title: String,
    description: String,
    user: String,
    onUserChange: (String) -> Unit,
    key: String,
    onKeyChange: (String) -> Unit,
    lang: AppLanguage,
    hasSavedKeys: Boolean,
    onGetKey: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit
) {
    val userTrim = user.trim()
    val keyTrim = key.trim()
    val bothEmpty = userTrim.isEmpty() && keyTrim.isEmpty()
    val userError = when {
        userTrim.isNotEmpty() && !userTrim.all { it.isDigit() } -> Strings.tr(lang, "Digits only", "Только цифры", "数字のみ", "仅限数字", "숫자만", "أرقام فقط")
        userTrim.isEmpty() && keyTrim.isNotEmpty() -> Strings.tr(lang, "Enter your User ID", "Введите User ID", "ユーザーIDを入力", "请输入用户 ID", "사용자 ID를 입력하세요", "أدخل معرّف المستخدم")
        else -> null
    }
    val keyError = when {
        keyTrim.isEmpty() && userTrim.isNotEmpty() -> Strings.tr(lang, "Enter your API key", "Введите API-ключ", "APIキーを入力", "请输入 API 密钥", "API 키를 입력하세요", "أدخل مفتاح API")
        keyTrim.isNotEmpty() && keyTrim.length < 16 -> Strings.tr(lang, "Key looks too short", "Ключ слишком короткий", "キーが短すぎます", "密钥太短", "키가 너무 짧습니다", "المفتاح قصير جدًا")
        keyTrim.any { it.isWhitespace() } -> Strings.tr(lang, "Key can't contain spaces", "Ключ без пробелов", "キーにスペースは使えません", "密钥不能包含空格", "키에 공백을 넣을 수 없습니다", "لا يمكن أن يحتوي المفتاح على مسافات")
        else -> null
    }
    val canSave = !bothEmpty && userError == null && keyError == null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetHeader(icon = Icons.Rounded.Key, title = title, subtitle = description)
        Column {
            GroupedField(
                value = user,
                onValueChange = { onUserChange(it.trim()) },
                label = Strings.tr(lang, "User ID", "ID пользователя", "ユーザーID", "用户 ID", "사용자 ID", "معرّف المستخدم"),
                icon = Icons.Rounded.Person,
                shape = segmentedListShape(0, 2),
                error = userError,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            GroupedField(
                value = key,
                onValueChange = { onKeyChange(it.trim()) },
                label = Strings.tr(lang, "API key", "API-ключ", "APIキー", "API 密钥", "API 키", "مفتاح API"),
                icon = Icons.Rounded.VpnKey,
                shape = segmentedListShape(1, 2),
                error = keyError,
                secret = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = onGetKey,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Strings.getKeyFromSite(lang), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = if (hasSavedKeys) 8.dp else 28.dp, bottomEnd = if (hasSavedKeys) 8.dp else 28.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Strings.saveBtn(lang), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            if (hasSavedKeys) {
                AnimatedConfirmDeleteButton(
                    onConfirmed = onRemove,
                    lang = lang,
                    initialIcon = Icons.Rounded.DeleteOutline,
                    confirmText = Strings.confirmDeleteAction(lang),
                    height = 56.dp,
                    contentPadding = 16.dp,
                    idleContainerColor = MaterialTheme.colorScheme.errorContainer,
                    idleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp)
                )
            }
        }
    }
}
