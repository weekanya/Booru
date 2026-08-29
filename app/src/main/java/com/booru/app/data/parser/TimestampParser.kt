package com.booru.app.data.parser

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimestampParser {

    private val FORMATTER_DATETIME_SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val FORMATTER_RUBY_DATE = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.US)

    /**
     * Parses an arbitrary timestamp representation (Unix seconds, Unix milliseconds, ISO-8601,
     * RFC-1123, local date, or local datetime) and returns normalized Unix epoch seconds.
     *
     * Returns 0L if the input is null, empty, or unparseable.
     * Never uses Post ID as a timestamp.
     */
    fun parseToEpochSeconds(raw: Any?): Long {
        if (raw == null) return 0L

        when (raw) {
            is Number -> {
                return normalizeNumericTimestamp(raw.toLong())
            }
            is String -> {
                val text = raw.trim()
                if (text.isBlank()) return 0L

                // 1. Numeric timestamp in seconds or milliseconds
                val numeric = text.toLongOrNull()
                if (numeric != null) {
                    return normalizeNumericTimestamp(numeric)
                }

                // 2. ISO-8601 Instant (e.g. "2026-08-29T14:32:10Z", "2026-08-29T14:32:10.000Z")
                try {
                    return Instant.parse(text).epochSecond
                } catch (_: Exception) { }

                // 3. ISO-8601 with timezone offset (e.g. "2026-08-29T14:32:10+03:00")
                try {
                    return OffsetDateTime.parse(text).toEpochSecond()
                } catch (_: Exception) { }

                // 4. RFC-1123 HTTP Date (e.g. "Wed, 21 Oct 2026 07:28:00 GMT")
                try {
                    return ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
                } catch (_: Exception) { }

                // 5. Standard date with space (e.g. "2026-08-29 14:32:10")
                try {
                    return LocalDateTime.parse(text, FORMATTER_DATETIME_SPACE).toEpochSecond(ZoneOffset.UTC)
                } catch (_: Exception) { }

                // 6. Ruby/Ctime style date (e.g. "Thu May 12 15:23:01 -0500 2022")
                try {
                    return ZonedDateTime.parse(text, FORMATTER_RUBY_DATE).toEpochSecond()
                } catch (_: Exception) { }

                // 7. Date only (e.g. "2026-08-29")
                try {
                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                } catch (_: Exception) { }

                return 0L
            }
            else -> return 0L
        }
    }

    private fun normalizeNumericTimestamp(num: Long): Long {
        if (num <= 0L) return 0L
        // Values > 100,000,000,000 are in milliseconds (past year 5138 in seconds)
        return if (num > 100_000_000_000L) {
            num / 1000L
        } else {
            num
        }
    }
}
