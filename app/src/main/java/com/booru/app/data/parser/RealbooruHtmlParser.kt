package com.booru.app.data.parser

import com.booru.app.RemoteMedia
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

            val previewUrl = if (thumbSrc.startsWith("//")) {
                "https:$thumbSrc"
            } else if (thumbSrc.startsWith("/")) {
                "https://realbooru.com$thumbSrc"
            } else if (!thumbSrc.startsWith("http")) {
                "https://realbooru.com/$thumbSrc"
            } else {
                thumbSrc
            }

            val tags = img.attr("title").ifBlank { img.attr("alt") }.trim()

            if (noAi && (tags.contains("ai_generated", ignoreCase = true) || tags.contains("novelai", ignoreCase = true))) {
                continue
            }

            val imgStyle = img.attr("style")
            val isVideo = imgStyle.contains("0000ff", ignoreCase = true) ||
                    imgStyle.contains("blue", ignoreCase = true) ||
                    tags.contains("webm", ignoreCase = true) ||
                    tags.contains("mp4", ignoreCase = true) ||
                    tags.contains("video", ignoreCase = true)

            val originalUrl = if (isVideo) {
                previewUrl
                    .replace("/thumbnails/", "/images/")
                    .replace("/thumbnail_", "/")
                    .replace(Regex("\\.[a-zA-Z0-9]+$"), ".mp4")
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

            results.add(
                RemoteMedia(
                    id = id,
                    url = originalUrl,
                    preview = previewUrl,
                    sample = sampleUrl,
                    tags = tags,
                    score = scoreAttr,
                    source = "Realbooru",
                    rating = "e",
                    createdAt = createdAt
                )
            )
        }

        return results
    }
}
