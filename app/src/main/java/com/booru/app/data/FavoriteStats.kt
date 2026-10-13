package com.booru.app.data

import com.booru.app.Rating
import com.booru.app.RemoteMedia
import com.booru.app.data.db.FavoriteEntity
import com.booru.app.data.db.HistoryEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class StatsPeriod { DAY, WEEK, MONTH, ALL_TIME }

enum class BucketSize { HOUR, DAY, MONTH }

data class PostRef(
    val mediaKey: String,
    val url: String,
    val preview: String,
    val tags: String,
    val source: String,
    val rating: String,
    val score: Int
) {
    fun toRemoteMedia() = RemoteMedia(url = url, preview = preview, tags = tags, score = score, source = source, rating = rating)

    companion object {
        fun of(f: FavoriteEntity) = PostRef(f.mediaKey, f.url, f.preview.ifBlank { f.sample }, f.tags, f.source, f.rating, f.score)
        fun of(h: HistoryEntity) = PostRef(h.mediaKey, h.url, h.preview, h.tags, h.source, h.rating, h.score)
    }
}

data class CountEntry(val label: String, val count: Int, val sample: PostRef? = null)

data class ActivityBucket(val start: java.time.LocalDateTime, val views: Int, val favorites: Int)

data class FavoriteStats(
    val viewed: Int,
    val favorited: Int,
    val downloaded: Int,
    val searches: Int,
    val likeRate: Int?,
    val prevViewed: Int?,
    val prevFavorited: Int?,
    val images: Int,
    val gifs: Int,
    val videos: Int,
    val safe: Int,
    val questionable: Int,
    val explicit: Int,
    val topTags: List<CountEntry>,
    val topCharacters: List<CountEntry>,
    val topCopyrights: List<CountEntry>,
    val topArtists: List<CountEntry>,
    val topSources: List<CountEntry>,
    val topSearches: List<CountEntry>,
    val buckets: List<ActivityBucket>,
    val bucketSize: BucketSize,
    val hours: List<Int>,
    val busiestDay: DayOfWeek?,
    val longestStreak: Int,
    val activeDays: Int,
    val firstActivityAt: Long?,
    val historySince: Long?,
    val topScored: PostRef?
) {
    val isEmpty: Boolean get() = viewed == 0 && favorited == 0 && downloaded == 0 && searches == 0

    companion object {
        private const val TOP_LIMIT = 12
        private val TAG_SPLIT = Regex("[\\s,]+")

        private fun splitTags(tags: String) = tags.split(TAG_SPLIT).map { it.trim().lowercase() }.filter { it.isNotBlank() }

        fun allTags(favorites: List<FavoriteEntity>, history: List<HistoryEntity> = emptyList()): Set<String> {
            val result = HashSet<String>()
            favorites.forEach { result.addAll(splitTags(it.tags)) }
            history.forEach { if (it.kind != HistoryEntity.KIND_SEARCH) result.addAll(splitTags(it.tags)) }
            return result
        }

        fun compute(
            favorites: List<FavoriteEntity>,
            history: List<HistoryEntity>,
            period: StatsPeriod,
            zone: ZoneId = ZoneId.systemDefault(),
            today: LocalDate = LocalDate.now(zone)
        ): FavoriteStats {
            val spanDays = when (period) {
                StatsPeriod.DAY -> 1L
                StatsPeriod.WEEK -> 7L
                StatsPeriod.MONTH -> 30L
                StatsPeriod.ALL_TIME -> 0L
            }
            val fromMillis = if (spanDays == 0L) null else today.minusDays(spanDays - 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevFromMillis = if (spanDays == 0L) null else today.minusDays(spanDays * 2 - 1).atStartOfDay(zone).toInstant().toEpochMilli()
            fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            fun inPeriod(millis: Long) = fromMillis == null || millis >= fromMillis
            fun inPrevious(millis: Long) = fromMillis != null && prevFromMillis != null && millis >= prevFromMillis && millis < fromMillis
            val prevViewed = if (fromMillis == null) null else history.filter { it.kind == HistoryEntity.KIND_VIEW && inPrevious(it.at) }.mapTo(HashSet()) { it.mediaKey }.size
            val prevFavorited = if (fromMillis == null) null else favorites.count { inPrevious(it.savedAt) }

            val favs = favorites.filter { inPeriod(it.savedAt) }.sortedByDescending { it.savedAt }
            val events = history.filter { inPeriod(it.at) }
            val views = events.filter { it.kind == HistoryEntity.KIND_VIEW }
            val downloads = events.filter { it.kind == HistoryEntity.KIND_DOWNLOAD }
            val searches = events.filter { it.kind == HistoryEntity.KIND_SEARCH && it.query.isNotBlank() }

            val posts = LinkedHashMap<String, PostRef>()
            favs.forEach { posts.putIfAbsent(it.mediaKey, PostRef.of(it)) }
            (views + downloads).forEach { if (it.mediaKey.isNotBlank()) posts.putIfAbsent(it.mediaKey, PostRef.of(it)) }

            val viewedKeys = views.mapTo(HashSet()) { it.mediaKey }
            val favoriteKeys = favorites.mapTo(HashSet()) { it.mediaKey }
            val likeRate = if (viewedKeys.size >= 5) {
                Math.round(viewedKeys.count { it in favoriteKeys } * 100f / viewedKeys.size)
            } else null

            val general = Counter()
            val characters = Counter()
            val copyrights = Counter()
            val artists = Counter()
            val sources = Counter()
            for (post in posts.values) {
                sources.add(post.source.ifBlank { "?" }, post)
                for (tag in splitTags(post.tags).toSet()) {
                    if (!TagClassifier.isRecommendationCandidate(tag)) continue
                    when (TagClassifier.classify(tag).category) {
                        TagCategory.CHARACTER -> characters.add(tag, post)
                        TagCategory.COPYRIGHT -> copyrights.add(tag, post)
                        TagCategory.ARTIST -> artists.add(tag, post)
                        TagCategory.GENERAL -> general.add(tag, post)
                        TagCategory.META -> Unit
                    }
                }
            }
            val searchCounter = Counter()
            searches.forEach { searchCounter.add(it.query.trim().lowercase(), null) }

            val media = posts.values.map { it.toRemoteMedia() }

            val bucketSize = when (period) {
                StatsPeriod.DAY -> BucketSize.HOUR
                StatsPeriod.WEEK, StatsPeriod.MONTH -> BucketSize.DAY
                StatsPeriod.ALL_TIME -> BucketSize.MONTH
            }
            fun bucketOf(millis: Long): java.time.LocalDateTime {
                val t = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
                return when (bucketSize) {
                    BucketSize.HOUR -> t.withMinute(0).withSecond(0).withNano(0)
                    BucketSize.DAY -> t.toLocalDate().atStartOfDay()
                    BucketSize.MONTH -> t.toLocalDate().withDayOfMonth(1).atStartOfDay()
                }
            }
            val bucketStarts = when (period) {
                StatsPeriod.DAY -> (0..23).map { today.atTime(it, 0) }
                StatsPeriod.WEEK, StatsPeriod.MONTH -> (spanDays - 1 downTo 0).map { today.minusDays(it).atStartOfDay() }
                StatsPeriod.ALL_TIME -> (11 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()).atStartOfDay() }
            }
            val viewsPerBucket = views.groupingBy { bucketOf(it.at) }.eachCount()
            val favsPerBucket = favs.groupingBy { bucketOf(it.savedAt) }.eachCount()
            val buckets = bucketStarts.map { ActivityBucket(it, viewsPerBucket[it] ?: 0, favsPerBucket[it] ?: 0) }

            val activityTimes = buildList {
                favs.forEach { add(it.savedAt) }
                events.forEach { add(it.at) }
            }
            val hours = MutableList(24) { 0 }
            activityTimes.forEach { hours[ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), zone).hour]++ }

            val days = activityTimes.map { dateOf(it) }.distinct().sortedBy { it.toEpochDay() }
            var longest = 0
            var current = 0
            var previous: LocalDate? = null
            for (day in days) {
                current = if (previous != null && previous.plusDays(1) == day) current + 1 else 1
                longest = maxOf(longest, current)
                previous = day
            }

            return FavoriteStats(
                viewed = viewedKeys.size,
                favorited = favs.size,
                downloaded = downloads.mapTo(HashSet()) { it.mediaKey }.size,
                searches = searches.size,
                likeRate = likeRate,
                prevViewed = prevViewed,
                prevFavorited = prevFavorited,
                images = media.count { !it.isVideo && !it.isGif },
                gifs = media.count { it.isGif },
                videos = media.count { it.isVideo },
                safe = media.count { it.ratingType == Rating.SAFE },
                questionable = media.count { it.ratingType == Rating.QUESTIONABLE },
                explicit = media.count { it.ratingType == Rating.EXPLICIT },
                topTags = general.top(),
                topCharacters = characters.top(),
                topCopyrights = copyrights.top(),
                topArtists = artists.top(),
                topSources = sources.top(),
                topSearches = searchCounter.top(),
                buckets = buckets,
                bucketSize = bucketSize,
                hours = hours,
                busiestDay = if (days.size < 3) null else activityTimes.groupingBy { dateOf(it).dayOfWeek }.eachCount().maxByOrNull { it.value }?.key,
                longestStreak = longest,
                activeDays = days.size,
                firstActivityAt = activityTimes.minOrNull(),
                historySince = history.minOfOrNull { it.at },
                topScored = posts.values.filter { it.score > 0 }.maxByOrNull { it.score }
            )
        }
    }

    private class Counter {
        private val counts = HashMap<String, Int>()
        private val samples = HashMap<String, PostRef>()

        fun add(key: String, post: PostRef?) {
            counts[key] = (counts[key] ?: 0) + 1
            if (post != null && key !in samples) samples[key] = post
        }

        fun top(): List<CountEntry> = counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(TOP_LIMIT)
            .map { CountEntry(it.key, it.value, samples[it.key]) }
    }
}
