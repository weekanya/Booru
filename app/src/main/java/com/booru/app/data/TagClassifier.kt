package com.booru.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TagCategory(
    val displayName: String,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightContent: Color,
    val darkContent: Color,
    val icon: ImageVector
) {
    ARTIST(
        displayName = "Artist",
        lightContainer = Color(0xFFFFEBEE),
        darkContainer = Color(0xFF3C1F22),
        lightContent = Color(0xFFD32F2F),
        darkContent = Color(0xFFFF8A80),
        icon = Icons.Rounded.Palette
    ),
    CHARACTER(
        displayName = "Character",
        lightContainer = Color(0xFFE8F5E9),
        darkContainer = Color(0xFF1B3420),
        lightContent = Color(0xFF2E7D32),
        darkContent = Color(0xFFA5D6A7),
        icon = Icons.Rounded.Person
    ),
    COPYRIGHT(
        displayName = "Copyright",
        lightContainer = Color(0xFFF3E5F5),
        darkContainer = Color(0xFF341A3E),
        lightContent = Color(0xFF7B1FA2),
        darkContent = Color(0xFFCE93D8),
        icon = Icons.Rounded.AutoStories
    ),
    META(
        displayName = "Meta",
        lightContainer = Color(0xFFFFF3E0),
        darkContainer = Color(0xFF3A2B14),
        lightContent = Color(0xFFE65100),
        darkContent = Color(0xFFFFCC80),
        icon = Icons.Rounded.Info
    ),
    GENERAL(
        displayName = "General",
        lightContainer = Color.Unspecified,
        darkContainer = Color.Unspecified,
        lightContent = Color.Unspecified,
        darkContent = Color.Unspecified,
        icon = Icons.Rounded.Tag
    );

    fun containerColor(isDark: Boolean): Color? {
        val c = if (isDark) darkContainer else lightContainer
        return if (c == Color.Unspecified) null else c
    }

    fun contentColor(isDark: Boolean): Color? {
        val c = if (isDark) darkContent else lightContent
        return if (c == Color.Unspecified) null else c
    }
}

data class ClassifiedTag(
    val rawTag: String,
    val displayTag: String,
    val category: TagCategory
)

object TagClassifier {

    private val KNOWN_META_TAGS = setOf(
        "highres", "absurdres", "superabsurdres", "incredibly_absurdres",
        "huge_filesize", "translated", "translation_request", "partially_translated",
        "commentary", "commentary_request", "commentary_typo", "scan", "lossless",
        "ai_generated", "third-party_edit", "bad_id", "tagme", "check_my_tags",
        "source_request", "artist_request", "character_request", "copyright_request",
        "md5_mismatch", "duplicate", "watermark", "sample", "poor_quality"
    )

    private val COUNT_PERSON_REGEX = Regex("""^(\d+\+?|multiple|no)[\s_]*(girls?|boys?|females?|males?|others?)$""")

    private val COMMON_GENERAL_DESCRIPTORS = setOf(
        "solo", "duo", "trio", "group", "female", "male", "hetero", "yuri", "yaoi",
        "looking_at_viewer", "simple_background", "white_background", "transparent_background",
        "black_background", "monochrome", "greyscale", "grayscale", "comic", "parody",
        "watermark", "sample", "bad_id", "tagme", "scan", "poor_quality", "md5_mismatch",
        "official_art", "anime", "manga", "game_cg", "animated", "video", "webm", "mp4",
        "sound", "lossless", "ai_generated", "novelai", "stable_diffusion",
        "safe", "general", "questionable", "explicit", "uncensored", "censored"
    )

    fun isRecommendationCandidate(tag: String): Boolean {
        val lower = tag.trim().lowercase().trim(',', ';', '.', '(', ')', '"', '\'')
        if (lower.length <= 1 || lower.contains(":") || lower.startsWith("-")) return false
        if (COUNT_PERSON_REGEX.matches(lower)) return false
        if (lower in COMMON_GENERAL_DESCRIPTORS) return false
        if (lower.endsWith("res") || lower.contains("resolution") || lower.contains("filesize")) return false
        if (lower.endsWith("_request") || lower.contains("commentary") || lower.contains("translated")) return false
        if (lower in KNOWN_META_TAGS) return false
        return true
    }

    fun classify(tag: String, typeHint: String = ""): ClassifiedTag {
        val raw = tag.trim()
        val lower = raw.lowercase()
        val hint = typeHint.trim().lowercase()

        val category = when {
            hint == "1" || hint == "artist" || hint == "art" -> TagCategory.ARTIST
            hint == "4" || hint == "character" || hint == "char" -> TagCategory.CHARACTER
            hint == "3" || hint == "copyright" || hint == "series" || hint == "copy" -> TagCategory.COPYRIGHT
            hint == "5" || hint == "meta" || hint == "metadata" -> TagCategory.META
            hint == "0" || hint == "general" || hint == "tag" -> TagCategory.GENERAL
            lower.startsWith("artist:") || lower.startsWith("art:") -> TagCategory.ARTIST
            lower.startsWith("character:") || lower.startsWith("char:") -> TagCategory.CHARACTER
            lower.startsWith("copyright:") || lower.startsWith("series:") || lower.startsWith("copy:") -> TagCategory.COPYRIGHT
            lower.startsWith("meta:") || lower.startsWith("metadata:") || lower in KNOWN_META_TAGS -> TagCategory.META
            lower.endsWith("_request") || lower.contains("commentary") || lower.contains("translated") -> TagCategory.META
            lower.endsWith("res") || lower.contains("filesize") -> TagCategory.META
            lower.contains("_(") && lower.endsWith(")") -> TagCategory.CHARACTER
            else -> TagCategory.GENERAL
        }

        val display = when {
            lower.startsWith("artist:") -> raw.substringAfter("artist:")
            lower.startsWith("art:") -> raw.substringAfter("art:")
            lower.startsWith("character:") -> raw.substringAfter("character:")
            lower.startsWith("char:") -> raw.substringAfter("char:")
            lower.startsWith("copyright:") -> raw.substringAfter("copyright:")
            lower.startsWith("series:") -> raw.substringAfter("series:")
            lower.startsWith("copy:") -> raw.substringAfter("copy:")
            lower.startsWith("meta:") -> raw.substringAfter("meta:")
            lower.startsWith("metadata:") -> raw.substringAfter("metadata:")
            else -> raw
        }.replace("_", " ")

        return ClassifiedTag(rawTag = raw, displayTag = display, category = category)
    }
}
