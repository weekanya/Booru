package com.booru.app

enum class Rating(val code: String) {
    SAFE("s"),
    QUESTIONABLE("q"),
    EXPLICIT("e"),
    UNKNOWN("u");

    companion object {
        fun fromString(value: String?): Rating {
            val v = value?.trim()?.lowercase() ?: return UNKNOWN
            return when (v) {
                "s", "safe", "g", "general" -> SAFE
                "q", "questionable", "sensitive" -> QUESTIONABLE
                "e", "explicit" -> EXPLICIT
                else -> UNKNOWN
            }
        }

        fun fromCode(code: String?): Rating = fromString(code)
    }
}

data class RemoteMedia(
    val url: String,
    val preview: String,
    val sample: String = "",
    val tags: String,
    val score: Int,
    val source: String,
    val rating: String = "u",
    val id: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = 0L
) {
    val mediaKey: String
        get() = if (id.isNotBlank()) "${source.lowercase().trim()}_$id" else url

    val ratingType: Rating
        get() = Rating.fromString(rating)

    val isSafe: Boolean
        get() = ratingType == Rating.SAFE

    val isQuestionable: Boolean
        get() = ratingType == Rating.QUESTIONABLE

    val isExplicit: Boolean
        get() = ratingType == Rating.EXPLICIT

    val isUnknownRating: Boolean
        get() = ratingType == Rating.UNKNOWN

    val tagList: List<String> by lazy {
        tags.split(Regex("[\\s,]+"))
            .map { it.trim().removeSuffix(",").removePrefix(",").trim() }
            .filter { it.isNotBlank() }
    }

    val isVideo: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".mkv") || clean.endsWith(".mov")
        }

    val isGif: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            val cleanPreview = preview.substringBefore("?").lowercase()
            val cleanSample = sample.substringBefore("?").lowercase()
            return clean.endsWith(".gif") ||
                cleanPreview.endsWith(".gif") ||
                cleanSample.endsWith(".gif") ||
                (!isVideo && tagList.any { it.equals("gif", ignoreCase = true) || it.equals("animated_gif", ignoreCase = true) })
        }

    val postWebUrl: String
        get() = when (source.lowercase()) {
            "realbooru" -> if (id.isNotBlank()) "https://realbooru.com/index.php?page=post&s=view&id=$id" else url
            "rule34" -> if (id.isNotBlank()) "https://rule34.xxx/index.php?page=post&s=view&id=$id" else url
            "gelbooru" -> if (id.isNotBlank()) "https://gelbooru.com/index.php?page=post&s=view&id=$id" else url
            "safebooru" -> if (id.isNotBlank()) "https://safebooru.org/index.php?page=post&s=view&id=$id" else url
            "xbooru" -> if (id.isNotBlank()) "https://xbooru.com/index.php?page=post&s=view&id=$id" else url
            "tbib" -> if (id.isNotBlank()) "https://tbib.org/index.php?page=post&s=view&id=$id" else url
            "yande" -> if (id.isNotBlank()) "https://yande.re/post/show/$id" else url
            "konachan" -> if (id.isNotBlank()) "https://konachan.net/post/show/$id" else url
            else -> url
        }
}
