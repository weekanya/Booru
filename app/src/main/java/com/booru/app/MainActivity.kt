package com.booru.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.booru.app.data.AppLanguage
import com.booru.app.ui.bouncyPress
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.booru.app.data.Strings
import com.booru.app.ui.BooruTheme
import com.booru.app.ui.ExploreScreen
import com.booru.app.ui.FavoritesScreen
import com.booru.app.ui.FullscreenMediaViewer
import com.booru.app.ui.Motion
import com.booru.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BooruApp() }
    }
}

private data class NavItemData(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun BooruApp(vm: GalleryViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val lang = vm.language

    val navItems = remember(vm.favoritesList.size, lang) {
        listOf(
            NavItemData(Strings.navExplore(lang), Icons.Rounded.Explore, Icons.Rounded.Explore),
            NavItemData(Strings.navFavorites(lang), Icons.Rounded.FavoriteBorder, Icons.Rounded.Favorite, badgeCount = vm.favoritesList.size),
            NavItemData(Strings.navSettings(lang), Icons.Rounded.Settings, Icons.Rounded.Settings)
        )
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            vm.refreshFeedIfNeeded()
        }
    }

    BooruTheme(
        themeMode = vm.themeMode,
        palette = vm.palette,
        useDynamicColor = vm.useDynamicColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "TabCrossfade",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        when (tab) {
                            0 -> ExploreScreen(
                                vm = vm,
                                onNavigateToSettings = { selectedTab = 2 }
                            )
                            1 -> FavoritesScreen(
                                vm = vm,
                                onNavigateToExplore = { selectedTab = 0 }
                            )
                            2 -> SettingsScreen(
                                vm = vm
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .height(64.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.02f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "navItemScale"
                            )
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                label = "navItemBg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                label = "navItemColor"
                            )

                            Surface(
                                shape = CircleShape,
                                color = containerColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .bouncyPress(scaleDown = 0.96f)
                                    .clickable {
                                        selectedTab = index
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp)
                                        .graphicsLayer {
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text("${item.badgeCount}")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                                            contentDescription = item.label,
                                            tint = contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandHorizontally(
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                                        ),
                                        exit = fadeOut(tween(120)) + shrinkHorizontally(
                                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                                        )
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                vm.fullscreenState?.let { state ->
                    FullscreenMediaViewer(
                        initialIndex = state.index,
                        mediaList = if (state.list === vm.results) vm.results else state.list,
                        vm = vm,
                        onDismiss = { vm.closeFullscreen() },
                        onLoadMore = {
                            if (state.list === vm.results) {
                                vm.loadMore()
                            }
                        },
                        onNavigateToExplore = { selectedTab = 0 }
                    )
                }
            }
        }
    }

        vm.updateInfo?.let { info ->
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = {
                    if (!vm.isDownloadingUpdate) {
                        vm.dismissUpdate()
                    }
                },
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = Strings.updateAvailableTitle(lang, info.latestVersion),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (vm.isDownloadingUpdate) {
                            Text(
                                text = Strings.downloadingUpdate(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { vm.updateDownloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = vm.updateDownloadProgressText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (vm.updateDownloadError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Strings.updateDownloadFailed(lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = vm.updateDownloadError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else if (vm.downloadedApkFile != null) {
                            Text(
                                text = if (lang == AppLanguage.RUSSIAN) "Обновление скачано и готово к установке." else "Update is downloaded and ready to install.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = Strings.updateAvailableDesc(lang, info.latestVersion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = info.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (vm.isDownloadingUpdate) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bouncyPress()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.downloadingUpdate(lang))
                        }
                    } else if (vm.downloadedApkFile != null) {
                        Button(
                            onClick = {
                                vm.installApk(context, vm.downloadedApkFile!!)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bouncyPress()
                        ) {
                            Text(Strings.installUpdate(lang), fontWeight = FontWeight.Bold)
                        }
                    } else if (vm.updateDownloadError != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val targetUrl = info.apkDownloadUrl ?: info.releaseUrl
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                    vm.dismissUpdate()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.bouncyPress()
                            ) {
                                Text(Strings.openInBrowser(lang))
                            }
                            Button(
                                onClick = {
                                    vm.downloadAndInstallUpdate(context, info)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.bouncyPress()
                            ) {
                                Text(Strings.updateButton(lang), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                vm.downloadAndInstallUpdate(context, info)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.bouncyPress()
                        ) {
                            Text(Strings.updateButton(lang), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!vm.isDownloadingUpdate) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    vm.ignoreUpdate(info.latestVersion)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.bouncyPress()
                            ) {
                                Text(
                                    Strings.dontRemindAgain(lang),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(
                                onClick = { vm.dismissUpdate() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.bouncyPress()
                            ) {
                                Text(Strings.closeBtn(lang))
                            }
                        }
                    }
                }
            )
        }

        vm.manualCheckResult?.let { result ->
            AlertDialog(
                onDismissRequest = { vm.clearManualCheckResult() },
                icon = {
                    if (result == "UP_TO_DATE") {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (result == "UP_TO_DATE") Strings.upToDateTitle(lang) else Strings.updateCheckFailedTitle(lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (result == "UP_TO_DATE") Strings.upToDateDesc(lang) else Strings.updateCheckFailedDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(onClick = { vm.clearManualCheckResult() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
