package com.booru.app.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.util.UUID

enum class BooruEngine(val displayName: String) {
    GELBOORU("Gelbooru / DAPI (index.php)"),
    MOEBOORU("Moebooru (post.json)"),
    DANBOORU("Danbooru / e621 (posts.json)")
}

fun sanitizeBooruBaseUrl(raw: String): String {
    var url = raw.trim().trimEnd('/')
    val suffixes = listOf(
        "/posts.json", "/posts.xml", "/posts",
        "/post.json", "/post.xml", "/post",
        "/index.php"
    )
    for (suffix in suffixes) {
        if (url.endsWith(suffix, ignoreCase = true)) {
            url = url.substring(0, url.length - suffix.length).trimEnd('/')
            break
        }
    }
    return url
}

fun isHttpsBooruUrl(raw: String): Boolean {
    val clean = sanitizeBooruBaseUrl(raw)
    val parsed = clean.toHttpUrlOrNull() ?: return false
    return parsed.isHttps && parsed.host.isNotBlank() && parsed.host.contains(".")
}

fun isBuiltInSourceName(name: String): Boolean {
    val clean = name.trim().lowercase()
    val builtIns = setOf(
        "all sources",
        "all",
        "recommendations",
        "rule34",
        "gelbooru",
        "realbooru",
        "xbooru",
        "tbib",
        "yande",
        "yande.re",
        "konachan",
        "safebooru"
    )
    return clean in builtIns
}

data class CustomBooruSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val engine: BooruEngine = BooruEngine.GELBOORU,
    val enabled: Boolean = true
) {
    val key: String get() = if (id.startsWith("custom_")) id else "custom_$id"
    val cleanBaseUrl: String get() = sanitizeBooruBaseUrl(baseUrl)
    val isHttps: Boolean get() = isHttpsBooruUrl(baseUrl)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("baseUrl", cleanBaseUrl)
        put("engine", engine.name)
        put("enabled", enabled)
    }

    companion object {
        fun create(
            name: String,
            baseUrl: String,
            engine: BooruEngine,
            enabled: Boolean = true
        ): CustomBooruSource {
            return CustomBooruSource(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                baseUrl = sanitizeBooruBaseUrl(baseUrl),
                engine = engine,
                enabled = enabled
            )
        }

        fun fromJson(json: JSONObject): CustomBooruSource? {
            val id = json.optString("id").ifBlank { return null }
            val name = json.optString("name").ifBlank { return null }
            val baseUrl = json.optString("baseUrl").ifBlank { return null }
            val engineName = json.optString("engine", BooruEngine.GELBOORU.name)
            val engine = runCatching { BooruEngine.valueOf(engineName) }.getOrDefault(BooruEngine.GELBOORU)
            val enabled = json.optBoolean("enabled", true)
            return CustomBooruSource(
                id = id,
                name = name,
                baseUrl = sanitizeBooruBaseUrl(baseUrl),
                engine = engine,
                enabled = enabled
            )
        }
    }
}
