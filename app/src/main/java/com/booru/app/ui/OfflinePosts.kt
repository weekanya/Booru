@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.booru.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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

private val OFFLINE_COUNTS = listOf(100, 200, 300, 500)

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

@Composable
fun OfflineSheet(vm: GalleryViewModel, lang: AppLanguage, onDismiss: () -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(200) }
    val saved = OfflineStore.posts.size
    val scheme = MaterialTheme.colorScheme
    SheetMotion {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberExpandedSheetState(),
            containerColor = scheme.surfaceContainerLow,
            shape = ShapeTokens.ExtraLargeTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = ShapeTokens.Medium, color = scheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.DownloadForOffline, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(offlineTitle(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Text(
                            Strings.tr(lang, "From your current feed, search and filters", "Из текущей ленты, поиска и фильтров", "現在のフィード・検索・フィルターから", "来自当前的推荐、搜索和筛选", "현재 피드, 검색, 필터 기준", "من خلاصتك وبحثك وعوامل التصفية الحالية"),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedContent(
                    targetState = vm.offlineDownloading,
                    transitionSpec = { fadeIn(Motion.effectsDefault()) togetherWith fadeOut(Motion.effectsDefault()) },
                    label = "offlineState"
                ) { downloading ->
                    if (downloading) {
                        DownloadProgress(vm, lang)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                Strings.tr(lang, "How many posts", "Сколько постов", "投稿数", "帖子数量", "게시물 수", "عدد المنشورات"),
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            ToggleGroupColors {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                ) {
                                    OFFLINE_COUNTS.forEachIndexed { index, value ->
                                        ToggleButton(
                                            checked = count == value,
                                            onCheckedChange = { count = value },
                                            shapes = groupPosition(index, OFFLINE_COUNTS.size).toggleShapes(),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .semantics { role = Role.RadioButton }
                                        ) {
                                            Text(value.toString(), fontWeight = if (count == value) FontWeight.Bold else FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            if (vm.isMeteredNetwork) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                    Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        Strings.tr(lang, "You're on mobile data. Videos can be large.", "Вы в мобильной сети. Видео могут весить много.", "モバイルデータ通信中です。動画は容量が大きい場合があります。", "正在使用移动数据，视频可能较大。", "모바일 데이터 사용 중입니다. 동영상은 용량이 클 수 있습니다.", "أنت على بيانات الجوال. قد تكون مقاطع الفيديو كبيرة."),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                            vm.offlineError?.let { error ->
                                Text(error, style = MaterialTheme.typography.bodySmall, color = scheme.error, modifier = Modifier.padding(start = 4.dp))
                            }
                            Button(
                                onClick = { vm.startOfflineDownload(count) },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (saved > 0) {
                                        Strings.tr(lang, "Replace with $count posts", "Заменить на $count постов", "$count 件で置き換え", "替换为 $count 个帖子", "${count}개로 교체", "استبدال بـ $count منشور")
                                    } else {
                                        Strings.tr(lang, "Download $count posts", "Скачать $count постов", "$count 件をダウンロード", "下载 $count 个帖子", "${count}개 다운로드", "تنزيل $count منشور")
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = saved > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(shape = ShapeTokens.ExtraLarge, color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    Strings.tr(lang, "Saved on this phone", "Сохранено на телефоне", "この端末に保存済み", "已保存到本机", "이 기기에 저장됨", "محفوظ على هذا الهاتف"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(offlineSubtitle(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            }
                            AnimatedConfirmDeleteButton(
                                onConfirmed = { vm.deleteOfflinePosts() },
                                lang = lang,
                                compact = true
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        vm.openOfflinePosts()
                    },
                    enabled = saved > 0,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.tr(lang, "Open offline posts", "Открыть офлайн-посты", "オフライン投稿を開く", "打开离线帖子", "오프라인 게시물 열기", "فتح المنشورات بلا إنترنت"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(vm: GalleryViewModel, lang: AppLanguage) {
    val scheme = MaterialTheme.colorScheme
    val collecting = vm.offlinePhase == 0
    val target = vm.offlineTarget.coerceAtLeast(1)
    Surface(shape = ShapeTokens.ExtraLarge, color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (collecting) {
                            Strings.tr(lang, "Finding posts…", "Подбираем посты…", "投稿を探しています…", "正在查找帖子…", "게시물 찾는 중…", "جارٍ البحث عن المنشورات…")
                        } else {
                            Strings.tr(lang, "Downloading…", "Скачиваем…", "ダウンロード中…", "正在下载…", "다운로드 중…", "جارٍ التنزيل…")
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        "${vm.offlineDone} / ${vm.offlineTarget}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { vm.cancelOfflineDownload() }, shapes = ButtonDefaults.shapes()) {
                    Text(Strings.tr(lang, "Stop", "Стоп", "停止", "停止", "중지", "إيقاف"))
                }
            }
            LinearWavyProgressIndicator(
                progress = { (vm.offlineDone.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                Strings.tr(lang, "Keep the app open until it finishes.", "Не закрывайте приложение до конца загрузки.", "完了までアプリを開いたままにしてください。", "完成前请保持应用打开。", "완료될 때까지 앱을 열어 두세요.", "أبقِ التطبيق مفتوحاً حتى ينتهي."),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
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
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.tr(lang, "Back", "Назад", "戻る", "返回", "뒤로", "رجوع"))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(offlineTitle(lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(offlineSubtitle(lang), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
            }
            val labels = listOf(
                OfflineFilter.ALL to Strings.tr(lang, "All", "Все", "すべて", "全部", "전체", "الكل"),
                OfflineFilter.IMAGES to Strings.tr(lang, "Images", "Картинки", "画像", "图片", "이미지", "صور"),
                OfflineFilter.GIFS to "GIF",
                OfflineFilter.VIDEOS to Strings.tr(lang, "Videos", "Видео", "動画", "视频", "동영상", "فيديو")
            )
            ToggleGroupColors {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    labels.forEachIndexed { index, (value, label) ->
                        ToggleButton(
                            checked = filter == value,
                            onCheckedChange = { filter = value },
                            shapes = groupPosition(index, labels.size).toggleShapes(),
                            modifier = Modifier.weight(1f).semantics { role = Role.Tab }
                        ) {
                            Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
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
