package com.booru.app.data

object FurryFilter {

    val EXCLUDE_QUERY_TAGS = listOf("-furry")

    private val FURRY_TAGS = setOf(
        "furry",
        "furry_female",
        "furry_male",
        "furry_only",
        "furry_with_furry",
        "furry_with_non-furry",
        "anthro",
        "anthrofied",
        "anthro_only",
        "feral",
        "fursona",
        "fursuit",
        "yiff",
        "scalie",
        "protogen",
        "sergal"
    )

    fun isFurryPost(tags: String): Boolean {
        if (tags.isBlank()) return false
        return tags.split(Regex("[\\s,]+")).any { isFurryTag(it) }
    }

    fun isFurryTag(rawTag: String): Boolean {
        val clean = rawTag.trim().lowercase()
        if (clean.isBlank()) return false
        return clean in FURRY_TAGS || clean.startsWith("furry_") || clean.startsWith("anthro_")
    }
}
