package com.booru.app.data

import org.json.JSONObject

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

data class CustomBooruSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val engine: BooruEngine = BooruEngine.GELBOORU,
    val apiKey: String = "",
    val userId: String = ""
) {
    val key: String get() = if (id.startsWith("custom_")) id else "custom_$id"
    val cleanBaseUrl: String get() = sanitizeBooruBaseUrl(baseUrl)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("baseUrl", cleanBaseUrl)
        put("engine", engine.name)
        put("apiKey", apiKey)
        put("userId", userId)
    }

    companion object {
        fun fromJson(json: JSONObject): CustomBooruSource? {
            val id = json.optString("id").ifBlank { return null }
            val name = json.optString("name").ifBlank { return null }
            val baseUrl = json.optString("baseUrl").ifBlank { return null }
            val engineName = json.optString("engine", BooruEngine.GELBOORU.name)
            val engine = runCatching { BooruEngine.valueOf(engineName) }.getOrDefault(BooruEngine.GELBOORU)
            return CustomBooruSource(
                id = id,
                name = name,
                baseUrl = sanitizeBooruBaseUrl(baseUrl),
                engine = engine,
                apiKey = json.optString("apiKey", ""),
                userId = json.optString("userId", "")
            )
        }
    }
}
