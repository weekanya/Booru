@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.booru.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.booru.app.GalleryViewModel
import com.booru.app.RemoteMedia
import com.booru.app.data.AppLanguage
import com.booru.app.data.BooruCacheManager
import com.booru.app.data.OfflineStore
import com.booru.app.data.Strings

fun offlineTitle(lang: AppLanguage) =
    Strings.tr(lang, "Offline posts", "Офлайн-посты", "オフライン投稿", "离线帖子", "오프라인 게시물", "منشورات بلا إنترنت")

fun offlineSubtitle(lang: AppLanguage): String {
    val count = OfflineStore.posts.size
    return if (count == 0) {
        Strings.tr(lang, "Save posts to watch without internet", "Сохраните посты, чтобы смотреть без интернета", "インターネットなしで見られるよう保存", "保存帖子以便离线观看", "인터넷 없이 보도록 저장", "احفظ المنشورات لمشاهدتها بلا إنترنت")
    } else {
        val size = BooruCacheManager.formatBytes(OfflineStore.sizeBytes)
        Strings.tr(lang, "$count posts · $size", "Постов: $count · $size", "$count 件 · $size", "$count 个帖子 · $size", "게시물 ${count}개 · $size", "$count منشور · $size")
    }
}

private data class CountOption(val count: Int, val label: (AppLanguage) -> String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val OFFLINE_OPTIONS = listOf(
    CountOption(100, { Strings.tr(it, "Quick look", "Быстро", "サクッと", "快速", "가볍게", "نظرة سريعة") }, Icons.Rounded.Bolt),
    CountOption(200, { Strings.tr(it, "For the evening", "На вечер", "夜のお供", "晚上看", "저녁용", "للمساء") }, Icons.Rounded.Bedtime),
    CountOption(300, { Strings.tr(it, "For a trip", "В дорогу", "移動用", "旅途用", "이동용", "للسفر") }, Icons.Rounded.Flight),
    CountOption(500, { Strings.tr(it, "Maximum", "Максимум", "最大", "最多", "최대", "الحد الأقصى") }, Icons.Rounded.AllInclusive)
)

@Composable
fun OfflineSheet(vm: GalleryViewModel, lang: AppLanguage, onDismiss: () -> Unit) {
    val count = vm.offlineCount
    val context = LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { vm.startOfflineDownload(vm.offlineCount) }
    val startDownload = {
        val needsPermission = android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startOfflineDownload(count)
        }
    }
    val saved = OfflineStore.posts.size
    val downloading = vm.offlineDownloading
    val scheme = MaterialTheme.colorScheme
    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberExpandedSheetState(),
            containerColor = scheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeIncreasedTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val added = com.booru.app.data.OfflineDownloader.lastAdded
                LaunchedEffect(added) {
                    if (added >= 0) {
                        kotlinx.coroutines.delay(4000)
                        com.booru.app.data.OfflineDownloader.clearLastAdded()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    OfflineHeroBadge(
                        state = when {
                            downloading -> 1
                            saved > 0 -> 2
                            else -> 0
                        }
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        offlineTitle(lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(
                        visible = added > 0 && !downloading,
                        enter = fadeIn(Motion.effectsDefault()) + androidx.compose.animation.scaleIn(Motion.spatialFast(), initialScale = 0.6f),
                        exit = fadeOut(Motion.effectsDefault()) + androidx.compose.animation.scaleOut(Motion.spatialDefault(), targetScale = 0.6f)
                    ) {
                        Surface(shape = CircleShape, color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("+${added.coerceAtLeast(0)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = saved > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Surface(
                            onClick = {
                                onDismiss()
                                vm.openOfflinePosts()
                            },
                            shape = ShapeTokens.ExtraLarge,
                            color = scheme.secondaryContainer,
                            contentColor = scheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
                            ) {
                                ThumbStack(OfflineStore.posts.take(4))
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        Strings.tr(lang, "$saved posts", "Постов: $saved", "$saved 件", "$saved 个帖子", "게시물 ${saved}개", "$saved منشور"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        BooruCacheManager.formatBytes(OfflineStore.sizeBytes),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = Strings.tr(lang, "Open offline posts", "Открыть офлайн-посты", "オフライン投稿を開く", "打开离线帖子", "오프라인 게시물 열기", "فتح المنشورات بلا إنترنت"))
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = downloading,
                    transitionSpec = {
                        (fadeIn(Motion.effectsDefault()) + androidx.compose.animation.scaleIn(Motion.spatialDefault(), initialScale = 0.92f)) togetherWith
                            (fadeOut(Motion.effectsDefault()) + androidx.compose.animation.scaleOut(Motion.spatialDefault(), targetScale = 0.92f))
                    },
                    label = "offlineState"
                ) { isDownloading ->
                    if (isDownloading) {
                        DownloadProgress(vm, lang)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OFFLINE_OPTIONS.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { option ->
                                        CountTile(
                                            option = option,
                                            lang = lang,
                                            selected = count == option.count,
                                            onClick = { vm.updateOfflineCount(option.count) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            vm.offlineError?.let { error ->
                                Text(error, style = MaterialTheme.typography.bodySmall, color = scheme.error, modifier = Modifier.padding(start = 4.dp))
                            }
                            val complete = saved >= count
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = startDownload,
                                    enabled = !complete,
                                    shape = if (saved > 0) androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp) else CircleShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    modifier = Modifier.weight(1f).height(60.dp)
                                ) {
                                    Icon(if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        if (complete) {
                                            Strings.tr(lang, "All $count saved", "Все $count сохранены", "$count 件すべて保存済み", "已全部保存 $count 个", "${count}개 모두 저장됨", "تم حفظ $count كلها")
                                        } else if (saved > 0) {
                                            Strings.tr(lang, "Download ${count - saved} more", "Докачать ${count - saved}", "残り ${count - saved} 件を取得", "再下载 ${count - saved} 个", "${count - saved}개 더 받기", "تنزيل ${count - saved} أخرى")
                                        } else {
                                            Strings.tr(lang, "Download $count", "Скачать $count", "$count 件をダウンロード", "下载 $count 个", "${count}개 다운로드", "تنزيل $count")
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                if (saved > 0) {
                                    AnimatedConfirmDeleteButton(
                                        onConfirmed = { vm.deleteOfflinePosts() },
                                        lang = lang,
                                        height = 60.dp,
                                        idleContainerColor = scheme.errorContainer,
                                        idleContentColor = scheme.onErrorContainer,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp)
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

@Composable
private fun OfflineHeroBadge(state: Int) {
    val scheme = MaterialTheme.colorScheme
    val polygon = when (state) {
        1 -> MaterialShapes.SoftBurst
        2 -> MaterialShapes.Sunny
        else -> MaterialShapes.Cookie9Sided
    }
    var from by remember { mutableStateOf(polygon) }
    var to by remember { mutableStateOf(polygon) }
    val morphProgress = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(polygon) {
        if (polygon !== to) {
            from = to
            to = polygon
            morphProgress.snapTo(0f)
            morphProgress.animateTo(1f, Motion.spatialDefault())
        }
    }
    val morph = remember(from, to) { androidx.graphics.shapes.Morph(from, to) }
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "heroSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(6000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "heroAngle"
    )
    val slowAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(16000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "heroSlowAngle"
    )
    val container by androidx.compose.animation.animateColorAsState(
        when (state) {
            1 -> scheme.secondaryContainer
            2 -> scheme.tertiaryContainer
            else -> scheme.primaryContainer
        },
        Motion.effectsDefault(),
        label = "heroContainer"
    )
    val content = when (state) {
        1 -> scheme.onSecondaryContainer
        2 -> scheme.onTertiaryContainer
        else -> scheme.onPrimaryContainer
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = if (state == 1) angle else slowAngle }
                .background(container, MorphShape(morph, morphProgress.value.coerceIn(0f, 1f)))
        )
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(Motion.effectsDefault()) + androidx.compose.animation.scaleIn(Motion.spatialDefault(), initialScale = 0.6f)) togetherWith
                    (fadeOut(Motion.effectsDefault()) + androidx.compose.animation.scaleOut(Motion.spatialDefault(), targetScale = 0.6f))
            },
            label = "heroIcon"
        ) { s ->
            Icon(
                when (s) {
                    1 -> Icons.Rounded.Downloading
                    2 -> Icons.Rounded.OfflinePin
                    else -> Icons.Rounded.DownloadForOffline
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CountTile(option: CountOption, lang: AppLanguage, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val corner by androidx.compose.animation.core.animateDpAsState(if (selected) 32.dp else 16.dp, Motion.spatialFast(), label = "tileCorner")
    val container by androidx.compose.animation.animateColorAsState(if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh, Motion.effectsDefault(), label = "tileColor")
    val content by androidx.compose.animation.animateColorAsState(if (selected) scheme.onPrimaryContainer else scheme.onSurface, Motion.effectsDefault(), label = "tileContent")
    val checkScale by androidx.compose.animation.core.animateFloatAsState(if (selected) 1f else 0f, Motion.spatialFast(), label = "tileCheck")
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = modifier
            .height(104.dp)
            .semantics { role = Role.RadioButton }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Icon(option.icon, contentDescription = null, modifier = Modifier.size(20.dp).align(Alignment.TopStart))
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(option.count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(option.label(lang), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ThumbStack(items: List<RemoteMedia>) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.width((40 + (items.size - 1).coerceAtLeast(0) * 18).dp).height(40.dp)) {
        items.forEachIndexed { index, media ->
            val url = media.preview.ifBlank { media.sample }
            AsyncImage(
                model = remember(url) { ImageRequest.Builder(context).data(url).crossfade(150).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = (index * 18).dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
                    .border(2.dp, scheme.surfaceContainerHigh, CircleShape)
            )
        }
    }
}

@Composable
private fun DownloadProgress(vm: GalleryViewModel, lang: AppLanguage) {
    val scheme = MaterialTheme.colorScheme
    val collecting = vm.offlinePhase == 0
    val goal = vm.offlineGoal.coerceAtLeast(1)
    val fraction by androidx.compose.animation.core.animateFloatAsState(
        ((vm.offlineBase + vm.offlineDone).toFloat() / goal).coerceIn(0f, 1f),
        Motion.effectsDefault(),
        label = "offlineProgress"
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stroke = remember(density) {
        androidx.compose.ui.graphics.drawscope.Stroke(
            width = with(density) { 6.dp.toPx() },
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
            if (collecting) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    stroke = stroke,
                    trackStroke = stroke,
                    wavelength = 36.dp
                )
            } else {
                CircularWavyProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    stroke = stroke,
                    trackStroke = stroke,
                    amplitude = { 1f },
                    wavelength = 36.dp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (collecting) "${vm.offlineDone}" else "${OfflineStore.posts.size}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface
                )
                Text(
                    if (collecting) {
                        Strings.tr(lang, "found", "найдено", "件見つかりました", "已找到", "찾음", "تم العثور")
                    } else {
                        Strings.tr(lang, "of ${vm.offlineGoal}", "из ${vm.offlineGoal}", "/ ${vm.offlineGoal}", "/ ${vm.offlineGoal}", "/ ${vm.offlineGoal}", "من ${vm.offlineGoal}")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (collecting) {
                Strings.tr(lang, "Finding posts…", "Подбираем посты…", "投稿を探しています…", "正在查找帖子…", "게시물 찾는 중…", "جارٍ البحث عن المنشورات…")
            } else {
                Strings.tr(lang, "Downloading…", "Скачиваем…", "ダウンロード中…", "正在下载…", "다운로드 중…", "جارٍ التنزيل…")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        if (!collecting) {
            Text(
                BooruCacheManager.formatBytes(vm.offlineBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = { vm.cancelOfflineDownload() },
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = scheme.errorContainer, contentColor = scheme.onErrorContainer),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(Strings.tr(lang, "Stop", "Остановить", "停止", "停止", "중지", "إيقاف"), fontWeight = FontWeight.Bold)
        }
    }
}

private enum class OfflineFilter { ALL, IMAGES, GIFS, VIDEOS }

@Composable
fun OfflinePostsScreen(vm: GalleryViewModel, lang: AppLanguage, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val scheme = MaterialTheme.colorScheme
    var filter by rememberSaveable { mutableStateOf(OfflineFilter.ALL) }
    val all = OfflineStore.posts
    val items = remember(all, filter, vm.tagBlacklist) {
        all.filter { media ->
            !vm.isBlacklisted(media) && when (filter) {
                OfflineFilter.ALL -> true
                OfflineFilter.IMAGES -> !media.isVideo && !media.isGif
                OfflineFilter.GIFS -> media.isGif
                OfflineFilter.VIDEOS -> media.isVideo
            }
        }
    }
    val gridState = rememberLazyStaggeredGridState()
    Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.tr(lang, "Back", "Назад", "戻る", "返回", "뒤로", "رجوع"))
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(Icons.Rounded.OfflinePin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(BooruCacheManager.formatBytes(OfflineStore.sizeBytes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                offlineTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
            Text(
                Strings.tr(lang, "Works without internet", "Работает без интернета", "インターネットなしで閲覧可能", "无需网络即可浏览", "인터넷 없이 볼 수 있어요", "يعمل بلا إنترنت"),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            val counts = remember(all) {
                mapOf(
                    OfflineFilter.ALL to all.size,
                    OfflineFilter.IMAGES to all.count { !it.isVideo && !it.isGif },
                    OfflineFilter.GIFS to all.count { it.isGif },
                    OfflineFilter.VIDEOS to all.count { it.isVideo }
                )
            }
            val options = listOf(
                Triple(OfflineFilter.ALL, Strings.tr(lang, "All", "Все", "すべて", "全部", "전체", "الكل"), Icons.Rounded.GridView),
                Triple(OfflineFilter.IMAGES, Strings.tr(lang, "Images", "Фото", "画像", "图片", "이미지", "صور"), Icons.Rounded.Image),
                Triple(OfflineFilter.GIFS, "GIF", Icons.Rounded.Gif),
                Triple(OfflineFilter.VIDEOS, Strings.tr(lang, "Videos", "Видео", "動画", "视频", "동영상", "فيديو"), Icons.Rounded.PlayCircle)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                options.forEachIndexed { index, (value, label, icon) ->
                    val selected = filter == value
                    val outer by androidx.compose.animation.core.animateDpAsState(if (selected) 36.dp else 20.dp, Motion.spatialFast(), label = "filterOuter")
                    val inner by androidx.compose.animation.core.animateDpAsState(if (selected) 36.dp else 6.dp, Motion.spatialFast(), label = "filterInner")
                    val first = index == 0
                    val last = index == options.lastIndex
                    val container by androidx.compose.animation.animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHigh, Motion.effectsDefault(), label = "filterColor")
                    val content by androidx.compose.animation.animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, Motion.effectsDefault(), label = "filterContent")
                    Surface(
                        onClick = { filter = value },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = if (first) outer else inner,
                            bottomStart = if (first) outer else inner,
                            topEnd = if (last) outer else inner,
                            bottomEnd = if (last) outer else inner
                        ),
                        color = container,
                        contentColor = content,
                        modifier = Modifier.weight(1f).height(72.dp).semantics { role = Role.Tab }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            Text("${counts[value] ?: 0}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(shape = CircleShape, color = scheme.surfaceContainerHigh, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        Strings.tr(lang, "Nothing saved here yet", "Здесь пока ничего нет", "まだ何も保存されていません", "这里还没有内容", "아직 저장된 게시물이 없습니다", "لا يوجد شيء محفوظ هنا بعد"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                    state = gridState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { _, m -> m.mediaKey }) { index, media ->
                        OfflineCard(
                            media = media,
                            modifier = Modifier.edgeReveal(gridState, media.mediaKey),
                            onClick = { vm.openFullscreen(items, index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineCard(media: RemoteMedia, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val ratio = remember(media.mediaKey) {
        if (media.width > 0 && media.height > 0) (media.width.toFloat() / media.height).coerceIn(0.55f, 1.6f) else 0.75f
    }
    val url = remember(media.mediaKey) {
        if (media.isVideo) media.preview.ifBlank { media.sample } else media.sample.ifBlank { media.url.ifBlank { media.preview } }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(ShapeTokens.LargeIncreased)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = remember(url) { ImageRequest.Builder(context).data(url).crossfade(180).build() },
            contentDescription = media.tagList.take(4).joinToString(", "),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (media.isVideo || media.isGif) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(
                    if (media.isVideo) Icons.Rounded.PlayArrow else Icons.Rounded.Gif,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp).size(16.dp)
                )
            }
        }
    }
}
