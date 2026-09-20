package com.booru.app.data.parser

import com.booru.app.Rating
import com.booru.app.RemoteMedia
import com.booru.app.data.AiFilter
import org.jsoup.Jsoup

object RealbooruHtmlParser {

    fun parse(html: String, noAi: Boolean): List<RemoteMedia> {
        if (html.isBlank()) return emptyList()

        val doc = Jsoup.parse(html)
        val thumbElements = doc.select(".thumb")
        val results = mutableListOf<RemoteMedia>()

        for (thumb in thumbElements) {
            val link = thumb.selectFirst("a") ?: continue
            val img = thumb.selectFirst("img") ?: continue

            val href = link.attr("href")
            val id = href.substringAfter("id=").substringBefore("&").trim()
            if (id.isBlank()) continue

            val thumbSrc = img.attr("src").ifBlank { img.attr("data-src") }
            if (thumbSrc.isBlank()) continue

            val previewUrl = when {
                thumbSrc.startsWith("//") -> "https:$thumbSrc"
                thumbSrc.startsWith("/") -> "https://realbooru.com$thumbSrc"
                !thumbSrc.startsWith("http") -> "https://realbooru.com/$thumbSrc"
                else -> thumbSrc
            }

            val rawTags = img.attr("title").ifBlank { img.attr("alt") }.trim()

            val tagTokens = if (rawTags.contains(",")) {
                rawTags.split(",")
                    .map { it.trim().replace("\\s+".toRegex(), "_").removeSuffix(",").removePrefix(",").trim() }
                    .filter { it.isNotBlank() }
            } else {
                rawTags.split("\\s+".toRegex())
                    .map { it.trim().removeSuffix(",").removePrefix(",").trim() }
                    .filter { it.isNotBlank() }
            }

            val tags = tagTokens.joinToString(" ")

            if (noAi && AiFilter.isAiGeneratedPost(tags)) {
                continue
            }

            val thumbClass = thumb.className().lowercase()
            val linkClass = link.className().lowercase()
            val imgClass = img.className().lowercase()
            val dataType = thumb.attr("data-type").lowercase()

            val isGif = previewUrl.endsWith(".gif", ignoreCase = true) ||
                    tagTokens.any { t ->
                        val l = t.lowercase()
                        l == "gif" || l == "animated_gif"
                    }

            val isVideo = !isGif && (
                    dataType == "video" ||
                    thumbClass.contains("video") ||
                    thumbClass.contains("webm") ||
                    linkClass.contains("video") ||
                    imgClass.contains("video") ||
                    previewUrl.endsWith(".webm", ignoreCase = true) ||
                    previewUrl.endsWith(".mp4", ignoreCase = true) ||
                    tagTokens.any { t ->
                        val l = t.lowercase()
                        l == "video" || l == "webm" || l == "mp4"
                    }
            )

            val originalUrl = if (isVideo) {
                previewUrl
                    .replace("/thumbnails/", "/images/")
                    .replace("/thumbnail_", "/")
                    .replace(Regex("\\.[a-zA-Z0-9]+$"), ".mp4")
            } else if (isGif) {
                previewUrl
                    .replace("/thumbnails/", "/images/")
                    .replace("/thumbnail_", "/")
                    .replace(Regex("\\.[a-zA-Z0-9]+$"), ".gif")
            } else {
                previewUrl
                    .replace("/thumbnails/", "/images/")
                    .replace("/thumbnail_", "/")
            }

            val sampleUrl = if (isVideo) {
                previewUrl
                    .replace("/thumbnails/", "/images/")
                    .replace("/thumbnail_", "/")
                    .replace(Regex("\\.[a-zA-Z0-9]+$"), ".jpg")
            } else {
                previewUrl
                    .replace("/thumbnails/", "/samples/")
                    .replace("/thumbnail_", "/sample_")
            }

            val scoreAttr = thumb.attr("data-score").toIntOrNull() ?: 0
            val dateRaw = thumb.attr("data-posted").ifBlank { thumb.attr("data-time") }
            val createdAt = TimestampParser.parseToEpochSeconds(dateRaw)

            val parsedRating = extractRating(tagTokens, thumb.attr("data-rating"))

            results.add(
                RemoteMedia(
                    id = id,
                    url = originalUrl,
                    preview = previewUrl,
                    sample = sampleUrl,
                    tags = tags,
                    score = scoreAttr,
                    source = "Realbooru",
                    rating = parsedRating.code,
                    createdAt = createdAt
                )
            )
        }

        return results
    }

    private fun extractRating(tagTokens: List<String>, dataRating: String): Rating {
        if (dataRating.isNotBlank()) {
            val fromData = Rating.fromString(dataRating)
            if (fromData != Rating.UNKNOWN) return fromData
        }
        for (token in tagTokens) {
            val lower = token.lowercase()
            when {
                lower == "rating:s" || lower == "rating:safe" || lower == "rating:g" || lower == "rating:general" -> return Rating.SAFE
                lower == "rating:q" || lower == "rating:questionable" || lower == "rating:sensitive" -> return Rating.QUESTIONABLE
                lower == "rating:e" || lower == "rating:explicit" -> return Rating.EXPLICIT
            }
        }
        return Rating.UNKNOWN
    }
}
