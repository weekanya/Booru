package com.booru.app.data

object AiFilter {

    val EXCLUDE_QUERY_TAGS = listOf("-ai_generated", "-novelai", "-stable_diffusion")

    val AI_TAG_EXACT = setOf(
        "ai_generated",
        "ai-generated",
        "novelai",
        "novel_ai",
        "novel-ai",
        "stable_diffusion",
        "stablediffusion",
        "stable-diffusion",
        "midjourney",
        "dall-e",
        "dall_e",
        "dalle",
        "synthetic",
        "synthetic_media",
        "synthetic_image",
        "synthetic_art",
        "created_by_ai",
        "created-by-ai",
        "ai_art",
        "ai-art",
        "ai_upscale",
        "ai-upscale",
        "ai_assisted",
        "ai-assisted",
        "deepfake",
        "deep_fake"
    )

    private val AI_COMPOUNDS = AI_TAG_EXACT - "synthetic"

    private val TAG_SPLIT = Regex("[\\s,]+")

    fun isAiGeneratedPost(tags: String): Boolean {
        if (tags.isBlank()) return false
        return tags.split(TAG_SPLIT).any { isAiTag(it) }
    }

    fun isAiTag(rawTag: String): Boolean {
        val clean = rawTag.trim()
            .removePrefix("-")
            .removePrefix("+")
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
            .lowercase()

        if (clean.isBlank()) return false
        if (clean in AI_TAG_EXACT) return true

        if (clean.startsWith("ai:") || clean.endsWith(":ai")) return true
        if (clean == "synthetic" || clean.startsWith("synthetic:") || clean.endsWith(":synthetic")) return true

        return AI_COMPOUNDS.any { compound ->
            clean == compound ||
                clean.contains("_${compound}_") ||
                clean.startsWith("${compound}_") ||
                clean.endsWith("_${compound}") ||
                clean.contains(":${compound}") ||
                clean.contains("${compound}:") ||
                clean.contains("-${compound}-") ||
                clean.startsWith("${compound}-") ||
                clean.endsWith("-${compound}")
        }
    }
}
