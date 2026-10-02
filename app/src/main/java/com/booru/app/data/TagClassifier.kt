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

    fun classify(tag: String): ClassifiedTag {
        val raw = tag.trim()
        val lower = raw.lowercase()

        val category = when {
            lower.startsWith("artist:") || lower.startsWith("art:") -> TagCategory.ARTIST
            lower.startsWith("character:") || lower.startsWith("char:") -> TagCategory.CHARACTER
            lower.startsWith("copyright:") || lower.startsWith("series:") || lower.startsWith("copy:") -> TagCategory.COPYRIGHT
            lower.startsWith("meta:") || lower.startsWith("metadata:") || lower in KNOWN_META_TAGS -> TagCategory.META
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
