package com.booru.app

data class RemoteMedia(
    val url: String,
    val preview: String,
    val sample: String = "",
    val tags: String,
    val score: Int,
    val source: String,
    val rating: String,
    val id: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = 0L
) {
    val tagList: List<String> by lazy {
        tags.split(" ").map { it.trim() }.filter { it.isNotBlank() }
    }

    val isVideo: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".mkv") || clean.endsWith(".mov")
        }

    val isGif: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            return clean.endsWith(".gif")
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
