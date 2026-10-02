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

        if (p.startsWith("*") && p.endsWith("*") && p.length > 2) {
            val sub = p.substring(1, p.length - 1)
            return t.contains(sub)
        }

        if (p.startsWith("*") && !p.substring(1).contains("*")) {
            val suffix = p.substring(1)
            return t.endsWith(suffix)
        }

        if (p.endsWith("*") && !p.substring(0, p.length - 1).contains("*")) {
            val prefix = p.substring(0, p.length - 1)
            return t.startsWith(prefix)
        }

        val regexPattern = "^" + Regex.escape(p).replace("\\*", ".*") + "$"
        return runCatching {
            Regex(regexPattern, RegexOption.IGNORE_CASE).matches(t)
        }.getOrDefault(false)
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
