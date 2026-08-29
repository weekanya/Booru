package com.example.boorugallery.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.boorugallery.GalleryViewModel
import com.example.boorugallery.R
import com.example.boorugallery.data.AppLanguage
import com.example.boorugallery.data.Strings

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

    var danbooruLogin by remember(vm.danbooruLogin) { mutableStateOf(vm.danbooruLogin) }
    var danbooruKey by remember(vm.danbooruApiKey) { mutableStateOf(vm.danbooruApiKey) }

    var showRule34Dialog by remember { mutableStateOf(false) }
    var showGelbooruDialog by remember { mutableStateOf(false) }
    var showDanbooruDialog by remember { mutableStateOf(false) }

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

    if (showDanbooruDialog) {
        AlertDialog(
            onDismissRequest = { showDanbooruDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Rounded.Api,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Danbooru API Keys", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Strings.danbooruDialogDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = danbooruLogin,
                        onValueChange = { danbooruLogin = it },
                        label = { Text("Username / Login") },
                        placeholder = { Text("username") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = danbooruKey,
                        onValueChange = { danbooruKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val url = "https://danbooru.donmai.us/profile"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(Strings.getDanbooruKeyFromSite(lang))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.saveDanbooruKeys(danbooruLogin, danbooruKey)
                        showDanbooruDialog = false
                        Toast.makeText(context, Strings.danbooruKeysSavedToast(lang), Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape
                ) {
                    Text(Strings.saveBtn(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDanbooruDialog = false }) {
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
            .padding(top = 16.dp, bottom = 100.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(18.dp))
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = lang == AppLanguage.ENGLISH,
                        onClick = { vm.updateLanguage(AppLanguage.ENGLISH) },
                        label = { Text("EN", fontWeight = FontWeight.Bold) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = lang == AppLanguage.RUSSIAN,
                        onClick = { vm.updateLanguage(AppLanguage.RUSSIAN) },
                        label = { Text("RU", fontWeight = FontWeight.Bold) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
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

            SettingsDivider()

            SettingRowItem(
                title = "Danbooru API",
                subtitle = if (vm.danbooruApiKey.isNotBlank()) "Configured (${vm.danbooruLogin})" else Strings.tapToEnterDanbooruKeys(lang),
                icon = Icons.Rounded.Api,
                onClick = { showDanbooruDialog = true }
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
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(Strings.appearanceSection(lang))

        SettingsGroupCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                    Spacer(Modifier.width(18.dp))
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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = vm.themeMode == ThemeMode.SYSTEM,
                        onClick = { vm.updateThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.bouncyPress(),
                        label = { Text("Auto", style = MaterialTheme.typography.labelSmall) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = vm.themeMode == ThemeMode.DARK,
                        onClick = { vm.updateThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.bouncyPress(),
                        label = { Text("Dark", style = MaterialTheme.typography.labelSmall) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = vm.themeMode == ThemeMode.LIGHT,
                        onClick = { vm.updateThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.bouncyPress(),
                        label = { Text("Light", style = MaterialTheme.typography.labelSmall) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            SettingsDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            text = Strings.colorPaletteTitle(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Strings.colorPaletteDesc(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

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

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AppPalette.entries) { pal ->
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
                            onClick = { vm.updatePalette(pal) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null,
                            modifier = Modifier
                                .height(48.dp)
                                .bouncyPress()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(swatchBrush)
                                        .then(
                                            if (isSelected)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer, CircleShape)
                                            else Modifier
                                        )
                                )
                                Text(
                                    text = if (pal == AppPalette.MONET) "Monet (Wallpaper)" else pal.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/weekanya/Booru"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
        }
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
                tint = MaterialTheme.colorScheme.primary,
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

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
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
