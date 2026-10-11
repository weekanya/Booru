@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.booru.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.booru.app.data.AppLanguage
import com.booru.app.data.CountEntry
import com.booru.app.data.FavoriteStats
import com.booru.app.data.StatsPeriod
import com.booru.app.data.Strings
import com.booru.app.data.TagCategory
import com.booru.app.data.db.AppDatabase
import com.booru.app.data.db.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteStatsSheet(
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberExpandedSheetState()
    val locale = remember(lang) { Locale.forLanguageTag(lang.code) }

    var favorites by remember { mutableStateOf<List<FavoriteEntity>?>(null) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.ALL_TIME) }
    var sharing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        favorites = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).favoriteDao().getAllFavoritesList()
        }
    }

    val stats = remember(favorites, period) {
        favorites?.let { FavoriteStats.compute(it, period) }
    }
    val captureLayer = rememberGraphicsLayer()

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
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = ShapeTokens.Medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = statsTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            if (sharing) return@FilledTonalButton
                            sharing = true
                            scope.launch {
                                try {
                                    val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
                                    shareStatsImage(context, bitmap, Strings.tr(lang, "Share", "Поделиться", "共有", "分享", "공유", "مشاركة"))
                                } catch (_: Exception) {
                                } finally {
                                    sharing = false
                                }
                            }
                        },
                        enabled = stats?.isEmpty == false && !sharing,
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.tr(lang, "Share", "Поделиться", "共有", "分享", "공유", "مشاركة"))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val periods = StatsPeriod.entries
                    periods.forEachIndexed { index, item ->
                        val selected = item == period
                        ConnectedBarButton(
                            position = groupPosition(index, periods.size),
                            onClick = { period = item },
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = periodLabel(item, lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val background = MaterialTheme.colorScheme.surfaceContainerLow
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                captureLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(captureLayer)
                            }
                            .background(background)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when {
                            stats == null -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                            stats.isEmpty -> EmptyStats(lang)
                            else -> StatsContent(stats, period, lang, locale)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsContent(stats: FavoriteStats, period: StatsPeriod, lang: AppLanguage, locale: Locale) {
    HeroCard(stats, period, lang, locale)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(Icons.Rounded.Image, stats.images.toString(), Strings.tr(lang, "Images", "Картинки", "画像", "图片", "이미지", "صور"), Modifier.weight(1f))
        StatTile(Icons.Rounded.Gif, stats.gifs.toString(), "GIF", Modifier.weight(1f))
        StatTile(Icons.Rounded.Videocam, stats.videos.toString(), Strings.tr(lang, "Videos", "Видео", "動画", "视频", "동영상", "فيديو"), Modifier.weight(1f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            Icons.Rounded.LocalFireDepartment,
            daysLabel(stats.longestStreak, lang),
            Strings.tr(lang, "Longest streak", "Лучшая серия", "最長連続", "最长连续", "최장 연속", "أطول سلسلة"),
            Modifier.weight(1f)
        )
        StatTile(
            Icons.Rounded.CalendarMonth,
            stats.busiestDay?.getDisplayName(TextStyle.FULL_STANDALONE, locale)?.replaceFirstChar { it.titlecase(locale) } ?: "—",
            Strings.tr(lang, "Most active day", "Самый активный день", "最も活発な曜日", "最活跃的日子", "가장 활발한 요일", "أكثر يوم نشاطاً"),
            Modifier.weight(1f)
        )
    }

    if (stats.monthly.size > 1) {
        StatsCard(Icons.Rounded.BarChart, Strings.tr(lang, "Activity", "Активность", "アクティビティ", "活跃度", "활동", "النشاط")) {
            ActivityChart(stats, locale)
        }
    }

    if (stats.topTags.isNotEmpty()) {
        StatsCard(Icons.Rounded.Tag, Strings.tr(lang, "Top tags", "Топ тегов", "トップタグ", "热门标签", "인기 태그", "أكثر الوسوم")) {
            RankedList(stats.topTags, TagCategory.GENERAL.seed)
        }
    }
    if (stats.topCharacters.isNotEmpty()) {
        StatsCard(TagCategory.CHARACTER.icon, Strings.tr(lang, "Top characters", "Любимые персонажи", "トップキャラクター", "最爱角色", "인기 캐릭터", "أكثر الشخصيات")) {
            RankedList(stats.topCharacters.take(5), TagCategory.CHARACTER.seed)
        }
    }
    if (stats.topCopyrights.isNotEmpty()) {
        StatsCard(TagCategory.COPYRIGHT.icon, Strings.tr(lang, "Top franchises", "Любимые франшизы", "トップ作品", "最爱作品", "인기 작품", "أكثر الأعمال")) {
            RankedList(stats.topCopyrights.take(5), TagCategory.COPYRIGHT.seed)
        }
    }
    if (stats.topSources.isNotEmpty()) {
        StatsCard(Icons.Rounded.Public, Strings.tr(lang, "Sources", "Источники", "ソース", "来源", "소스", "المصادر")) {
            RankedList(stats.topSources.take(5), MaterialTheme.colorScheme.primary, prettify = false)
        }
    }

    if (stats.safe + stats.questionable + stats.explicit > 0) {
        StatsCard(Icons.Rounded.Shield, Strings.tr(lang, "Ratings", "Рейтинги", "レーティング", "分级", "등급", "التصنيفات")) {
            RatingBar(stats, lang)
        }
    }

    stats.topScored?.let { top ->
        StatsCard(Icons.Rounded.EmojiEvents, Strings.tr(lang, "Highest score", "Самый высокий рейтинг", "最高スコア", "最高评分", "최고 점수", "أعلى تقييم")) {
            TopScoredRow(top)
        }
    }

    Text(
        text = "Booru · booru.weebio.ru",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun HeroCard(stats: FavoriteStats, period: StatsPeriod, lang: AppLanguage, locale: Locale) {
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = periodLabel(period, lang),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = stats.total.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = Strings.tr(lang, "favorites saved", "в избранном", "件のお気に入り", "个收藏", "개의 즐겨찾기", "عنصر في المفضلة"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            stats.firstSavedAt?.let { first ->
                val date = Instant.ofEpochMilli(first).atZone(ZoneId.systemDefault()).toLocalDate()
                val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Strings.tr(lang, "since $formatted", "с $formatted", "$formatted から", "自 $formatted 起", "${formatted}부터", "منذ $formatted"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = ShapeTokens.Large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = ShapeTokens.Large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun RankedList(entries: List<CountEntry>, accent: Color, prettify: Boolean = true) {
    val max = entries.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEachIndexed { index, entry ->
            val fraction by animateFloatAsState((entry.count.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "rankBar")
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = if (prettify) entry.label.replace('_', ' ') else entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = entry.count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .padding(start = 22.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityChart(stats: FavoriteStats, locale: Locale) {
    val max = stats.monthly.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.monthly.forEach { (month, count) ->
            val fraction by animateFloatAsState((count.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "monthBar")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .clip(ShapeTokens.ExtraSmall)
                        .background(
                            if (count == max) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun RatingBar(stats: FavoriteStats, lang: AppLanguage) {
    val parts = listOf(
        Triple(Strings.tr(lang, "Safe", "Безопасно", "全年齢", "安全", "안전", "آمن"), stats.safe, MaterialTheme.colorScheme.tertiary),
        Triple(Strings.tr(lang, "Questionable", "Сомнительно", "センシティブ", "可疑", "민감", "مشكوك"), stats.questionable, MaterialTheme.colorScheme.secondary),
        Triple(Strings.tr(lang, "Explicit", "Откровенно", "成人向け", "露骨", "성인", "صريح"), stats.explicit, MaterialTheme.colorScheme.error)
    ).filter { it.second > 0 }
    val total = parts.sumOf { it.second }.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        parts.forEach { (_, count, color) ->
            Box(
                modifier = Modifier
                    .weight(count.toFloat())
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    parts.forEach { (label, count, color) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${count * 100 / total}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopScoredRow(top: FavoriteEntity) {
    val context = LocalContext.current
    val media = remember(top) { top.toRemoteMedia() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = remember(top.mediaKey) {
                ImageRequest.Builder(context)
                    .data(media.preview.ifBlank { media.sample.ifBlank { media.url } })
                    .crossfade(180)
                    .allowHardware(false)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(ShapeTokens.Medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = top.score.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = media.tagList.take(4).joinToString(", ") { it.replace('_', ' ') },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = top.source,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyStats(lang: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.QueryStats,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = Strings.tr(
                lang,
                "No favorites in this period",
                "За этот период избранного нет",
                "この期間のお気に入りはありません",
                "此期间没有收藏",
                "이 기간에는 즐겨찾기가 없습니다",
                "لا توجد مفضلات في هذه الفترة"
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun statsTitle(lang: AppLanguage) =
    Strings.tr(lang, "Your stats", "Ваша статистика", "あなたの統計", "你的统计", "내 통계", "إحصائياتك")

private fun periodLabel(period: StatsPeriod, lang: AppLanguage) = when (period) {
    StatsPeriod.ALL_TIME -> Strings.tr(lang, "All time", "Всё время", "全期間", "全部", "전체 기간", "كل الوقت")
    StatsPeriod.YEAR -> Strings.tr(lang, "This year", "Этот год", "今年", "今年", "올해", "هذا العام")
    StatsPeriod.MONTH -> Strings.tr(lang, "This month", "Этот месяц", "今月", "本月", "이번 달", "هذا الشهر")
}

private fun daysLabel(days: Int, lang: AppLanguage) =
    Strings.tr(lang, "$days days", "$days дн.", "${days}日", "$days 天", "${days}일", "$days يوم")

private suspend fun shareStatsImage(context: Context, bitmap: Bitmap, chooserTitle: String) {
    val uri = withContext(Dispatchers.IO) {
        val software = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "booru_stats.png")
        file.outputStream().use { software.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
