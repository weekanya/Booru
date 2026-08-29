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

    fun parseToEpochSeconds(raw: Any?): Long {
        if (raw == null) return 0L

        when (raw) {
            is Number -> {
                return normalizeNumericTimestamp(raw.toLong())
            }
            is String -> {
                val text = raw.trim()
                if (text.isBlank()) return 0L

                val numeric = text.toLongOrNull()
                if (numeric != null) {
                    return normalizeNumericTimestamp(numeric)
                }

                try {
                    return Instant.parse(text).epochSecond
                } catch (_: Exception) { }

                try {
                    return OffsetDateTime.parse(text).toEpochSecond()
                } catch (_: Exception) { }

                try {
                    return ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
                } catch (_: Exception) { }

                try {
                    return LocalDateTime.parse(text, FORMATTER_DATETIME_SPACE).toEpochSecond(ZoneOffset.UTC)
                } catch (_: Exception) { }

                try {
                    return ZonedDateTime.parse(text, FORMATTER_RUBY_DATE).toEpochSecond()
                } catch (_: Exception) { }

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

        return if (num > 100_000_000_000L) {
            num / 1000L
        } else {
            num
        }
    }
}
