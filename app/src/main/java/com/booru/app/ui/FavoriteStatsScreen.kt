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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
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
import com.booru.app.data.db.HistoryEntity
import com.booru.app.data.PostRef
import com.booru.app.data.BucketSize
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
    interests: List<Pair<String, Float>>,
    onClearHistory: () -> Unit,
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
            FavoriteStatsContent(lang, allowTagLookup, interests, onClearHistory, onDismiss)
        }
    }
}

@Composable
private fun FavoriteStatsContent(lang: AppLanguage, allowTagLookup: Boolean, interests: List<Pair<String, Float>>, onClearHistory: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = remember(lang) { Locale.forLanguageTag(lang.code) }

    var favorites by remember { mutableStateOf<List<FavoriteEntity>?>(null) }
    var history by remember { mutableStateOf<List<HistoryEntity>>(emptyList()) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.WEEK) }
    var lookupProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var lookupAttempt by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text(Strings.tr(lang, "Clear history?", "Очистить историю?", "履歴を消去しますか？", "清除历史？", "기록을 지울까요?", "مسح السجل؟")) },
            text = {
                Text(
                    Strings.tr(
                        lang,
                        "Views, downloads and searches will be removed from stats. Favorites stay.",
                        "Просмотры, скачивания и поиски пропадут из статистики. Избранное останется.",
                        "閲覧・保存・検索の記録を統計から削除します。お気に入りは残ります。",
                        "浏览、下载和搜索记录将从统计中删除，收藏会保留。",
                        "보기, 다운로드, 검색 기록이 통계에서 지워집니다. 즐겨찾기는 유지됩니다.",
                        "ستُحذف المشاهدات والتنزيلات وعمليات البحث من الإحصائيات. تبقى المفضلة."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                    history = emptyList()
                }) {
                    Text(Strings.tr(lang, "Clear", "Очистить", "消去", "清除", "지우기", "مسح"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(Strings.tr(lang, "Cancel", "Отмена", "キャンセル", "取消", "취소", "إلغاء"))
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val (favs, events) = withContext(Dispatchers.IO) {
            db.favoriteDao().getAllFavoritesList() to runCatching { db.historyDao().since(0L) }.getOrDefault(emptyList())
        }
        TagCategoryCache.ensureLoaded()
        history = events
        favorites = favs
    }

    LaunchedEffect(favorites, lookupAttempt) {
        val list = favorites ?: return@LaunchedEffect
        val tags = FavoriteStats.allTags(list, history)
        if (!allowTagLookup || tags.isEmpty()) return@LaunchedEffect
        lookupProgress = 0 to 1
        try {
            TagCategoryCache.resolve(tags) { done, total ->
                lookupProgress = done to total
            }
        } finally {
            lookupProgress = null
        }
    }

    val cacheVersion = TagCategoryCache.version
    val stats by produceState<FavoriteStats?>(null, favorites, history, period, cacheVersion) {
        val list = favorites ?: return@produceState
        value = withContext(Dispatchers.Default) { FavoriteStats.compute(list, history, period) }
    }
    val tabs = listOf(
        Strings.tr(lang, "Overview", "Обзор", "概要", "概览", "개요", "نظرة عامة") to Icons.Rounded.Insights,
        Strings.tr(lang, "Interests", "Интересы", "好み", "兴趣", "관심사", "الاهتمامات") to Icons.Rounded.FavoriteBorder,
        Strings.tr(lang, "Activity", "Активность", "アクティビティ", "活跃度", "활동", "النشاط") to Icons.Rounded.BarChart
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val tabController = rememberTabPagerController(pagerState)

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
            }

            ToggleGroupColors {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .readableContentWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    val periods = StatsPeriod.entries
                    periods.forEachIndexed { index, item ->
                        ToggleButton(
                            checked = period == item,
                            onCheckedChange = { period = item },
                            shapes = groupPosition(index, periods.size).toggleShapes(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics { role = Role.RadioButton }
                        ) {
                            Text(
                                periodShortLabel(item, lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (period == item) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
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

            AnimatedVisibility(
                visible = allowTagLookup && lookupProgress == null && TagCategoryCache.lookupFailed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = Strings.tr(
                                lang,
                                "Couldn't load tag categories, so characters, artists and series may be missing.",
                                "Не удалось загрузить категории тегов, поэтому персонажи, авторы и франшизы могут не отображаться.",
                                "タグの分類を読み込めませんでした。キャラクター・作者・作品が表示されない場合があります。",
                                "无法加载标签分类，角色、画师和作品可能无法显示。",
                                "태그 분류를 불러오지 못해 캐릭터, 작가, 작품이 표시되지 않을 수 있습니다.",
                                "تعذّر تحميل تصنيفات الوسوم، لذا قد لا تظهر الشخصيات والفنانون والأعمال."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            TagCategoryCache.clearBackoff()
                            lookupAttempt++
                        }) {
                            Text(Strings.tr(lang, "Retry", "Повторить", "再試行", "重试", "다시 시도", "إعادة المحاولة"))
                        }
                    }
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
                        modifier = Modifier.fillMaxSize().tabTransition(tabController)
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
                                    1 -> InterestsPage(current, interests, lang)
                                    else -> {
                                        ActivityPage(current, lang, locale)
                                        ClearHistoryCard(history.size, lang) { confirmClear = true }
                                    }
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
private fun InterestsPage(stats: FavoriteStats, interests: List<Pair<String, Float>>, lang: AppLanguage) {
    val groups = listOf(
        Triple(TagCategory.CHARACTER, Strings.tr(lang, "Characters", "Персонажи", "キャラクター", "角色", "캐릭터", "الشخصيات"), stats.topCharacters),
        Triple(TagCategory.ARTIST, Strings.tr(lang, "Artists", "Авторы", "絵師", "画师", "작가", "الفنانون"), stats.topArtists),
        Triple(TagCategory.COPYRIGHT, Strings.tr(lang, "Series", "Франшизы", "作品", "作品", "작품", "الأعمال"), stats.topCopyrights)
    ).filter { it.third.isNotEmpty() }

    if (interests.isNotEmpty()) {
        StatsCard(
            Icons.Rounded.AutoAwesome,
            Strings.tr(lang, "Shaping your feed", "Сейчас влияет на ленту", "フィードに影響中", "正在影响推荐", "피드에 반영 중", "يشكّل خلاصتك"),
            Strings.tr(lang, "Tags your recommendations lean on right now", "Теги, на которые сейчас опираются рекомендации", "現在おすすめが重視しているタグ", "推荐当前依赖的标签", "현재 추천에 반영되는 태그", "الوسوم التي تعتمد عليها التوصيات الآن")
        ) {
            InterestBars(interests.take(8))
        }
    }
    if (stats.topTags.isNotEmpty()) {
        StatsCard(
            Icons.Rounded.Tag,
            Strings.tr(lang, "Your vibe", "Ваш вайб", "あなたの好み", "你的口味", "나의 취향", "ذوقك"),
            Strings.tr(lang, "Most common tags in what you view and save", "Самые частые теги в том, что вы смотрите и сохраняете", "閲覧・保存した投稿で多いタグ", "浏览和收藏中最常见的标签", "보고 저장한 게시물에서 많은 태그", "الوسوم الأكثر تكراراً فيما تشاهده وتحفظه")
        ) {
            TagCloud(stats.topTags)
        }
    }
    groups.forEach { (category, title, entries) ->
        StatsCard(
            category.icon,
            title,
            Strings.tr(lang, "Posts you viewed or saved with each one", "Сколько постов с ними вы смотрели или сохранили", "閲覧・保存した投稿数", "浏览或收藏的作品数", "보거나 저장한 게시물 수", "عدد المنشورات التي شاهدتها أو حفظتها")
        ) {
            RankedBars(entries.take(8), MaterialTheme.colorScheme.primary)
        }
    }
    if (stats.topSearches.isNotEmpty()) {
        StatsCard(
            Icons.Rounded.Search,
            Strings.tr(lang, "Top searches", "Частые запросы", "よく使う検索", "常用搜索", "자주 찾은 검색어", "عمليات البحث الأكثر"),
            Strings.tr(lang, "What you search for most", "Что вы ищете чаще всего", "よく検索するキーワード", "你最常搜索的内容", "가장 많이 검색한 항목", "ما تبحث عنه أكثر")
        ) {
            RankedBars(stats.topSearches.take(8), MaterialTheme.colorScheme.tertiary)
        }
    }
    if (stats.topSources.size > 1) {
        StatsCard(
            Icons.Rounded.Public,
            Strings.tr(lang, "Sources", "Источники", "ソース", "来源", "소스", "المصادر"),
            Strings.tr(lang, "Where the posts you like come from", "Откуда посты, которые вы смотрите", "閲覧した投稿の取得元", "你浏览的帖子来自哪里", "본 게시물의 출처", "مصادر المنشورات التي تشاهدها")
        ) {
            RankedBars(stats.topSources.take(5), MaterialTheme.colorScheme.primary, prettify = false)
        }
    }
    if (groups.isEmpty() && stats.topTags.isEmpty() && interests.isEmpty() && stats.topSearches.isEmpty()) EmptyStats(lang)
}

@Composable
private fun ActivityPage(stats: FavoriteStats, lang: AppLanguage, locale: Locale) {
    if (stats.buckets.size > 1) {
        StatsCard(
            Icons.Rounded.BarChart,
            when (stats.bucketSize) {
                BucketSize.HOUR -> Strings.tr(lang, "By hour", "По часам", "時間別", "按小时", "시간별", "حسب الساعة")
                BucketSize.DAY -> Strings.tr(lang, "By day", "По дням", "日別", "按天", "일별", "حسب اليوم")
                BucketSize.MONTH -> Strings.tr(lang, "By month", "По месяцам", "月別", "按月", "월별", "حسب الشهر")
            },
            Strings.tr(lang, "Posts viewed and favorited", "Просмотрено и добавлено в избранное", "閲覧数とお気に入り数", "浏览与收藏", "본 게시물과 즐겨찾기", "المنشورات المشاهدة والمفضلة")
        ) {
            ActivityChart(stats, locale, lang)
        }
    }

    if (stats.bucketSize != BucketSize.HOUR && stats.hours.any { it > 0 }) {
        StatsCard(
            Icons.Rounded.Schedule,
            Strings.tr(lang, "Time of day", "Время суток", "時間帯", "时段", "시간대", "وقت اليوم"),
            Strings.tr(lang, "When you use the app", "Когда вы пользуетесь приложением", "アプリを使う時間", "你使用应用的时间", "앱을 사용하는 시간", "متى تستخدم التطبيق")
        ) {
            HourChart(stats.hours, lang)
        }
    }

    if (stats.images + stats.gifs + stats.videos > 0) {
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
    }

    stats.topScored?.let { top ->
        StatsCard(
            Icons.Rounded.EmojiEvents,
            Strings.tr(lang, "Highest score", "Самый высокий рейтинг", "最高スコア", "最高评分", "최고 점수", "أعلى تقييم"),
            Strings.tr(lang, "The post with the most votes on its site", "Пост с наибольшим рейтингом на сайте", "サイトで最も評価された投稿", "在站点评分最高的帖子", "사이트 점수가 가장 높은 게시물", "المنشور الأعلى تقييماً")
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
                    text = stats.viewed.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = Strings.tr(lang, "posts viewed", "постов просмотрено", "件閲覧", "个帖子已浏览", "개 게시물 봄", "منشور تمت مشاهدته"),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            stats.prevViewed?.let { prev ->
                TrendChip(current = stats.viewed, previous = prev, period = period, lang = lang)
                Spacer(Modifier.height(6.dp))
            }
            stats.likeRate?.let { rate ->
                Text(
                    text = Strings.tr(lang, "You save $rate% of what you open", "Вы сохраняете $rate% того, что открываете", "開いた投稿の $rate% を保存", "你收藏了打开内容的 $rate%", "연 게시물의 $rate%를 저장", "تحفظ $rate% مما تفتحه"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                val items = listOf(
                    Triple(Icons.Rounded.Favorite, stats.favorited, Strings.tr(lang, "Favorited", "В избранное", "お気に入り", "收藏", "즐겨찾기", "مفضلة")),
                    Triple(Icons.Rounded.Download, stats.downloaded, Strings.tr(lang, "Downloaded", "Скачано", "保存", "下载", "다운로드", "تنزيل")),
                    Triple(Icons.Rounded.Search, stats.searches, Strings.tr(lang, "Searches", "Поисков", "検索", "搜索", "검색", "عمليات بحث"))
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
            val since = stats.historySince
            val trackingText = if (since == null) {
                Strings.tr(lang, "Views, downloads and searches are counted from now on.", "Просмотры, скачивания и поиски считаются с этого момента.", "閲覧・保存・検索は今後カウントされます。", "浏览、下载和搜索将从现在开始统计。", "보기, 다운로드, 검색은 지금부터 집계됩니다.", "تُحتسب المشاهدات والتنزيلات وعمليات البحث من الآن.")
            } else {
                val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
                val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
                Strings.tr(lang, "Views, downloads and searches counted since $formatted", "Просмотры, скачивания и поиски считаются с $formatted", "閲覧・保存・検索は $formatted から集計", "浏览、下载和搜索自 $formatted 起统计", "보기, 다운로드, 검색은 ${formatted}부터 집계", "تُحتسب المشاهدات والتنزيلات وعمليات البحث منذ $formatted")
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(trackingText, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendChip(current: Int, previous: Int, period: StatsPeriod, lang: AppLanguage) {
    val scheme = MaterialTheme.colorScheme
    val versus = when (period) {
        StatsPeriod.DAY -> Strings.tr(lang, "vs yesterday", "к вчера", "昨日比", "较昨天", "어제 대비", "مقارنة بالأمس")
        StatsPeriod.WEEK -> Strings.tr(lang, "vs previous week", "к прошлой неделе", "前週比", "较上周", "지난주 대비", "مقارنة بالأسبوع السابق")
        else -> Strings.tr(lang, "vs previous 30 days", "к прошлым 30 дням", "前の30日比", "较前 30 天", "이전 30일 대비", "مقارنة بالثلاثين يومًا السابقة")
    }
    val (icon, text, container, content) = when {
        previous == 0 && current == 0 -> Quad(Icons.AutoMirrored.Rounded.TrendingFlat, "0", scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        previous == 0 -> Quad(Icons.AutoMirrored.Rounded.TrendingUp, Strings.tr(lang, "new", "новое", "新規", "新增", "신규", "جديد"), scheme.primaryContainer, scheme.onPrimaryContainer)
        else -> {
            val delta = Math.round((current - previous) * 100f / previous)
            when {
                delta > 0 -> Quad(Icons.AutoMirrored.Rounded.TrendingUp, "+$delta%", scheme.primaryContainer, scheme.onPrimaryContainer)
                delta < 0 -> Quad(Icons.AutoMirrored.Rounded.TrendingDown, "$delta%", scheme.tertiaryContainer, scheme.onTertiaryContainer)
                else -> Quad(Icons.AutoMirrored.Rounded.TrendingFlat, "0%", scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
            }
        }
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("$text $versus", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

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
                    text = Strings.tr(lang, "${entry.count} posts", "Постов: ${entry.count}", "${entry.count} 件", "${entry.count} 个帖子", "게시물 ${entry.count}개", "${entry.count} منشور"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(post: PostRef?, modifier: Modifier) {
    val context = LocalContext.current
    val url = post?.let { it.preview.ifBlank { it.url } }
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
private fun ClearHistoryCard(entries: Int, lang: AppLanguage, onClear: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = ShapeTokens.ExtraLarge,
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Surface(shape = ShapeTokens.Medium, color = scheme.errorContainer, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = scheme.onErrorContainer, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.tr(lang, "History", "История", "履歴", "历史", "기록", "السجل"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = Strings.tr(
                        lang,
                        "$entries views, downloads and searches",
                        "Просмотры, скачивания и поиски: $entries",
                        "閲覧・保存・検索: $entries",
                        "浏览、下载和搜索：$entries",
                        "보기, 다운로드, 검색: $entries",
                        "المشاهدات والتنزيلات وعمليات البحث: $entries"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = onClear,
                enabled = entries > 0,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer
                ),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Strings.tr(lang, "Clear", "Очистить", "消去", "清除", "지우기", "مسح"), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
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
private fun RankedBars(entries: List<CountEntry>, accent: Color, startRank: Int = 1, prettify: Boolean = true, suffix: String = "") {
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
                            text = "${entry.count}$suffix",
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
    val scheme = MaterialTheme.colorScheme
    val buckets = stats.buckets
    val max = buckets.maxOfOrNull { maxOf(it.views, it.favorites) }?.coerceAtLeast(1) ?: 1
    val peak = buckets.maxByOrNull { it.views + it.favorites }?.takeIf { it.views + it.favorites > 0 }
    val daily = stats.bucketSize != BucketSize.MONTH
    val weekly = stats.bucketSize == BucketSize.DAY && buckets.size <= 7
    fun bucketName(time: java.time.LocalDateTime, style: TextStyle) = when {
        stats.bucketSize == BucketSize.HOUR -> "%02d".format(time.hour)
        weekly -> time.dayOfWeek.getDisplayName(if (style == TextStyle.FULL_STANDALONE) TextStyle.FULL_STANDALONE else TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
        stats.bucketSize == BucketSize.DAY -> time.dayOfMonth.toString()
        else -> time.month.getDisplayName(style, locale).replaceFirstChar { it.titlecase(locale) }
    }
    if (peak != null) {
        val name = bucketName(peak.start, TextStyle.FULL_STANDALONE)
        val total = peak.views + peak.favorites
        Text(
            text = Strings.tr(lang, "Peak: $name · $total", "Пик: $name · $total", "ピーク：$name · $total", "高峰：$name · $total", "최고: $name · $total", "الذروة: $name · $total"),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(if (daily) 2.dp else 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEachIndexed { index, bucket ->
            val viewFraction by animateFloatAsState((bucket.views.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "viewBar")
            val favFraction by animateFloatAsState((bucket.favorites.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "favBar")
            val showLabel = when {
                stats.bucketSize == BucketSize.HOUR -> index % 6 == 0
                weekly || !daily -> true
                else -> index == 0 || (index + 1) % 5 == 0 || index == buckets.lastIndex
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(viewFraction.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (bucket.views > 0) scheme.primary.copy(alpha = 0.35f) else scheme.surfaceContainerHighest)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(favFraction.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (bucket.favorites > 0) scheme.primary else scheme.surfaceContainerHighest)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (showLabel) bucketName(bucket.start, TextStyle.NARROW_STANDALONE) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(scheme.primary.copy(alpha = 0.35f), Strings.tr(lang, "Viewed", "Просмотрено", "閲覧", "浏览", "봄", "مشاهدة"))
        LegendDot(scheme.primary, Strings.tr(lang, "Favorited", "В избранное", "お気に入り", "收藏", "즐겨찾기", "مفضلة"))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourChart(hours: List<Int>, lang: AppLanguage) {
    val scheme = MaterialTheme.colorScheme
    val max = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakHour = hours.indices.maxByOrNull { hours[it] } ?: 0
    val part = when (peakHour) {
        in 5..11 -> Strings.tr(lang, "Mostly in the morning", "Чаще всего утром", "主に朝", "多在早上", "주로 아침", "غالباً في الصباح")
        in 12..17 -> Strings.tr(lang, "Mostly in the afternoon", "Чаще всего днём", "主に昼", "多在下午", "주로 낮", "غالباً بعد الظهر")
        in 18..22 -> Strings.tr(lang, "Mostly in the evening", "Чаще всего вечером", "主に夜", "多在晚上", "주로 저녁", "غالباً في المساء")
        else -> Strings.tr(lang, "Mostly at night", "Чаще всего ночью", "主に深夜", "多在深夜", "주로 밤", "غالباً في الليل")
    }
    Text(
        text = "$part · ${"%02d".format(peakHour)}:00",
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hours.forEachIndexed { hour, count ->
            val fraction by animateFloatAsState((count.toFloat() / max).coerceIn(0f, 1f), Motion.effectsDefault(), label = "hourBar")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(
                        when {
                            hour == peakHour && count > 0 -> scheme.primary
                            count > 0 -> scheme.primary.copy(alpha = 0.4f)
                            else -> scheme.surfaceContainerHighest
                        }
                    )
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("00", "06", "12", "18").forEach { label ->
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InterestBars(interests: List<Pair<String, Float>>) {
    val max = interests.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f
    RankedBars(
        entries = interests.map { (tag, weight) -> CountEntry(tag, Math.round(weight * 100f / max)) },
        accent = MaterialTheme.colorScheme.tertiary,
        suffix = "%"
    )
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
private fun TopScoredRow(top: PostRef) {
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
                "Nothing here for this period yet",
                "За этот период пока ничего нет",
                "この期間のデータはまだありません",
                "此期间暂无数据",
                "이 기간에는 아직 데이터가 없습니다",
                "لا يوجد شيء في هذه الفترة بعد"
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
    "What you view, save and search, and your favorite characters and artists",
    "Что вы смотрите, сохраняете и ищете, любимые персонажи и авторы",
    "閲覧・保存・検索の傾向、推しキャラと絵師",
    "你的浏览、收藏和搜索，以及最爱的角色和画师",
    "보고, 저장하고, 검색한 것과 최애 캐릭터·작가",
    "ما تشاهده وتحفظه وتبحث عنه، وشخصياتك وفنانيك المفضلون"
)

private fun periodShortLabel(period: StatsPeriod, lang: AppLanguage) = when (period) {
    StatsPeriod.DAY -> Strings.tr(lang, "Day", "День", "日", "日", "일", "يوم")
    StatsPeriod.WEEK -> Strings.tr(lang, "Week", "Неделя", "週", "周", "주", "أسبوع")
    StatsPeriod.MONTH -> Strings.tr(lang, "Month", "Месяц", "月", "月", "월", "شهر")
    StatsPeriod.ALL_TIME -> Strings.tr(lang, "All", "Всё", "全期間", "全部", "전체", "الكل")
}

private fun periodLabel(period: StatsPeriod, lang: AppLanguage) = when (period) {
    StatsPeriod.ALL_TIME -> Strings.tr(lang, "All time", "Всё время", "全期間", "全部", "전체 기간", "كل الوقت")
    StatsPeriod.DAY -> Strings.tr(lang, "Today", "Сегодня", "今日", "今天", "오늘", "اليوم")
    StatsPeriod.WEEK -> Strings.tr(lang, "Last 7 days", "Последние 7 дней", "過去7日間", "最近 7 天", "최근 7일", "آخر 7 أيام")
    StatsPeriod.MONTH -> Strings.tr(lang, "Last 30 days", "Последние 30 дней", "過去30日間", "最近 30 天", "최근 30일", "آخر 30 يومًا")
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
