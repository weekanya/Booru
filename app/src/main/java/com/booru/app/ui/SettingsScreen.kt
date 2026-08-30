package com.booru.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.booru.app.GalleryViewModel
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
    var newBlacklistTag by remember { mutableStateOf("") }

    if (showRule34Dialog) {
        AlertDialog(
            onDismissRequest = { showRule34Dialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Rule34.xxx API Keys", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Strings.rule34DialogDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rule34User,
                        onValueChange = { rule34User = it },
                        label = { Text("User ID") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rule34Key,
                        onValueChange = { rule34Key = it },
                        label = { Text("API Key") },
                        placeholder = { Text("API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val url = "https://rule34.xxx/index.php?page=account&s=options"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(Strings.getKeyFromSite(lang))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
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
                    shape = CircleShape
                ) {
                    Text(Strings.saveBtn(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRule34Dialog = false }) {
                    Text(Strings.cancelBtn(lang))
                }
            }
        )
    }

    if (showGelbooruDialog) {
        AlertDialog(
            onDismissRequest = { showGelbooruDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Gelbooru API Keys", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Strings.gelbooruDialogDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = gelbooruUser,
                        onValueChange = { gelbooruUser = it },
                        label = { Text("User ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gelbooruKey,
                        onValueChange = { gelbooruKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val url = "https://gelbooru.com/index.php?page=account&s=options"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(Strings.getKeyFromSite(lang))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
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
                    shape = CircleShape
                ) {
                    Text(Strings.saveBtn(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGelbooruDialog = false }) {
                    Text(Strings.cancelBtn(lang))
                }
            }
        )
    }

    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(Strings.tagBlacklistTitle(lang), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        Strings.tagBlacklistDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
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
                            placeholder = { Text(Strings.addTagPlaceholder(lang)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                if (newBlacklistTag.isNotBlank()) {
                                    vm.addBlacklistedTag(newBlacklistTag)
                                    newBlacklistTag = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(Strings.addTagBtn(lang), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (vm.tagBlacklist.isEmpty()) {
                        Text(
                            Strings.noBlacklistedTags(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vm.tagBlacklist) { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { vm.removeBlacklistedTag(tag) },
                                    label = { Text(tag) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBlacklistDialog = false },
                    shape = CircleShape
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (vm.tagBlacklist.isNotEmpty()) {
                    TextButton(
                        onClick = { vm.clearBlacklist() }
                    ) {
                        Text(Strings.clearAllBlacklist(lang), color = MaterialTheme.colorScheme.error)
                    }
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
                Color(0xFF7E5260)
            }
        }

        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(Strings.colorPaletteTitle(lang), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            shape = RoundedCornerShape(16.dp),
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
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(swatchBrush)
                                        .then(
                                            if (isSelected)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pal.title,
                                        style = MaterialTheme.typography.bodyLarge,
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
            confirmButton = {
                TextButton(
                    onClick = { showPaletteDialog = false },
                    shape = CircleShape
                ) {
                    Text(Strings.cancelBtn(lang))
                }
            }
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

        SectionLabel(Strings.contentSection(lang))

        SettingsGroupCard {
            SettingSwitchItem(
                title = Strings.safeModeTitle(lang),
                subtitle = Strings.safeModeDesc(lang),
                icon = Icons.Rounded.Shield,
                checked = vm.safeMode,
                onCheckedChange = { vm.setSafeModeEnabled(it) }
            )

            SettingsDivider()

            SettingSwitchItem(
                title = Strings.excludeSafeTitle(lang),
                subtitle = Strings.excludeSafeDesc(lang),
                icon = Icons.Rounded.Lock,
                checked = vm.excludeSafe,
                onCheckedChange = { vm.setExcludeSafeEnabled(it) },
                isDangerous = true
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
