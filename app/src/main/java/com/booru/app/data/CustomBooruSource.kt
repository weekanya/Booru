package com.booru.app.data

import org.json.JSONObject

enum class BooruEngine(val displayName: String) {
    GELBOORU("Gelbooru / DAPI (index.php)"),
    MOEBOORU("Moebooru (post.json)"),
    DANBOORU("Danbooru 2.x (posts.json)")
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

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("baseUrl", baseUrl)
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
                baseUrl = baseUrl.trim().trimEnd('/'),
                engine = engine,
                apiKey = json.optString("apiKey", ""),
                userId = json.optString("userId", "")
            )
        }
    }
}
