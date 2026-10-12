@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.booru.app.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.booru.app.data.TagCategoryCache
import com.booru.app.data.db.AppDatabase
import com.booru.app.data.db.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun FavoriteStatsScreen(
    lang: AppLanguage,
    allowTagLookup: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        var backProgress by remember { mutableFloatStateOf(0f) }
        PredictiveBackHandler { events ->
            try {
                events.collect { backProgress = it.progress }
                onDismiss()
            } catch (e: kotlinx.coroutines.CancellationException) {
                backProgress = 0f
                throw e
            }
        }
        Box(
            modifier = Modifier.graphicsLayer {
                val scale = 1f - 0.08f * backProgress
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape((32 * backProgress).dp)
                clip = backProgress > 0f
            }
        ) {
            FavoriteStatsContent(lang, allowTagLookup, onDismiss)
        }
    }
}

@Composable
private fun FavoriteStatsContent(lang: AppLanguage, allowTagLookup: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = remember(lang) { Locale.forLanguageTag(lang.code) }

    var favorites by remember { mutableStateOf<List<FavoriteEntity>?>(null) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.ALL_TIME) }
    var lookupProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showPeriodSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).favoriteDao().getAllFavoritesList()
        }
        TagCategoryCache.ensureLoaded()
        favorites = list
        if (allowTagLookup && list.isNotEmpty()) {
            TagCategoryCache.resolve(FavoriteStats.allTags(list)) { done, total ->
                lookupProgress = done to total
            }
            lookupProgress = null
        }
    }

    val cacheVersion = TagCategoryCache.version
    val stats by produceState<FavoriteStats?>(null, favorites, period, cacheVersion) {
        val list = favorites ?: return@produceState
        value = withContext(Dispatchers.Default) { FavoriteStats.compute(list, period) }
    }
    val tabs = listOf(
        Strings.tr(lang, "Overview", "Обзор", "概要", "概览", "개요", "نظرة عامة") to Icons.Rounded.Insights,
        Strings.tr(lang, "Favorites", "Любимое", "お気に入り", "最爱", "최애", "المفضلة") to Icons.Rounded.FavoriteBorder,
        Strings.tr(lang, "Activity", "Активность", "アクティビティ", "活跃度", "활동", "النشاط") to Icons.Rounded.BarChart
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val tabController = rememberTabPagerController(pagerState)

    if (showPeriodSheet) {
        StatsPeriodSheet(
            current = period,
            lang = lang,
            onSelect = {
                period = it
                showPeriodSheet = false
            },
            onDismiss = { showPeriodSheet = false }
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = Strings.tr(lang, "Back", "Назад", "戻る", "返回", "뒤로", "رجوع")
                    )
                }
                Text(
                    text = statsTitle(lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
                FilledTonalButton(
                    onClick = { showPeriodSheet = true },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(start = 12.dp, end = 8.dp)
                ) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(periodLabel(period, lang), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(
                visible = lookupProgress != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val (done, total) = lookupProgress ?: (0 to 1)
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = Strings.tr(
                            lang,
                            "Sorting tags into characters, artists and series…",
                            "Распознаём персонажей, авторов и франшизы…",
                            "キャラクター・作者・作品を判別中…",
                            "正在识别角色、画师和作品…",
                            "캐릭터, 작가, 작품을 분류하는 중…",
                            "جارٍ تصنيف الشخصيات والفنانين والأعمال…"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearWavyProgressIndicator(
                        progress = { done.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val current = stats
            when {
                current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                current.isEmpty -> EmptyStats(lang)
                else -> Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = tabs.size - 1,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                    .readableContentWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 8.dp, bottom = 112.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (page) {
                                    0 -> OverviewPage(current, period, lang, locale)
                                    1 -> FavoritesPage(current, lang)
                                    else -> ActivityPage(current, lang, locale)
                                }
                            }
                        }
                    }
                    FloatingTabToolbar(
                        tabs = tabs.map { FloatingTab(it.first, it.second) },
                        selected = tabController.selectedPage,
                        onSelect = { index -> tabController.select(index) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewPage(stats: FavoriteStats, period: StatsPeriod, lang: AppLanguage, locale: Locale) {
    SummaryCard(stats, period, lang, locale)

    val spotlights = listOfNotNull(
        stats.topCharacters.firstOrNull()?.let { Triple(TagCategory.CHARACTER, Strings.tr(lang, "Favorite character", "Любимый персонаж", "推しキャラ", "最爱角色", "최애 캐릭터", "الشخصية المفضلة"), it) },
        stats.topArtists.firstOrNull()?.let { Triple(TagCategory.ARTIST, Strings.tr(lang, "Favorite artist", "Любимый автор", "推し絵師", "最爱画师", "최애 작가", "الفنان المفضل"), it) },
        stats.topCopyrights.firstOrNull()?.let { Triple(TagCategory.COPYRIGHT, Strings.tr(lang, "Favorite series", "Любимая франшиза", "推し作品", "最爱作品", "최애 작품", "العمل المفضل"), it) }
    )
    if (spotlights.isNotEmpty()) {
        SectionHeader(Strings.tr(lang, "Your top picks", "Ваши любимчики", "あなたの推し", "你的最爱", "나의 최애", "مفضلاتك"))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            spotlights.forEachIndexed { index, (category, title, entry) ->
                SpotlightRow(category, title, entry, lang, segmentShape(index, spotlights.size))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        FactTile(
            icon = Icons.Rounded.LocalFireDepartment,
            value = daysLabel(stats.longestStreak, lang),
            label = Strings.tr(lang, "Longest streak of days in a row", "Лучшая серия дней подряд", "最長連続日数", "最长连续天数", "최장 연속 일수", "أطول سلسلة أيام متتالية"),
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        FactTile(
            icon = Icons.Rounded.CalendarMonth,
            value = stats.busiestDay?.getDisplayName(TextStyle.FULL_STANDALONE, locale)?.replaceFirstChar { it.titlecase(locale) }
                ?: daysLabel(stats.activeDays, lang),
            label = if (stats.busiestDay != null) {
                Strings.tr(lang, "Most active day", "Самый активный день", "最も活発な曜日", "最活跃的日子", "가장 활발한 요일", "أكثر يوم نشاطاً")
            } else {
                Strings.tr(lang, "Active days", "Активных дней", "活動日数", "活跃天数", "활동한 날", "أيام النشاط")
            },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun FavoritesPage(stats: FavoriteStats, lang: AppLanguage) {
    val groups = listOf(
        Triple(TagCategory.CHARACTER, Strings.tr(lang, "Characters", "Персонажи", "キャラクター", "角色", "캐릭터", "الشخصيات"), stats.topCharacters),
        Triple(TagCategory.ARTIST, Strings.tr(lang, "Artists", "Авторы", "絵師", "画师", "작가", "الفنانون"), stats.topArtists),
        Triple(TagCategory.COPYRIGHT, Strings.tr(lang, "Series", "Франшизы", "作品", "作品", "작품", "الأعمال"), stats.topCopyrights)
    ).filter { it.third.isNotEmpty() }

    if (stats.topTags.isNotEmpty()) {
        StatsCard(
            Icons.Rounded.Tag,
            Strings.tr(lang, "Your vibe", "Ваш вайб", "あなたの好み", "你的口味", "나의 취향", "ذوقك"),
            Strings.tr(lang, "Most common tags in your favorites", "Самые частые теги в избранном", "お気に入りで多いタグ", "收藏中最常见的标签", "즐겨찾기에서 가장 많은 태그", "الوسوم الأكثر تكراراً في مفضلتك")
        ) {
            TagCloud(stats.topTags)
        }
    }
    groups.forEach { (category, title, entries) ->
        StatsCard(
            category.icon,
            title,
            Strings.tr(lang, "Posts in favorites with each one", "Сколько постов с ними в избранном", "お気に入りの投稿数", "收藏中的作品数", "즐겨찾기 게시물 수", "عدد المنشورات في المفضلة")
        ) {
            RankedBars(entries.take(8), MaterialTheme.colorScheme.primary)
        }
    }
    if (stats.topSources.size > 1) {
        StatsCard(
            Icons.Rounded.Public,
            Strings.tr(lang, "Sources", "Источники", "ソース", "来源", "소스", "المصادر"),
            Strings.tr(lang, "Where your favorites come from", "Откуда вы чаще всего добавляете", "お気に入りの取得元", "收藏来自哪里", "즐겨찾기 출처", "مصادر مفضلتك")
        ) {
            RankedBars(stats.topSources.take(5), MaterialTheme.colorScheme.primary, prettify = false)
        }
    }
    if (groups.isEmpty() && stats.topTags.isEmpty()) EmptyStats(lang)
}

@Composable
private fun ActivityPage(stats: FavoriteStats, lang: AppLanguage, locale: Locale) {
    if (stats.monthly.size > 1) {
        StatsCard(
            Icons.Rounded.BarChart,
            Strings.tr(lang, "By month", "По месяцам", "月別", "按月", "월별", "حسب الشهر"),
            Strings.tr(lang, "Favorites added each month", "Сколько добавлено за каждый месяц", "月ごとの追加数", "每月新增收藏", "월별 추가 수", "المضاف كل شهر")
        ) {
            ActivityChart(stats, locale, lang)
        }
    }

    StatsCard(
        Icons.Rounded.PieChart,
        Strings.tr(lang, "Mix", "Состав", "内訳", "构成", "구성", "التركيبة"),
        Strings.tr(lang, "File types and ratings", "Типы файлов и рейтинг", "ファイル形式とレーティング", "文件类型与分级", "파일 형식과 등급", "أنواع الملفات والتصنيفات")
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val secondary = MaterialTheme.colorScheme.secondary
        SplitBar(
            listOf(
                Triple(Strings.tr(lang, "Images", "Картинки", "画像", "图片", "이미지", "صور"), stats.images, primary),
                Triple("GIF", stats.gifs, tertiary),
                Triple(Strings.tr(lang, "Videos", "Видео", "動画", "视频", "동영상", "فيديو"), stats.videos, secondary)
            )
        )
        if (stats.safe + stats.questionable + stats.explicit > 0) {
            Spacer(Modifier.height(16.dp))
            SplitBar(
                listOf(
                    Triple(Strings.tr(lang, "Safe", "Безопасно", "全年齢", "安全", "안전", "آمن"), stats.safe, tertiary),
                    Triple(Strings.tr(lang, "Questionable", "Сомнительно", "センシティブ", "可疑", "민감", "مشكوك"), stats.questionable, secondary),
                    Triple(Strings.tr(lang, "Explicit", "Откровенно", "成人向け", "露骨", "성인", "صريح"), stats.explicit, primary)
                )
            )
        }
    }

    stats.topScored?.let { top ->
        StatsCard(
            Icons.Rounded.EmojiEvents,
            Strings.tr(lang, "Highest score", "Самый высокий рейтинг", "最高スコア", "最高评分", "최고 점수", "أعلى تقييم"),
            Strings.tr(lang, "Your favorite with the most votes on its site", "Пост с наибольшим рейтингом на сайте", "サイトで最も評価された投稿", "在站点评分最高的收藏", "사이트 점수가 가장 높은 게시물", "المنشور الأعلى تقييماً")
        ) {
            TopScoredRow(top)
        }
    }
}

@Composable
private fun SummaryCard(stats: FavoriteStats, period: StatsPeriod, lang: AppLanguage, locale: Locale) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = periodLabel(period, lang),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stats.total.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = Strings.tr(lang, "favorites", "в избранном", "件", "个收藏", "개", "مفضلة"),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            stats.firstSavedAt?.let { first ->
                val date = Instant.ofEpochMilli(first).atZone(ZoneId.systemDefault()).toLocalDate()
                val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
                Text(
                    text = Strings.tr(lang, "since $formatted", "с $formatted", "$formatted から", "自 $formatted 起", "${formatted}부터", "منذ $formatted"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                val items = listOf(
                    Triple(Icons.Rounded.Image, stats.images, Strings.tr(lang, "Images", "Картинки", "画像", "图片", "이미지", "صور")),
                    Triple(Icons.Rounded.Gif, stats.gifs, "GIF"),
                    Triple(Icons.Rounded.Videocam, stats.videos, Strings.tr(lang, "Videos", "Видео", "動画", "视频", "동영상", "فيديو"))
                )
                items.forEachIndexed { index, (icon, count, label) ->
                    val outer = 16.dp
                    val inner = 4.dp
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
                        items.lastIndex -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
                        else -> RoundedCornerShape(inner)
                    }
                    Surface(shape = shape, color = scheme.surfaceContainerHighest, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun segmentShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    return when {
        count == 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

@Composable
private fun SpotlightRow(category: TagCategory, title: String, entry: CountEntry, lang: AppLanguage, shape: RoundedCornerShape) {
    val tint = categoryTint(category)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Thumbnail(entry.sample, Modifier.size(72.dp).clip(ShapeTokens.Large))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = prettyTag(entry.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Strings.tr(lang, "${entry.count} in favorites", "${entry.count} в избранном", "お気に入り ${entry.count} 件", "收藏 ${entry.count} 个", "즐겨찾기 ${entry.count}개", "${entry.count} في المفضلة"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(favorite: FavoriteEntity?, modifier: Modifier) {
    val context = LocalContext.current
    val url = favorite?.let { it.preview.ifBlank { it.sample.ifBlank { it.url } } }
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(180)
                        .allowHardware(false)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun StatsCard(icon: ImageVector, title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                Surface(shape = ShapeTokens.Medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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
            content()
        }
    }
}

@Composable
private fun FactTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RankedBars(entries: List<CountEntry>, accent: Color, startRank: Int = 1, prettify: Boolean = true) {
    val max = entries.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEachIndexed { index, entry ->
            val fraction by animateFloatAsState((entry.count.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "rankBar")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + startRank}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(22.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(ShapeTokens.Small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(accent.copy(alpha = 0.28f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (prettify) prettyTag(entry.label) else entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = entry.count.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(entries: List<CountEntry>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            val highlight = index < 3
            Surface(
                shape = CircleShape,
                color = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prettyTag(entry.label),
                        style = if (highlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                        color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (highlight) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = entry.count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityChart(stats: FavoriteStats, locale: Locale, lang: AppLanguage) {
    val max = stats.monthly.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val peak = stats.monthly.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    if (peak != null) {
        val monthName = peak.first.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
        Text(
            text = Strings.tr(lang, "Peak: $monthName · ${peak.second}", "Пик: $monthName · ${peak.second}", "ピーク：$monthName · ${peak.second}", "高峰：$monthName · ${peak.second}", "최고: $monthName · ${peak.second}", "الذروة: $monthName · ${peak.second}"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.monthly.forEach { (month, count) ->
            val fraction by animateFloatAsState((count.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "monthBar")
            val isPeak = peak != null && month == peak.first
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            when {
                                isPeak -> MaterialTheme.colorScheme.primary
                                count > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = month.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SplitBar(parts: List<Triple<String, Int, Color>>) {
    val visible = parts.filter { it.second > 0 }
    if (visible.isEmpty()) return
    val total = visible.sumOf { it.second }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        visible.forEach { (_, count, color) ->
            Box(
                modifier = Modifier
                    .weight(count.toFloat())
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        visible.forEach { (label, count, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$label ${(count * 100f / total).let { if (it in 0.01f..0.99f) "<1" else Math.round(it).toString() }}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopScoredRow(top: FavoriteEntity) {
    val media = remember(top) { top.toRemoteMedia() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(top, Modifier.size(88.dp).clip(ShapeTokens.Large))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = top.score.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = media.tagList.take(4).joinToString(", ") { prettyTag(it) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = top.source,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
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
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun categoryTint(@Suppress("UNUSED_PARAMETER") category: TagCategory): Color = MaterialTheme.colorScheme.primary

private fun prettyTag(tag: String) = tag.replace('_', ' ')

fun statsTitle(lang: AppLanguage) =
    Strings.tr(lang, "Your stats", "Ваша статистика", "あなたの統計", "你的统计", "내 통계", "إحصائياتك")

fun statsSubtitle(lang: AppLanguage) = Strings.tr(
    lang,
    "Favorite characters, artists, tags and activity",
    "Любимые персонажи, авторы, теги и активность",
    "推しキャラ、絵師、タグ、アクティビティ",
    "最爱角色、画师、标签和活跃度",
    "최애 캐릭터, 작가, 태그와 활동",
    "الشخصيات والفنانون والوسوم والنشاط"
)

private fun periodLabel(period: StatsPeriod, lang: AppLanguage) = when (period) {
    StatsPeriod.ALL_TIME -> Strings.tr(lang, "All time", "Всё время", "全期間", "全部", "전체 기간", "كل الوقت")
    StatsPeriod.YEAR -> Strings.tr(lang, "This year", "Этот год", "今年", "今年", "올해", "هذا العام")
    StatsPeriod.MONTH -> Strings.tr(lang, "This month", "Этот месяц", "今月", "本月", "이번 달", "هذا الشهر")
}

private fun daysLabel(days: Int, lang: AppLanguage) =
    Strings.tr(lang, "$days days", "$days дн.", "${days}日", "$days 天", "${days}일", "$days يوم")

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsPeriodSheet(
    current: StatsPeriod,
    lang: AppLanguage,
    onSelect: (StatsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberExpandedSheetState()
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = ShapeTokens.Medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Strings.tr(lang, "Period", "Период", "期間", "时间范围", "기간", "الفترة"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val periods = StatsPeriod.entries
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    periods.forEachIndexed { index, item ->
                        val selected = item == current
                        Surface(
                            onClick = { onSelect(item) },
                            shape = segmentShape(index, periods.size),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = periodLabel(item, lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
