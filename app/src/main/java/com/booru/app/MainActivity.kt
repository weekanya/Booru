package com.booru.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.booru.app.data.AppLanguage
import com.booru.app.ui.bouncyPress
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.booru.app.data.Strings
import com.booru.app.data.BooruCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.booru.app.ui.BooruTheme
import com.booru.app.ui.ExploreScreen
import com.booru.app.ui.FavoritesScreen
import com.booru.app.ui.FullscreenMediaViewer
import com.booru.app.ui.Motion
import com.booru.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        super.onCreate(savedInstanceState)
        setContent { BooruApp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            CoroutineScope(Dispatchers.IO).launch {
                BooruCacheManager.clearBrowsingCache(applicationContext)
            }
        }
    }
}

private data class NavItemData(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (selectedTab != 0) {
            selectedTab = 0
        } else if (vm.query.isNotBlank()) {
            vm.search(vm.source, "", vm.safeMode)
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, Strings.pressBackAgainToExit(lang), Toast.LENGTH_SHORT).show()
            }
        }
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
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                label = "navItemBg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
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
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconView = @Composable {
                                        Crossfade(
                                            targetState = isSelected,
                                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                            label = "navIconFade"
                                        ) { sel ->
                                            Icon(
                                                imageVector = if (sel) item.selectedIcon else item.icon,
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

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
                                            iconView()
                                        }
                                    } else {
                                        iconView()
                                    }

                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + expandHorizontally(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            expandFrom = Alignment.Start
                                        ),
                                        exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) + shrinkHorizontally(
                                            animationSpec = tween(200, easing = FastOutLinearInEasing),
                                            shrinkTowards = Alignment.Start
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

                val state = vm.fullscreenState
                if (state != null) {
                    FullscreenMediaViewer(
                        initialIndex = state.index,
                        mediaList = if (state.isFromResults) vm.results else state.list,
                        vm = vm,
                        onDismiss = { vm.closeFullscreen() },
                        onLoadMore = if (state.isFromResults) { { vm.loadMore() } } else null,
                        onNavigateToExplore = { selectedTab = 0 }
                    )
                }
            }
        }
    }

        vm.updateInfo?.let { info ->
            UpdateBottomSheet(
                info = info,
                vm = vm,
                lang = lang,
                onDismiss = { vm.dismissUpdate() }
            )
        }

        vm.manualCheckResult?.let { result ->
            val checkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { vm.clearManualCheckResult() },
                sheetState = checkSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp),
                        shape = CircleShape,
                        color = Color.White
                    ) {}
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (result == "UP_TO_DATE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (result == "UP_TO_DATE") Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (result == "UP_TO_DATE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (result == "UP_TO_DATE") Strings.upToDateTitle(lang) else Strings.updateCheckFailedTitle(lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (result == "UP_TO_DATE") Strings.upToDateDesc(lang) else Strings.updateCheckFailedDesc(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { vm.clearManualCheckResult() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBottomSheet(
    info: com.booru.app.data.AppUpdateInfo,
    vm: GalleryViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!vm.isDownloadingUpdate) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = Color.White
            ) {}
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = Strings.updateAvailableTitle(lang, info.latestVersion),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "v${info.latestVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Strings.updateAvailableDesc(lang, info.latestVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!vm.isDownloadingUpdate) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = Strings.closeBtn(lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (vm.isDownloadingUpdate) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.downloadingUpdate(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(vm.updateDownloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
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
                    }
                }
            } else if (vm.updateDownloadError != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (lang == AppLanguage.RUSSIAN) "Обновление скачано и готово к установке." else "Update is downloaded and ready to install.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (info.releaseNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 340.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        com.booru.app.ui.MarkdownText(markdown = info.releaseNotes)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (vm.isDownloadingUpdate) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
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
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bouncyPress()
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.installUpdate(lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            } else if (vm.updateDownloadError != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            vm.downloadAndInstallUpdate(context, info)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(Strings.updateButton(lang), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = {
                            val targetUrl = info.apkDownloadUrl ?: info.releaseUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            context.startActivity(intent)
                            vm.dismissUpdate()
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncyPress()
                    ) {
                        Text(Strings.openInBrowser(lang))
                    }
                }
            } else {
                Button(
                    onClick = {
                        vm.downloadAndInstallUpdate(context, info)
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bouncyPress()
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.updateButton(lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (!vm.isDownloadingUpdate) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            vm.ignoreUpdate(info.latestVersion)
                        },
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    ) {
                        Text(
                            Strings.dontRemindAgain(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.bouncyPress()
                    ) {
                        Text(
                            Strings.closeBtn(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
