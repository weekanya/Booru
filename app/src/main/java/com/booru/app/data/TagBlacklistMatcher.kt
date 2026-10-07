package com.booru.app.data

import com.booru.app.RemoteMedia

object TagBlacklistMatcher {

    fun matchesTag(pattern: String, tag: String): Boolean {
        if (pattern.isBlank() || tag.isBlank()) return false
        val p = pattern.trim().lowercase()
        val t = tag.trim().lowercase()

        if (!p.contains("*")) {
            return p == t
        }

        val inner = p.trim('*')
        if (inner.isEmpty()) return false

        if (!inner.contains("*")) {
            return when {
                p.startsWith("*") && p.endsWith("*") -> t.contains(inner)
                p.startsWith("*") -> t.endsWith(inner)
                else -> t.startsWith(inner)
            }
        }

        val regexPattern = p.split('*').joinToString(".*", prefix = "^", postfix = "$") { Regex.escape(it) }
        return runCatching { Regex(regexPattern).matches(t) }.getOrDefault(false)
    }

    fun isBlacklisted(media: RemoteMedia, blacklist: Collection<String>): Boolean {
        if (blacklist.isEmpty()) return false
        val mediaTags = media.tagList.map { it.lowercase() }.toSet()
        if (mediaTags.isEmpty()) return false
        val mediaTagsStripped = mediaTags.mapNotNull { if (it.contains(":")) it.substringAfter(":") else null }.toSet()

        return blacklist.any { bl ->
            val clean = bl.trim().lowercase()
            if (clean.isBlank()) return@any false

            val hasWildcard = clean.contains("*")
            val hasColon = clean.contains(":")

            if (!hasWildcard) {
                if (hasColon) {
                    clean in mediaTags || clean.substringAfter(":") in mediaTags
                } else {
                    clean in mediaTags || clean in mediaTagsStripped
                }
            } else {
                if (hasColon) {
                    val rawCleanPrefix = clean.substringBefore(":")
                    val rawCleanSuffix = clean.substringAfter(":")
                    mediaTags.any { tag ->
                        matchesTag(clean, tag) || (tag.contains(":") && matchesTag(rawCleanSuffix, tag.substringAfter(":")))
                    }
                } else {
                    mediaTags.any { tag -> matchesTag(clean, tag) } ||
                        mediaTagsStripped.any { tag -> matchesTag(clean, tag) }
                }
            }
        }
    }
}
