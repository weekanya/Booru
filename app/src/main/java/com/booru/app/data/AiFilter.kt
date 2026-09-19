package com.booru.app.data

object AiFilter {

    private val AI_TAG_EXACT = setOf(
        "ai_generated",
        "ai-generated",
        "novelai",
        "novel_ai",
        "stable_diffusion",
        "stablediffusion",
        "midjourney",
        "dall-e",
        "dall_e",
        "dalle",
        "synthetic",
        "created_by_ai",
        "ai_art",
        "ai-art",
        "ai_upscale",
        "deepfake",
        "ai"
    )

    private val AI_COMPOUNDS = setOf(
        "ai_generated",
        "ai-generated",
        "novelai",
        "novel_ai",
        "stable_diffusion",
        "stablediffusion",
        "midjourney",
        "dall-e",
        "dall_e",
        "synthetic",
        "created_by_ai",
        "ai_art",
        "ai-art",
        "ai_upscale",
        "deepfake"
    )

    fun isAiGeneratedPost(tags: String): Boolean {
        if (tags.isBlank()) return false
        val tokens = tags.split(Regex("[\\s,]+"))
        return tokens.any { isAiTag(it) }
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
