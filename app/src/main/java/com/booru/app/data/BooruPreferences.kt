package com.booru.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.booru.app.RemoteMedia
import com.booru.app.ui.AppPalette
import com.booru.app.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "booru_settings")

enum class ImageQuality(val code: String) {
    ORIGINAL("original"),
    SAMPLE("sample"),
    SAVER("saver")
}

class BooruPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLOR_PALETTE = stringPreferencesKey("color_palette")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_SAFE_MODE = booleanPreferencesKey("safe_mode")
        val KEY_EXCLUDE_SAFE = booleanPreferencesKey("exclude_safe")
        val KEY_NO_AI = booleanPreferencesKey("no_ai_filter")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DEFAULT_SOURCE = stringPreferencesKey("default_source")
        val KEY_SEARCH_HISTORY = stringSetPreferencesKey("search_history")
        val KEY_FAVORITES_JSON = stringPreferencesKey("favorites_json")

        val KEY_RULE34_USER_ID = stringPreferencesKey("rule34_user_id")
        val KEY_RULE34_API_KEY = stringPreferencesKey("rule34_api_key")
        val KEY_GELBOORU_USER_ID = stringPreferencesKey("gelbooru_user_id")
        val KEY_GELBOORU_API_KEY = stringPreferencesKey("gelbooru_api_key")
        val KEY_TAG_BLACKLIST = stringSetPreferencesKey("tag_blacklist")
        val KEY_IGNORED_UPDATE_VERSION = stringPreferencesKey("ignored_update_version")

        val KEY_IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val KEY_CUSTOM_SOURCES = stringPreferencesKey("custom_sources")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    val palette: Flow<AppPalette> = context.dataStore.data.map { prefs ->
        val code = prefs[KEY_COLOR_PALETTE] ?: "monet"
        AppPalette.entries.find { it.code == code } ?: AppPalette.MONET
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    val safeMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAFE_MODE] ?: false
    }

    val excludeSafe: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXCLUDE_SAFE] ?: false
    }

    val noAiFilter: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NO_AI] ?: false
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[KEY_LANGUAGE] ?: AppLanguage.ENGLISH.code
        if (code == "ru") AppLanguage.RUSSIAN else AppLanguage.ENGLISH
    }

    val defaultSource: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SOURCE] ?: "All sources"
    }

    val searchHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEARCH_HISTORY]?.toList() ?: emptyList()
    }

    val rule34UserId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RULE34_USER_ID] ?: ""
    }

    val rule34ApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RULE34_API_KEY] ?: ""
    }

    val gelbooruUserId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GELBOORU_USER_ID] ?: ""
    }

    val gelbooruApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GELBOORU_API_KEY] ?: ""
    }

    val tagBlacklist: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_TAG_BLACKLIST]?.toList() ?: emptyList()
    }

    val ignoredUpdateVersion: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_IGNORED_UPDATE_VERSION]
    }

    val imageQuality: Flow<ImageQuality> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_IMAGE_QUALITY]) {
            "original" -> ImageQuality.ORIGINAL
            "saver"    -> ImageQuality.SAVER
            else       -> ImageQuality.SAMPLE
        }
    }

    val customSources: Flow<List<CustomBooruSource>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_CUSTOM_SOURCES] ?: ""
        if (jsonStr.isBlank()) emptyList()
        else {
            runCatching {
                val arr = JSONArray(jsonStr)
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let { CustomBooruSource.fromJson(it) }
                }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun setImageQuality(quality: ImageQuality) {
        context.dataStore.edit { it[KEY_IMAGE_QUALITY] = quality.code }
    }

    suspend fun saveCustomSources(sources: List<CustomBooruSource>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            sources.forEach { arr.put(it.toJson()) }
            prefs[KEY_CUSTOM_SOURCES] = arr.toString()
        }
    }

    val favorites: Flow<List<RemoteMedia>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_FAVORITES_JSON] ?: "[]"
        deserializeFavorites(jsonStr)
    }

    suspend fun setIgnoredUpdateVersion(version: String) {
        context.dataStore.edit { it[KEY_IGNORED_UPDATE_VERSION] = version }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.code }
    }

    suspend fun setPalette(palette: AppPalette) {
        context.dataStore.edit { it[KEY_COLOR_PALETTE] = palette.code }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang.code }
    }

    suspend fun setSafeMode(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_SAFE_MODE] = enabled
            if (enabled) {
                it[KEY_EXCLUDE_SAFE] = false
            }
        }
    }

    suspend fun setExcludeSafe(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_EXCLUDE_SAFE] = enabled
            if (enabled) {
                it[KEY_SAFE_MODE] = false
            }
        }
    }

    suspend fun setNoAiFilter(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_NO_AI] = enabled
        }
    }

    suspend fun setDefaultSource(source: String) {
        context.dataStore.edit { it[KEY_DEFAULT_SOURCE] = source }
    }

    suspend fun setRule34Credentials(userId: String, apiKey: String) {
        context.dataStore.edit {
            it[KEY_RULE34_USER_ID] = userId.trim()
            it[KEY_RULE34_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setGelbooruCredentials(userId: String, apiKey: String) {
        context.dataStore.edit {
            it[KEY_GELBOORU_USER_ID] = userId.trim()
            it[KEY_GELBOORU_API_KEY] = apiKey.trim()
        }
    }

    suspend fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SEARCH_HISTORY]?.toMutableSet() ?: mutableSetOf()
            current.remove(clean)
            val updated = linkedSetOf(clean)
            updated.addAll(current.take(19))
            prefs[KEY_SEARCH_HISTORY] = updated
        }
    }

    suspend fun removeSearchQuery(query: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SEARCH_HISTORY]?.toMutableSet() ?: return@edit
            current.remove(query)
            prefs[KEY_SEARCH_HISTORY] = current
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { it.remove(KEY_SEARCH_HISTORY) }
    }

    suspend fun addTagToBlacklist(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TAG_BLACKLIST]?.toMutableSet() ?: mutableSetOf()
            current.add(clean)
            prefs[KEY_TAG_BLACKLIST] = current
        }
    }

    suspend fun removeTagFromBlacklist(tag: String) {
        val clean = tag.trim().lowercase()
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TAG_BLACKLIST]?.toMutableSet() ?: return@edit
            current.remove(clean)
            prefs[KEY_TAG_BLACKLIST] = current
        }
    }

    suspend fun clearTagBlacklist() {
        context.dataStore.edit { it.remove(KEY_TAG_BLACKLIST) }
    }

    suspend fun saveFavorites(favs: List<RemoteMedia>) {
        val serialized = serializeFavorites(favs)
        context.dataStore.edit { it[KEY_FAVORITES_JSON] = serialized }
    }

    private fun serializeFavorites(list: List<RemoteMedia>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("url", item.url)
                put("preview", item.preview)
                put("sample", item.sample)
                put("tags", item.tags)
                put("score", item.score)
                put("source", item.source)
                put("rating", item.rating)
                put("width", item.width)
                put("height", item.height)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeFavorites(json: String): List<RemoteMedia> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                RemoteMedia(
                    id = obj.optString("id"),
                    url = obj.optString("url"),
                    preview = obj.optString("preview"),
                    sample = obj.optString("sample"),
                    tags = obj.optString("tags"),
                    score = obj.optInt("score", 0),
                    source = obj.optString("source"),
                    rating = obj.optString("rating", "s"),
                    width = obj.optInt("width", 0),
                    height = obj.optInt("height", 0)
                )
            }
        }.getOrDefault(emptyList())
    }
}
