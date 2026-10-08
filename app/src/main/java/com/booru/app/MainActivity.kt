@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app

import android.app.Activity
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.SizeTransform
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import android.content.Intent
import android.content.res.Configuration
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
import android.view.WindowManager
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.booru.app.data.AppLanguage
import com.booru.app.ui.pressMorphShape
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
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
import com.booru.app.ui.SheetMotion
import com.booru.app.ui.rememberExpandedSheetState
import com.booru.app.ui.ShapeTokens
import com.booru.app.ui.LocalWindowWidthClass
import com.booru.app.ui.WindowWidthClass
import com.booru.app.ui.LocalShowMessage
import com.booru.app.ui.LocalMessageRouter
import com.booru.app.ui.MessageRouter
import com.booru.app.ui.FlatSnackbarHost
import com.booru.app.ui.SheetSnackbarHost

class MainActivity : ComponentActivity() {
    private val isAppLocked = mutableStateOf(false)
    private var activeSignal: CancellationSignal? = null
    private val vm by lazy { ViewModelProvider(this)[GalleryViewModel::class.java] }

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
        isAppLocked.value = vm.biometricLockEnabled && !vm.hasUnlockedSession
        setContent {
            BooruApp(
                vm = vm,
                isAppLocked = isAppLocked.value,
                onUnlockRequest = { promptBiometric() },
                onLockNeeded = {
                    if (!vm.hasUnlockedSession) {
                        isAppLocked.value = true
                        promptBiometric()
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (vm.biometricLockEnabled) {
            val timeoutMs = vm.biometricLockTimeoutMin * 60 * 1000L
            val elapsed = SystemClock.elapsedRealtime() - vm.lastBackgroundAt
            if (!vm.hasUnlockedSession || (vm.lastBackgroundAt != 0L && elapsed >= timeoutMs)) {
                vm.markSessionLocked()
                isAppLocked.value = true
                promptBiometric()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            vm.markBackgrounded(SystemClock.elapsedRealtime())
        }
    }

    private fun canUseDeviceAuth(): Boolean {
        val manager = getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun unlock() {
        activeSignal = null
        isAppLocked.value = false
        vm.markSessionUnlocked(SystemClock.elapsedRealtime())
    }

    private fun promptBiometric() {
        if (!vm.biometricLockEnabled) {
            unlock()
            return
        }
        if (!canUseDeviceAuth()) {
            vm.setBiometricLock(false)
            unlock()
            vm.postMessage(Strings.biometricUnavailable(vm.language))
            return
        }
        if (activeSignal != null) return
        try {
            val prompt = BiometricPrompt.Builder(this)
                .setTitle(Strings.biometricLockTitle(vm.language))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            val signal = CancellationSignal()
            activeSignal = signal
            prompt.authenticate(
                signal,
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        unlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (activeSignal === signal) activeSignal = null
                        when (errorCode) {
                            BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                            BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
                            BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL,
                            BiometricPrompt.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                                if (!canUseDeviceAuth()) {
                                    vm.setBiometricLock(false)
                                    unlock()
                                }
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            activeSignal = null
            unlock()
        }
    }
}

private data class NavItemData(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
private fun BooruNavigationRail(
    items: List<NavItemData>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(index) },
                icon = {
                    val icon = if (selected) item.selectedIcon else item.icon
                    if (item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(if (item.badgeCount > 99) "99+" else "${item.badgeCount}")
                                }
                            }
                        ) {
                            Icon(icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooruApp(
    vm: GalleryViewModel = viewModel(),
    isAppLocked: Boolean = false,
    onUnlockRequest: () -> Unit = {},
    onLockNeeded: () -> Unit = {}
) {
    val lang = vm.language
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(vm.biometricLockEnabled) {
        if (vm.biometricLockEnabled) {
            onLockNeeded()
        }
    }

    if (isAppLocked) {
        val context = LocalContext.current
        BackHandler(enabled = true) {
            (context as? Activity)?.finish()
        }
        BooruTheme(
            themeMode = vm.themeMode,
            palette = vm.palette,
            useDynamicColor = vm.useDynamicColor
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = Strings.appLocked(lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = Strings.biometricLockSubtitle(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onUnlockRequest,
                        shapes = ButtonDefaults.shapes(shape = ShapeTokens.LargeIncreased, pressedShape = ButtonDefaults.pressedShape),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.LockOpen, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.unlockApp(lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val navItems = remember(vm.favoritesList.size, lang) {
        listOf(
            NavItemData(Strings.navExplore(lang), Icons.Rounded.Explore, Icons.Rounded.Explore),
            NavItemData(Strings.navFavorites(lang), Icons.Rounded.FavoriteBorder, Icons.Rounded.Favorite, badgeCount = vm.favoritesList.size),
            NavItemData(Strings.navSettings(lang), Icons.Rounded.Settings, Icons.Rounded.Settings)
        )
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSnackbarHostState = remember { SnackbarHostState() }
    val messageScope = rememberCoroutineScope()
    val messageRouter = remember(appSnackbarHostState, messageScope) {
        MessageRouter { message ->
            messageScope.launch {
                appSnackbarHostState.currentSnackbarData?.dismiss()
                appSnackbarHostState.showSnackbar(message)
            }
        }
    }
    val showMessage: (String) -> Unit = remember(messageRouter) { { message -> messageRouter.show(message) } }
    LaunchedEffect(messageRouter) {
        vm.messages.collect { messageRouter.show(it) }
    }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = vm.fullscreenState == null) {
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
                showMessage(Strings.pressBackAgainToExit(lang))
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            vm.refreshFeedIfNeeded()
        }
    }

    val layoutDirection = if (lang == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection, LocalShowMessage provides showMessage, LocalMessageRouter provides messageRouter) {
        BooruTheme(
            themeMode = vm.themeMode,
            palette = vm.palette,
            useDynamicColor = vm.useDynamicColor
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 760.dp
                    val widthClass = WindowWidthClass.fromWidth(maxWidth)
                    val onNavSelect: (Int) -> Unit = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        if (selectedTab == index) {
                            if (index == 0) vm.scrollToTop()
                        } else {
                            selectedTab = index
                        }
                    }
                    val state = vm.fullscreenState

                    val mainContent = @Composable {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .displayCutoutPadding()
                                .statusBarsPadding()
                        ) {
                            CompositionLocalProvider(LocalWindowWidthClass provides widthClass) {
                            Row(modifier = Modifier.fillMaxSize()) {
                            if (widthClass.usesNavigationRail) {
                                BooruNavigationRail(
                                    items = navItems,
                                    selectedIndex = selectedTab,
                                    onSelect = onNavSelect
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    (fadeIn(tween(Motion.FADE_THROUGH_IN_MS, delayMillis = Motion.FADE_THROUGH_OUT_MS, easing = Motion.EmphasizedDecelerate)) +
                                        scaleIn(tween(Motion.FADE_THROUGH_IN_MS, delayMillis = Motion.FADE_THROUGH_OUT_MS, easing = Motion.EmphasizedDecelerate), initialScale = 0.96f))
                                        .togetherWith(fadeOut(tween(Motion.FADE_THROUGH_OUT_MS, easing = Motion.EmphasizedAccelerate)))
                                },
                                label = "TabFadeThrough",
                                modifier = Modifier.fillMaxSize()
                            ) { tab ->
                                tabStateHolder.SaveableStateProvider(tab) {
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
                            }

                            }
                            }
                            }

                            if (!widthClass.usesNavigationRail) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                        .height(64.dp)
                                        .widthIn(max = 480.dp)
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
                                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f),
                                                animationSpec = Motion.effectsDefault(),
                                                label = "navItemBg"
                                            )
                                            val contentColor by animateColorAsState(
                                                targetValue = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                animationSpec = Motion.effectsDefault(),
                                                label = "navItemColor"
                                            )

                                            val navPress = remember { MutableInteractionSource() }
                                            val navShape = pressMorphShape(navPress, pressedFraction = 0.3f)
                                            Surface(
                                                shape = navShape,
                                                color = containerColor,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(navShape)
                                                    .selectable(
                                                        selected = isSelected,
                                                        interactionSource = navPress,
                                                        indication = ripple(),
                                                        role = Role.Tab,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                            if (selectedTab == index) {
                                                                if (index == 0) {
                                                                    vm.scrollToTop()
                                                                }
                                                            } else {
                                                                selectedTab = index
                                                            }
                                                        }
                                                    )
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
                                                            animationSpec = Motion.effectsDefault(),
                                                            label = "navIconFade"
                                                        ) { sel ->
                                                            Icon(
                                                                imageVector = if (sel) item.selectedIcon else item.icon,
                                                                contentDescription = if (isSelected) null else item.label,
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
                                                                    Text(if (item.badgeCount > 99) "99+" else "${item.badgeCount}")
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
                                                        enter = fadeIn(animationSpec = Motion.effectsDefault()) + expandHorizontally(
                                                            animationSpec = Motion.spatialFast(),
                                                            expandFrom = Alignment.Start
                                                        ),
                                                        exit = fadeOut(animationSpec = Motion.effectsFast()) + shrinkHorizontally(
                                                            animationSpec = Motion.spatialFast(),
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
                            }
                            }
                    }

                    if (isWideScreen) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(if (state != null) 0.42f else 1f)
                                    .fillMaxHeight()
                            ) {
                                mainContent()
                            }
                            if (state != null) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .fillMaxHeight()
                                ) {
                                    key(state.openId) {
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
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            mainContent()
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
                
                    FlatSnackbarHost(
                        hostState = appSnackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = if (widthClass.usesNavigationRail) 16.dp else 88.dp)
                    )
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
            val checkSheetState = rememberExpandedSheetState()
            val scope = rememberCoroutineScope()
            val upToDate = result == "UP_TO_DATE"
            val scheme = MaterialTheme.colorScheme
            val checkContext = LocalContext.current
            val currentVersion = remember {
                runCatching { checkContext.packageManager.getPackageInfo(checkContext.packageName, 0).versionName }.getOrNull()
            }
            SheetMotion {
                ModalBottomSheet(
                    onDismissRequest = { vm.clearManualCheckResult() },
                    sheetState = checkSheetState,
                    containerColor = scheme.surfaceContainerLow,
                    shape = ShapeTokens.ExtraLargeTop
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ExpressiveBadge(
                            polygon = if (upToDate) MaterialShapes.Cookie9Sided else MaterialShapes.Clover4Leaf,
                            icon = if (upToDate) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                            containerColor = if (upToDate) scheme.primaryContainer else scheme.errorContainer,
                            contentColor = if (upToDate) scheme.onPrimaryContainer else scheme.onErrorContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (upToDate) Strings.upToDateTitle(lang) else Strings.updateCheckFailedTitle(lang),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (upToDate) Strings.upToDateDesc(lang) else Strings.updateCheckFailedDesc(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (upToDate && currentVersion != null) {
                            Spacer(Modifier.height(16.dp))
                            Surface(shape = CircleShape, color = scheme.surfaceContainerHigh) {
                                Text(
                                    text = versionLabel(currentVersion),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                scope.launch { checkSheetState.hide() }.invokeOnCompletion { vm.clearManualCheckResult() }
                            },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = ButtonDefaults.MediumContentPadding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ButtonDefaults.MediumContainerHeight)
                        ) {
                            Text("OK", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
}

private enum class UpdateStage { Available, Downloading, Ready, Failed }

private fun versionLabel(version: String): String =
    if (version.startsWith("v", ignoreCase = true)) version else "v$version"

@Composable
private fun ExpressiveBadge(
    polygon: androidx.graphics.shapes.RoundedPolygon,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    var from by remember { mutableStateOf(polygon) }
    var to by remember { mutableStateOf(polygon) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(polygon) {
        if (polygon !== to) {
            from = to
            to = polygon
            progress.snapTo(0f)
            progress.animateTo(1f, Motion.spatialDefault())
        }
    }
    val morph = remember(from, to) { androidx.graphics.shapes.Morph(from, to) }
    val container by animateColorAsState(containerColor, Motion.effectsDefault(), label = "badgeContainer")
    val content by animateColorAsState(contentColor, Motion.effectsDefault(), label = "badgeContent")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(container, com.booru.app.ui.MorphShape(morph, progress.value.coerceIn(0f, 1f)))
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.6f))
                    .togetherWith(fadeOut(Motion.effectsFast()) + scaleOut(Motion.effectsFast(), targetScale = 0.6f))
            },
            label = "badgeIcon"
        ) { target ->
            Icon(target, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.4f))
        }
    }
}

@Composable
private fun VersionTransition(current: String?, latest: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (current != null) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(
                    text = versionLabel(current),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = versionLabel(latest),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun UpdateStatusCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateProgressCard(vm: GalleryViewModel, lang: AppLanguage) {
    val progress by animateFloatAsState(
        targetValue = vm.updateDownloadProgress.coerceIn(0f, 1f),
        animationSpec = Motion.effectsDefault(),
        label = "updateProgress"
    )
    val sizeText = vm.updateDownloadProgressText.substringAfter("(", "").substringBefore(")")
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.downloadingUpdate(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (sizeText.isNotBlank()) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${(vm.updateDownloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

@Composable
private fun ReleaseNotesCard(notes: String, lang: AppLanguage) {
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.whatsNewTitle(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                com.booru.app.ui.MarkdownText(markdown = notes)
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
    val isDownloading by rememberUpdatedState(vm.isDownloadingUpdate)
    val sheetState = rememberExpandedSheetState(
        confirmValueChange = { it != SheetValue.Hidden || !isDownloading }
    )
    val scope = rememberCoroutineScope()
    val showMessage = LocalShowMessage.current
    val currentVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    val stage = when {
        vm.isDownloadingUpdate -> UpdateStage.Downloading
        vm.downloadedApkFile != null -> UpdateStage.Ready
        vm.updateDownloadError != null -> UpdateStage.Failed
        else -> UpdateStage.Available
    }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    val scheme = MaterialTheme.colorScheme

    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = {
                if (!vm.isDownloadingUpdate) {
                    onDismiss()
                }
            },
            sheetState = sheetState,
            containerColor = scheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeTop
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (stage) {
                        UpdateStage.Available -> ExpressiveBadge(MaterialShapes.Cookie9Sided, Icons.Rounded.SystemUpdate, scheme.primaryContainer, scheme.onPrimaryContainer)
                        UpdateStage.Downloading -> ExpressiveBadge(MaterialShapes.SoftBurst, Icons.Rounded.Download, scheme.secondaryContainer, scheme.onSecondaryContainer)
                        UpdateStage.Ready -> ExpressiveBadge(MaterialShapes.Sunny, Icons.Rounded.DownloadDone, scheme.tertiaryContainer, scheme.onTertiaryContainer)
                        UpdateStage.Failed -> ExpressiveBadge(MaterialShapes.Clover4Leaf, Icons.Rounded.ErrorOutline, scheme.errorContainer, scheme.onErrorContainer)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = Strings.updateHeadline(lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    VersionTransition(current = currentVersion, latest = info.latestVersion)
                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = {
                            (fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialFast(), initialScale = 0.96f))
                                .togetherWith(fadeOut(Motion.effectsFast()))
                                .using(SizeTransform(clip = false) { _, _ -> Motion.spatialDefault() })
                        },
                        label = "updateStatus"
                    ) { target ->
                        when (target) {
                            UpdateStage.Downloading -> UpdateProgressCard(vm, lang)
                            UpdateStage.Ready -> UpdateStatusCard(
                                icon = Icons.Rounded.DownloadDone,
                                title = Strings.updateReadyToInstall(lang),
                                subtitle = null,
                                containerColor = scheme.tertiaryContainer,
                                contentColor = scheme.onTertiaryContainer
                            )
                            UpdateStage.Failed -> UpdateStatusCard(
                                icon = Icons.Rounded.ErrorOutline,
                                title = Strings.updateDownloadFailed(lang),
                                subtitle = vm.updateDownloadError,
                                containerColor = scheme.errorContainer,
                                contentColor = scheme.onErrorContainer
                            )
                            UpdateStage.Available -> Spacer(Modifier.fillMaxWidth())
                        }
                    }

                    if (info.releaseNotes.isNotBlank()) {
                        ReleaseNotesCard(info.releaseNotes, lang)
                    }

                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = {
                            fadeIn(Motion.effectsDefault())
                                .togetherWith(fadeOut(Motion.effectsFast()))
                                .using(SizeTransform(clip = false) { _, _ -> Motion.spatialDefault() })
                        },
                        label = "updateActions"
                    ) { target ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (target) {
                                UpdateStage.Available -> UpdatePrimaryButton(
                                    text = Strings.updateButton(lang),
                                    icon = Icons.Rounded.SystemUpdate,
                                    onClick = { vm.downloadAndInstallUpdate(context, info) }
                                )
                                UpdateStage.Downloading -> OutlinedButton(
                                    onClick = {
                                        vm.cancelUpdateDownload()
                                        close()
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                    border = BorderStroke(1.dp, scheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                                    contentPadding = ButtonDefaults.MediumContentPadding,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = ButtonDefaults.MediumContainerHeight)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(ButtonDefaults.MediumIconSize))
                                    Spacer(Modifier.width(ButtonDefaults.MediumIconSpacing))
                                    Text(Strings.cancelBtn(lang), style = MaterialTheme.typography.titleMedium)
                                }
                                UpdateStage.Ready -> {
                                    UpdatePrimaryButton(
                                        text = Strings.installUpdate(lang),
                                        icon = Icons.Rounded.InstallMobile,
                                        onClick = { vm.downloadedApkFile?.let { vm.installApk(context, it) } }
                                    )
                                    TextButton(
                                        onClick = {
                                            vm.deleteDownloadedApk()
                                            vm.downloadAndInstallUpdate(context, info)
                                        },
                                        shapes = ButtonDefaults.shapes()
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(Strings.downloadAgain(lang), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                UpdateStage.Failed -> {
                                    UpdatePrimaryButton(
                                        text = Strings.updateButton(lang),
                                        icon = Icons.Rounded.Refresh,
                                        onClick = { vm.downloadAndInstallUpdate(context, info) }
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val targetUrl = info.apkDownloadUrl ?: info.releaseUrl
                                            val opened = runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            }.isSuccess
                                            if (opened) {
                                                scope.launch { sheetState.hide() }.invokeOnCompletion { vm.dismissUpdate() }
                                            } else {
                                                showMessage(Strings.noBrowserFound(lang))
                                            }
                                        },
                                        shapes = ButtonDefaults.shapes(),
                                        contentPadding = ButtonDefaults.MediumContentPadding,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = ButtonDefaults.MediumContainerHeight)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(ButtonDefaults.MediumIconSize))
                                        Spacer(Modifier.width(ButtonDefaults.MediumIconSpacing))
                                        Text(Strings.openInBrowser(lang), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }

                            if (target != UpdateStage.Downloading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            vm.ignoreUpdate(info.latestVersion)
                                            close()
                                        },
                                        shapes = ButtonDefaults.shapes(),
                                        colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurfaceVariant)
                                    ) {
                                        Text(Strings.dontRemindAgain(lang), style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(
                                        onClick = close,
                                        shapes = ButtonDefaults.shapes()
                                    ) {
                                        Text(Strings.closeBtn(lang), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
                SheetSnackbarHost(Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun UpdatePrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.MediumContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MediumContainerHeight)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.MediumIconSize))
        Spacer(Modifier.width(ButtonDefaults.MediumIconSpacing))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
