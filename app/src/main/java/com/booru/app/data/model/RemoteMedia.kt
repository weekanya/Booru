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
}
