package com.booru.app.data

import com.booru.app.Rating
import com.booru.app.data.db.FavoriteEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class StatsPeriod { ALL_TIME, YEAR, MONTH }

data class CountEntry(val label: String, val count: Int)

data class FavoriteStats(
    val total: Int,
    val images: Int,
    val gifs: Int,
    val videos: Int,
    val safe: Int,
    val questionable: Int,
    val explicit: Int,
    val topTags: List<CountEntry>,
    val topCharacters: List<CountEntry>,
    val topCopyrights: List<CountEntry>,
    val topSources: List<CountEntry>,
    val monthly: List<Pair<YearMonth, Int>>,
    val busiestDay: DayOfWeek?,
    val longestStreak: Int,
    val firstSavedAt: Long?,
    val topScored: FavoriteEntity?
) {
    val isEmpty: Boolean get() = total == 0

    companion object {
        private const val TOP_LIMIT = 10
        private val TAG_SPLIT = Regex("[\\s,]+")

        fun compute(
            favorites: List<FavoriteEntity>,
            period: StatsPeriod,
            zone: ZoneId = ZoneId.systemDefault(),
            today: LocalDate = LocalDate.now(zone)
        ): FavoriteStats {
            val from = when (period) {
                StatsPeriod.ALL_TIME -> null
                StatsPeriod.YEAR -> today.withDayOfYear(1)
                StatsPeriod.MONTH -> today.withDayOfMonth(1)
            }
            fun dateOf(f: FavoriteEntity): LocalDate = Instant.ofEpochMilli(f.savedAt).atZone(zone).toLocalDate()

            val items = if (from == null) favorites else favorites.filter { !dateOf(it).isBefore(from) }
            val media = items.map { it.toRemoteMedia() }

            val tagCounts = HashMap<String, Int>()
            val characterCounts = HashMap<String, Int>()
            val copyrightCounts = HashMap<String, Int>()
            for (f in items) {
                val tags = f.tags.split(TAG_SPLIT).map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                for (tag in tags) {
                    if (!TagClassifier.isRecommendationCandidate(tag)) continue
                    when (TagClassifier.classify(tag).category) {
                        TagCategory.CHARACTER -> characterCounts.merge(tag, 1) { a, b -> a + b }
                        TagCategory.COPYRIGHT -> copyrightCounts.merge(tag, 1) { a, b -> a + b }
                        TagCategory.GENERAL -> tagCounts.merge(tag, 1) { a, b -> a + b }
                        else -> Unit
                    }
                }
            }

            val sourceCounts = items.groupingBy { it.source.ifBlank { "?" } }.eachCount()

            val months = when (period) {
                StatsPeriod.MONTH -> 1
                StatsPeriod.YEAR -> today.monthValue
                StatsPeriod.ALL_TIME -> 12
            }
            val currentMonth = YearMonth.from(today)
            val perMonth = items.groupingBy { YearMonth.from(dateOf(it)) }.eachCount()
            val monthly = (months - 1 downTo 0).map { back ->
                val ym = currentMonth.minusMonths(back.toLong())
                ym to (perMonth[ym] ?: 0)
            }

            val days = items.map { dateOf(it) }.distinct().sortedBy { it.toEpochDay() }
            var longest = 0
            var current = 0
            var previous: LocalDate? = null
            for (day in days) {
                current = if (previous != null && previous.plusDays(1) == day) current + 1 else 1
                longest = maxOf(longest, current)
                previous = day
            }

            return FavoriteStats(
                total = items.size,
                images = media.count { !it.isVideo && !it.isGif },
                gifs = media.count { it.isGif },
                videos = media.count { it.isVideo },
                safe = media.count { it.ratingType == Rating.SAFE },
                questionable = media.count { it.ratingType == Rating.QUESTIONABLE },
                explicit = media.count { it.ratingType == Rating.EXPLICIT },
                topTags = tagCounts.top(),
                topCharacters = characterCounts.top(),
                topCopyrights = copyrightCounts.top(),
                topSources = sourceCounts.top(),
                monthly = monthly,
                busiestDay = items.groupingBy { dateOf(it).dayOfWeek }.eachCount().maxByOrNull { it.value }?.key,
                longestStreak = longest,
                firstSavedAt = items.minOfOrNull { it.savedAt },
                topScored = items.filter { it.score > 0 }.maxByOrNull { it.score }
            )
        }

        private fun Map<String, Int>.top(): List<CountEntry> =
            entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(TOP_LIMIT)
                .map { CountEntry(it.key, it.value) }
    }
}
