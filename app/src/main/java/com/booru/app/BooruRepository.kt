package com.booru.app

import android.util.Log
import com.booru.app.data.CustomBooruSource
import com.booru.app.data.BooruEngine
import com.booru.app.data.parser.RealbooruHtmlParser
import com.booru.app.data.parser.TimestampParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class TagSuggestion(
    val value: String,
    val label: String,
    val count: Int = 0,
    val type: String = ""
)

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    SCORE("Score"),
    RANDOM("Random")
}

data class BooruCredentials(
    val rule34UserId: String = "",
    val rule34ApiKey: String = "",
    val gelbooruUserId: String = "",
    val gelbooruApiKey: String = "",
    val customCredentials: Map<String, Pair<String, String>> = emptyMap()
)

open class BooruException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BooruAuthException(
    val sourceKey: String,
    val statusCode: Int? = null,
    message: String = "Authentication required for $sourceKey"
) : BooruException(message)

class BooruHttpException(
    val sourceKey: String,
    val statusCode: Int,
    val retryAfterSec: Int? = null,
    message: String = "HTTP $statusCode from $sourceKey"
) : BooruException(message)

class BooruRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.isRedirect) {
                val location = response.header("Location")
                if (!location.isNullOrBlank()) {
                    val targetUrl = request.url.resolve(location)
                    if (targetUrl != null) {
                        val hasCredentials = request.url.queryParameter("api_key") != null ||
                                request.url.queryParameter("user_id") != null ||
                                request.url.queryParameter("login") != null ||
                                request.header("Authorization") != null
                        if (hasCredentials) {
                            if (!targetUrl.isHttps) {
                                response.close()
                                throw IOException("Insecure redirect to non-HTTPS URL blocked")
                            }
                            if (!targetUrl.host.equals(request.url.host, ignoreCase = true)) {
                                response.close()
                                throw IOException("Cross-host redirect blocked for authenticated request")
                            }
                        }
                    }
                }
            }
            response
        }
        .build()
) {

    companion object {
        const val TAG = "BooruRepo"
        const val PAGE_SIZE = 40
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 BooruClient/1.0"
        private const val MAX_CONCURRENT_REQUESTS = 4
        private const val MAX_RETRY_AFTER_SECONDS = 60

        val EXPLICIT_RATINGS = setOf("e", "explicit", "q", "questionable")
        val SAFE_RATINGS = setOf("s", "safe", "g", "general")
        val MEDIA_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif", "mp4", "webm", "mkv", "mov", "zip")

        val AI_TAG_KEYWORDS = setOf(
            "ai_generated",
            "novelai",
            "stable_diffusion",
            "midjourney",
            "dall-e",
            "synthetic",
            "created_by_ai",
            "ai_art",
            "ai_upscale",
            "deepfake"
        )

        const val SOURCE_ALL = "All sources"
        const val SOURCE_SAFEBOORU = "Safebooru"
        const val SOURCE_YANDE = "Yande.re"
        const val SOURCE_RULE34 = "Rule34"
        const val SOURCE_GELBOORU = "Gelbooru"
        const val SOURCE_REALBOORU = "Realbooru"
        const val SOURCE_XBOORU = "Xbooru"
        const val SOURCE_TBIB = "TBIB"
        const val SOURCE_KONACHAN = "Konachan"

        val AVAILABLE_SOURCES = listOf(
            SOURCE_ALL,
            SOURCE_RULE34,
            SOURCE_GELBOORU,
            SOURCE_REALBOORU,
            SOURCE_XBOORU,
            SOURCE_TBIB,
            SOURCE_YANDE,
            SOURCE_KONACHAN,
            SOURCE_SAFEBOORU
        )

        fun isAiGeneratedPost(tags: String): Boolean {
            return com.booru.app.data.AiFilter.isAiGeneratedPost(tags)
        }

        fun getSourceDisplayName(key: String, customSources: List<CustomBooruSource> = emptyList()): String {
            val custom = customSources.find { it.key == key || it.id == key }
                ?: customSources.find { it.name.equals(key, ignoreCase = true) }
            if (custom != null) return custom.name
            if (key == SOURCE_ALL || key.equals("all sources", ignoreCase = true)) return "Recommendations"
            return when (key.lowercase()) {
                "rule34"    -> SOURCE_RULE34
                "gelbooru"  -> SOURCE_GELBOORU
                "realbooru" -> SOURCE_REALBOORU
                "xbooru"    -> SOURCE_XBOORU
                "tbib"      -> SOURCE_TBIB
                "yande"     -> SOURCE_YANDE
                "konachan"  -> SOURCE_KONACHAN
                "safebooru" -> SOURCE_SAFEBOORU
                else        -> key
            }
        }
    }

    suspend fun search(
        source: String,
        tags: String,
        safeMode: Boolean,
        excludeSafe: Boolean = false,
        noAi: Boolean = false,
        page: Int = 0,
        sortOrder: SortOrder = SortOrder.NEWEST,
        contentTypes: Set<ContentType> = emptySet(),
        credentials: BooruCredentials = BooruCredentials(),
        customSources: List<CustomBooruSource> = emptyList()
    ): List<RemoteMedia> = withContext(Dispatchers.IO) {
        val customMatch = customSources.find { it.key == source || it.id == source }
            ?: customSources.find { it.name.equals(source, ignoreCase = true) }
        val wantsOnlyVideos = contentTypes.contains(ContentType.VIDEOS) && !contentTypes.contains(ContentType.PHOTOS) && !contentTypes.contains(ContentType.GIFS)
        val targets = when {
            customMatch != null -> if (!customMatch.enabled) emptyList() else listOf(customMatch.key)
            source == SOURCE_SAFEBOORU -> if (excludeSafe || wantsOnlyVideos) emptyList() else listOf("safebooru")
            source == SOURCE_YANDE     -> if (wantsOnlyVideos) emptyList() else listOf("yande")
            source == SOURCE_RULE34    -> listOf("rule34")
            source == SOURCE_GELBOORU  -> listOf("gelbooru")
            source == SOURCE_REALBOORU -> listOf("realbooru")
            source == SOURCE_XBOORU    -> listOf("xbooru")
            source == SOURCE_TBIB      -> if (wantsOnlyVideos) emptyList() else listOf("tbib")
            source == SOURCE_KONACHAN  -> if (wantsOnlyVideos) emptyList() else listOf("konachan")
            else -> {
                if (wantsOnlyVideos) {
                    listOf("rule34", "gelbooru", "realbooru", "xbooru")
                } else if (excludeSafe) {
                    listOf("rule34", "gelbooru", "realbooru", "xbooru")
                } else {
                    listOf("rule34", "gelbooru", "realbooru", "xbooru", "tbib", "yande", "konachan", "safebooru")
                }
            }
        }

        if (targets.isEmpty()) {
            return@withContext emptyList()
        }

        val errors = mutableListOf<String>()
        val allResults = mutableListOf<RemoteMedia>()
        var firstAuthEx: BooruAuthException? = null

        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        val deferredList = coroutineScope {
            targets.map { key ->
                async {
                    semaphore.withPermit {
                        try {
                            val list = requestSourceWithRetry(key, tags.trim(), safeMode, excludeSafe, noAi, page, sortOrder, contentTypes, credentials, customSources)
                            Result.success(list)
                        } catch (c: kotlinx.coroutines.CancellationException) {
                            throw c
                        } catch (auth: BooruAuthException) {
                            Log.e(TAG, "[$key] Auth Error: ${auth.message}")
                            Result.failure(auth)
                        } catch (e: Exception) {
                            Log.e(TAG, "[$key] Error: ${e.message}")
                            Result.failure(e)
                        }
                    }
                }
            }.awaitAll()
        }

        for (res in deferredList) {
            res.onSuccess { allResults.addAll(it) }
            res.onFailure { ex ->
                if (ex is BooruAuthException && firstAuthEx == null) {
                    firstAuthEx = ex
                }
                errors.add(ex.message ?: "Load failed")
            }
        }

        if (allResults.isEmpty()) {
            if (firstAuthEx != null && (targets.size == 1 || errors.size == targets.size)) {
                throw firstAuthEx!!
            }
            if (errors.isNotEmpty()) {
                throw BooruException(errors.joinToString("\n"))
            }
        }

        val filteredResults = filterMediaList(allResults, safeMode, excludeSafe, noAi)

        if (source == SOURCE_ALL && filteredResults.isNotEmpty()) {
            when (sortOrder) {
                SortOrder.NEWEST -> {
                    filteredResults.sortWith(
                        compareByDescending<RemoteMedia> { it.createdAt > 0 }
                            .thenByDescending { it.createdAt }
                            .thenByDescending { it.score }
                    )
                }
                SortOrder.SCORE -> {
                    filteredResults.sortByDescending { it.score }
                }
                SortOrder.RANDOM -> {
                    filteredResults.shuffle()
                }
            }
        }
        filteredResults
    }

    private fun filterMediaList(
        items: List<RemoteMedia>,
        safeMode: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean
    ): MutableList<RemoteMedia> {
        val result = mutableListOf<RemoteMedia>()
        for (item in items) {
            if (safeMode && item.ratingType != Rating.SAFE) {
                continue
            }
            if (excludeSafe && item.ratingType == Rating.SAFE) {
                continue
            }
            if (noAi && isAiGeneratedPost(item.tags)) {
                continue
            }
            result.add(item)
        }
        return result
    }

    suspend fun getTagSuggestions(
        source: String,
        query: String,
        customSources: List<CustomBooruSource> = emptyList()
    ): List<TagSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.length < 2) return@withContext emptyList()

        val customMatch = customSources.find { it.key == source || it.id == source || it.name.equals(source, ignoreCase = true) }
        if (customMatch != null && !customMatch.enabled) {
            return@withContext emptyList()
        }

        runCatching {
            if (customMatch != null) {
                val base = customMatch.cleanBaseUrl
                when (customMatch.engine) {
                    BooruEngine.DANBOORU -> {
                        val url = "$base/autocomplete.json".toHttpUrl().newBuilder().apply {
                            addQueryParameter("search[query]", q)
                            addQueryParameter("search[type]", "tag_query")
                            addQueryParameter("limit", "10")
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optInt("post_count", 0)
                                val type = o.optString("category", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                    BooruEngine.MOEBOORU -> {
                        val url = "$base/tag.json".toHttpUrl().newBuilder().apply {
                            addQueryParameter("name", "$q*")
                            addQueryParameter("limit", "10")
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).header("Referer", "$base/").build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val name = o.optString("name")
                                val count = o.optInt("count", 0)
                                val typeInt = o.optInt("type", 0)
                                val type = when (typeInt) {
                                    1 -> "artist"
                                    3 -> "copyright"
                                    4 -> "character"
                                    else -> "general"
                                }
                                if (name.isNotBlank()) list.add(TagSuggestion(name, name, count, type))
                            }
                            list
                        }
                    }
                    BooruEngine.GELBOORU -> {
                        val url = "$base/index.php".toHttpUrl().newBuilder().apply {
                            addQueryParameter("page", "autocomplete2")
                            addQueryParameter("term", q)
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).header("Referer", "$base/").build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optInt("post_count", 0)
                                val type = o.optString("category", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                }
            } else {
                val endpointKey = when {
                    source.contains("danbooru", ignoreCase = true) -> "danbooru"
                    source.contains("gelbooru", ignoreCase = true) -> "gelbooru"
                    source == SOURCE_YANDE || source.contains("yande", ignoreCase = true) -> "yande"
                    source == SOURCE_SAFEBOORU || source.contains("safe", ignoreCase = true) -> "safebooru"
                    else -> "rule34"
                }
                when (endpointKey) {
                    "danbooru" -> {
                        val url = "https://danbooru.donmai.us/autocomplete.json".toHttpUrl().newBuilder().apply {
                            addQueryParameter("search[query]", q)
                            addQueryParameter("search[type]", "tag_query")
                            addQueryParameter("limit", "10")
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optInt("post_count", 0)
                                val type = o.optString("category", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                    "rule34" -> {
                        val url = "https://api.rule34.xxx/autocomplete.php".toHttpUrl().newBuilder().apply {
                            addQueryParameter("q", q)
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optString("total").toIntOrNull() ?: 0
                                val type = o.optString("type", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                    "safebooru" -> {
                        val url = "https://safebooru.org/autocomplete.php".toHttpUrl().newBuilder().apply {
                            addQueryParameter("q", q)
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optString("total").toIntOrNull() ?: 0
                                val type = o.optString("type", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                    "gelbooru" -> {
                        val url = "https://gelbooru.com/index.php".toHttpUrl().newBuilder().apply {
                            addQueryParameter("page", "autocomplete2")
                            addQueryParameter("term", q)
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).header("Referer", "https://gelbooru.com/").build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val value = o.optString("value")
                                val label = o.optString("label")
                                val count = o.optInt("post_count", 0)
                                val type = o.optString("category", "")
                                if (value.isNotBlank()) list.add(TagSuggestion(value, label.ifBlank { value }, count, type))
                            }
                            list
                        }
                    }
                    "yande" -> {
                        val url = "https://yande.re/tag.json".toHttpUrl().newBuilder().apply {
                            addQueryParameter("name", "$q*")
                            addQueryParameter("limit", "10")
                        }.build()
                        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).header("Referer", "https://yande.re/").build()
                        client.newCall(req).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching emptyList()
                            val body = response.body?.string() ?: return@runCatching emptyList()
                            val arr = JSONArray(body)
                            val list = mutableListOf<TagSuggestion>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val name = o.optString("name")
                                val count = o.optInt("count", 0)
                                val typeInt = o.optInt("type", 0)
                                val type = when (typeInt) {
                                    1 -> "artist"
                                    3 -> "copyright"
                                    4 -> "character"
                                    else -> "general"
                                }
                                if (name.isNotBlank()) list.add(TagSuggestion(name, name, count, type))
                            }
                            list
                        }
                    }
                    else -> emptyList()
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun normalizeUserTags(userTags: String): String {
        return userTags
            .split(Regex("[\\s,]+"))
            .map { it.trim().removeSuffix(",").removePrefix(",").trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    internal fun buildTagQuery(
        userTags: String,
        safe: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean,
        key: String,
        sortOrder: SortOrder,
        customSources: List<CustomBooruSource> = emptyList(),
        contentTypes: Set<ContentType> = emptySet()
    ): String {
        val parts = mutableListOf<String>()

        val cleaned = normalizeUserTags(userTags)
        if (cleaned.isNotBlank()) {
            parts.add(cleaned)
        }

        val custom = customSources.find { it.key == key || it.id == key }
            ?: customSources.find { it.name.equals(key, ignoreCase = true) }

        if (contentTypes.isNotEmpty()) {
            val wantsPhotos = contentTypes.contains(ContentType.PHOTOS)
            val wantsVideos = contentTypes.contains(ContentType.VIDEOS)
            val wantsGifs = contentTypes.contains(ContentType.GIFS)

            if (wantsGifs && !wantsVideos && !wantsPhotos) {
                if (!cleaned.contains("animated")) {
                    when (key) {
                        "rule34", "gelbooru", "safebooru", "xbooru", "tbib", "realbooru" -> parts.add("animated")
                        else -> if (custom != null) parts.add("animated")
                    }
                }
            } else if (wantsVideos && !wantsGifs && !wantsPhotos) {
                if (!cleaned.contains("video")) {
                    when (key) {
                        "rule34", "gelbooru", "xbooru", "realbooru" -> parts.add("video")
                        else -> if (custom != null) parts.add("video")
                    }
                }
            } else if (wantsPhotos && !wantsVideos && !wantsGifs) {
                when (key) {
                    "rule34", "gelbooru", "xbooru", "tbib", "realbooru" -> {
                        if (!cleaned.contains("-video")) parts.add("-video")
                        if (!cleaned.contains("-animated")) parts.add("-animated")
                    }
                    "safebooru" -> {
                        if (!cleaned.contains("-animated")) parts.add("-animated")
                    }
                    else -> {
                        if (custom != null) {
                            if (!cleaned.contains("-video")) parts.add("-video")
                            if (!cleaned.contains("-animated")) parts.add("-animated")
                        }
                    }
                }
            } else if (wantsVideos && wantsGifs && !wantsPhotos) {
                if (!cleaned.contains("animated")) {
                    when (key) {
                        "rule34", "gelbooru", "safebooru", "xbooru", "tbib", "realbooru" -> parts.add("animated")
                        else -> if (custom != null) parts.add("animated")
                    }
                }
            }
        }

        if (safe) {
            if (custom != null) {
                when (custom.engine) {
                    BooruEngine.MOEBOORU -> parts.add("rating:s")
                    BooruEngine.GELBOORU -> parts.add("rating:general")
                    BooruEngine.DANBOORU -> {
                        if (custom.cleanBaseUrl.contains("e621") || custom.cleanBaseUrl.contains("e926")) {
                            parts.add("rating:s")
                        } else {
                            parts.add("rating:g")
                        }
                    }
                }
            } else {
                when (key) {
                    "yande", "konachan" -> parts.add("rating:s")
                    "gelbooru"          -> parts.add("rating:general")
                    "rule34", "xbooru", "tbib", "realbooru" -> parts.add("rating:safe")
                    "safebooru"         -> Unit
                }
            }
        } else if (excludeSafe) {
            if (custom != null) {
                when (custom.engine) {
                    BooruEngine.MOEBOORU -> parts.add("-rating:s")
                    BooruEngine.GELBOORU -> parts.add("-rating:general")
                    BooruEngine.DANBOORU -> {
                        if (custom.cleanBaseUrl.contains("e621") || custom.cleanBaseUrl.contains("e926")) {
                            parts.add("-rating:s")
                        } else {
                            parts.add("-rating:g")
                        }
                    }
                }
            } else {
                when (key) {
                    "yande", "konachan" -> parts.add("-rating:s")
                    "gelbooru"          -> parts.add("-rating:general")
                    "rule34", "xbooru", "tbib", "realbooru" -> parts.add("-rating:safe")
                    "safebooru"         -> Unit
                }
            }
        }

        if (noAi) {
            if (custom != null) {
                parts.add("-ai_generated")
                parts.add("-novelai")
            } else {
                when (key) {
                    "gelbooru" -> {
                        parts.add("-ai_generated")
                        parts.add("-novelai")
                    }
                    "rule34", "xbooru", "tbib", "safebooru", "realbooru" -> {
                        parts.add("-ai_generated")
                        parts.add("-novelai")
                    }
                    "yande", "konachan" -> {
                        parts.add("-ai_generated")
                        parts.add("-novelai")
                    }
                }
            }
        }

        val effectiveSortOrder = if (key == "realbooru" && sortOrder == SortOrder.SCORE) SortOrder.NEWEST else sortOrder
        when (effectiveSortOrder) {
            SortOrder.SCORE -> {
                if (custom != null) {
                    when (custom.engine) {
                        BooruEngine.MOEBOORU, BooruEngine.DANBOORU -> parts.add("order:score")
                        BooruEngine.GELBOORU -> parts.add("sort:score:desc")
                    }
                } else {
                    when (key) {
                        "yande", "konachan" -> parts.add("order:score")
                        "gelbooru", "rule34", "xbooru", "tbib", "safebooru" -> parts.add("sort:score:desc")
                    }
                }
            }
            SortOrder.RANDOM -> {
                if (custom != null) {
                    when (custom.engine) {
                        BooruEngine.MOEBOORU, BooruEngine.DANBOORU -> parts.add("order:random")
                        BooruEngine.GELBOORU -> parts.add("sort:random")
                    }
                } else {
                    when (key) {
                        "yande", "konachan" -> parts.add("order:random")
                        "gelbooru", "rule34", "xbooru", "tbib", "safebooru", "realbooru" -> parts.add("sort:random")
                    }
                }
            }
            SortOrder.NEWEST -> {
                val hasExplicitSort = parts.any { it.startsWith("sort:") || it.startsWith("order:") }
                if (!hasExplicitSort) {
                    if (custom != null) {
                        when (custom.engine) {
                            BooruEngine.MOEBOORU, BooruEngine.DANBOORU -> parts.add("order:id_desc")
                            BooruEngine.GELBOORU -> parts.add("sort:id:desc")
                        }
                    } else {
                        when (key) {
                            "yande", "konachan" -> parts.add("order:id_desc")
                            "gelbooru", "rule34", "xbooru", "tbib", "safebooru", "realbooru" -> parts.add("sort:id:desc")
                        }
                    }
                }
            }
        }

        return parts.joinToString(" ")
    }

    private suspend fun requestSourceWithRetry(
        key: String,
        userTags: String,
        safe: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean,
        page: Int,
        sortOrder: SortOrder,
        contentTypes: Set<ContentType> = emptySet(),
        credentials: BooruCredentials,
        customSources: List<CustomBooruSource> = emptyList()
    ): List<RemoteMedia> {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < 3) {
            try {
                return requestSource(key, userTags, safe, excludeSafe, noAi, page, sortOrder, contentTypes, credentials, customSources)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (auth: BooruAuthException) {
                throw auth
            } catch (http: BooruHttpException) {
                if (http.statusCode in 400..499 && http.statusCode != 408 && http.statusCode != 429) {
                    throw http
                }
                lastException = http
                if (http.statusCode == 429 && http.retryAfterSec != null) {
                    val waitSec = http.retryAfterSec.coerceIn(1, MAX_RETRY_AFTER_SECONDS)
                    delay(waitSec * 1000L)
                } else {
                    val baseDelay = 500L * (1L shl attempt)
                    val jitter = Random.nextLong(0, 150)
                    val totalDelay = (baseDelay + jitter).coerceAtMost(5000L)
                    delay(totalDelay)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastException = e
                if (e is SocketTimeoutException || e is IOException) {
                    val baseDelay = 500L * (1L shl attempt)
                    val jitter = Random.nextLong(0, 150)
                    val totalDelay = (baseDelay + jitter).coerceAtMost(5000L)
                    delay(totalDelay)
                } else {
                    throw e
                }
            }
            attempt++
        }
        throw lastException ?: BooruException("Failed to fetch from $key")
    }

    private fun requestSource(
        key: String,
        userTags: String,
        safe: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean,
        page: Int,
        sortOrder: SortOrder,
        contentTypes: Set<ContentType> = emptySet(),
        credentials: BooruCredentials,
        customSources: List<CustomBooruSource> = emptyList()
    ): List<RemoteMedia> {
        val tagQuery = buildTagQuery(userTags, safe, excludeSafe, noAi, key, sortOrder, customSources, contentTypes)

        val custom = customSources.find { it.key == key || it.id == key }
            ?: customSources.find { it.name.equals(key, ignoreCase = true) }
        if (custom != null) {
            val base = custom.cleanBaseUrl
            if (!custom.isHttps) {
                throw BooruException("Insecure HTTP connections are not allowed for custom source '${custom.name}'. Please update its URL to HTTPS in Settings.")
            }
            val customKey = credentials.customCredentials[custom.id]?.first
                ?: credentials.customCredentials[custom.key]?.first ?: ""
            val customUid = credentials.customCredentials[custom.id]?.second
                ?: credentials.customCredentials[custom.key]?.second ?: ""
            val fullUrl = when (custom.engine) {
                BooruEngine.GELBOORU -> {
                    "$base/index.php".toHttpUrl().newBuilder().apply {
                        addQueryParameter("page", "dapi")
                        addQueryParameter("s", "post")
                        addQueryParameter("q", "index")
                        addQueryParameter("json", "1")
                        if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
                        addQueryParameter("limit", PAGE_SIZE.toString())
                        addQueryParameter("pid", page.toString())
                        if (customKey.isNotBlank() && customUid.isNotBlank()) {
                            addQueryParameter("api_key", customKey.trim())
                            addQueryParameter("user_id", customUid.trim())
                        }
                    }.build()
                }
                BooruEngine.MOEBOORU -> {
                    "$base/post.json".toHttpUrl().newBuilder().apply {
                        if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
                        addQueryParameter("limit", PAGE_SIZE.toString())
                        addQueryParameter("page", (page + 1).toString())
                    }.build()
                }
                BooruEngine.DANBOORU -> {
                    "$base/posts.json".toHttpUrl().newBuilder().apply {
                        if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
                        addQueryParameter("limit", PAGE_SIZE.toString())
                        addQueryParameter("page", (page + 1).toString())
                        if (customKey.isNotBlank() && customUid.isNotBlank()) {
                            addQueryParameter("api_key", customKey.trim())
                            addQueryParameter("login", customUid.trim())
                        }
                    }.build()
                }
            }
            logSanitized(custom.name, fullUrl)

            val reqBuilder = Request.Builder()
                .url(fullUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$base/")

            if (custom.engine == BooruEngine.DANBOORU && customUid.isNotBlank() && customKey.isNotBlank()) {
                reqBuilder.header("Authorization", Credentials.basic(customUid.trim(), customKey.trim()))
            }

            val req = reqBuilder.build()

            return client.newCall(req).execute().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    if (code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"))
                        throw BooruHttpException(sourceKey = custom.name, statusCode = code, retryAfterSec = retryAfter, message = "Rate limited by ${custom.name}")
                    }
                    throw BooruHttpException(sourceKey = custom.name, statusCode = code, message = "HTTP $code from ${custom.name}")
                }
                parseResponse(custom.name, body, noAi, base, customSources)
            }
        }

        if (key == "realbooru") {
            val urlBuilder = "https://realbooru.com/index.php".toHttpUrl().newBuilder().apply {
                addQueryParameter("page", "post")
                addQueryParameter("s", "list")
                if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
                addQueryParameter("pid", (page * PAGE_SIZE).toString())
            }
            val fullUrl = urlBuilder.build()
            logSanitized(key, fullUrl)

            val req = Request.Builder()
                .url(fullUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://realbooru.com/")
                .build()

            return client.newCall(req).execute().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    if (code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"))
                        throw BooruHttpException(sourceKey = "realbooru", statusCode = code, retryAfterSec = retryAfter, message = "Rate limited by Realbooru")
                    }
                    if (code == 401 || code == 403) {
                        throw BooruAuthException(sourceKey = "realbooru", statusCode = code, message = "Access denied by Realbooru ($code)")
                    }
                    throw BooruHttpException(sourceKey = "realbooru", statusCode = code, message = "HTTP $code from Realbooru")
                }
                RealbooruHtmlParser.parse(body, noAi)
            }
        }

        if (key == "gelbooru" && (credentials.gelbooruUserId.isBlank() || credentials.gelbooruApiKey.isBlank())) {
            throw BooruAuthException(sourceKey = "gelbooru", statusCode = 401, message = "Authentication required for Gelbooru (API Key & User ID needed)")
        }

        if (key == "rule34" && (credentials.rule34UserId.isBlank() || credentials.rule34ApiKey.isBlank())) {
            throw BooruAuthException(sourceKey = "rule34", statusCode = 401, message = "Authentication required for Rule34 (API Key & User ID needed)")
        }

        val baseUrl = when (key) {
            "safebooru" -> "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1"
            "yande"     -> "https://yande.re/post.json"
            "konachan"  -> "https://konachan.net/post.json"
            "xbooru"    -> "https://xbooru.com/index.php?page=dapi&s=post&q=index&json=1"
            "tbib"      -> "https://tbib.org/index.php?page=dapi&s=post&q=index&json=1"
            "gelbooru"  -> "https://gelbooru.com/index.php?page=dapi&s=post&q=index&json=1"
            "rule34"    -> "https://api.rule34.xxx/index.php?page=dapi&s=post&q=index&json=1"
            else        -> error("Unknown source: $key")
        }

        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        if (tagQuery.isNotBlank()) {
            urlBuilder.addQueryParameter("tags", tagQuery)
        }

        urlBuilder.addQueryParameter("limit", PAGE_SIZE.toString())

        when (key) {
            "yande", "konachan" -> urlBuilder.addQueryParameter("page", (page + 1).toString())
            else                -> urlBuilder.addQueryParameter("pid", page.toString())
        }

        if (key == "gelbooru") {
            urlBuilder.addQueryParameter("api_key", credentials.gelbooruApiKey.trim())
            urlBuilder.addQueryParameter("user_id", credentials.gelbooruUserId.trim())
        } else if (key == "rule34") {
            urlBuilder.addQueryParameter("api_key", credentials.rule34ApiKey.trim())
            urlBuilder.addQueryParameter("user_id", credentials.rule34UserId.trim())
        }

        val fullUrl = urlBuilder.build()
        logSanitized(key, fullUrl)

        val reqBuilder = Request.Builder()
            .url(fullUrl)
            .header("User-Agent", USER_AGENT)

        when (key) {
            "gelbooru"  -> reqBuilder.header("Referer", "https://gelbooru.com/")
            "rule34"    -> reqBuilder.header("Referer", "https://rule34.xxx/")
            "safebooru" -> reqBuilder.header("Referer", "https://safebooru.org/")
            "xbooru"    -> reqBuilder.header("Referer", "https://xbooru.com/")
            "tbib"      -> reqBuilder.header("Referer", "https://tbib.org/")
            "yande"     -> reqBuilder.header("Referer", "https://yande.re/")
            "konachan"  -> reqBuilder.header("Referer", "https://konachan.net/")
        }

        val req = reqBuilder.build()

        return client.newCall(req).execute().use { response ->
            val code = response.code
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (code == 401 || code == 403) {
                    throw BooruAuthException(
                        sourceKey = key,
                        statusCode = code,
                        message = "Access denied ($code) for ${getSourceDisplayName(key, customSources)}"
                    )
                }
                if (code == 429) {
                    val retryAfter = parseRetryAfter(response.header("Retry-After"))
                    throw BooruHttpException(
                        sourceKey = key,
                        statusCode = code,
                        retryAfterSec = retryAfter,
                        message = "Rate limited by ${getSourceDisplayName(key, customSources)}"
                    )
                }
                throw BooruHttpException(
                    sourceKey = key,
                    statusCode = code,
                    message = "HTTP $code from ${getSourceDisplayName(key, customSources)}"
                )
            }

            if (body.contains("Access denied", ignoreCase = true) ||
                body.contains("Authentication failed", ignoreCase = true) ||
                body.contains("api_key is invalid", ignoreCase = true) ||
                body.contains("invalid api key", ignoreCase = true) ||
                body.contains("user_id is invalid", ignoreCase = true) ||
                body.contains("invalid user id", ignoreCase = true) ||
                body.contains("login failed", ignoreCase = true)
            ) {
                throw BooruAuthException(
                    sourceKey = key,
                    statusCode = 401,
                    message = "Invalid credentials for ${getSourceDisplayName(key, customSources)}"
                )
            }

            parseResponse(key, body, noAi, "", customSources)
        }
    }

    private fun parseRetryAfter(headerValue: String?): Int? {
        if (headerValue.isNullOrBlank()) return null
        headerValue.trim().toIntOrNull()?.let { return it }
        return runCatching {
            val date = DateTimeFormatter.RFC_1123_DATE_TIME.parse(headerValue.trim(), Instant::from)
            val diffSec = date.epochSecond - Instant.now().epochSecond
            if (diffSec > 0) diffSec.toInt() else null
        }.getOrNull()
    }

    private fun logSanitized(key: String, url: HttpUrl) {
        val sensitiveKeys = setOf("api_key", "user_id", "password", "login", "token", "secret", "auth", "pass", "api-key", "apikey")
        val builder = url.newBuilder()
        for (paramName in url.queryParameterNames) {
            val lower = paramName.lowercase()
            if (sensitiveKeys.any { lower.contains(it) }) {
                builder.setQueryParameter(paramName, "[REDACTED]")
            }
        }
        val sanitized = builder.build()
        Log.d(TAG, "[$key] GET $sanitized")
    }

    internal fun parseResponse(
        key: String,
        body: String,
        noAi: Boolean,
        customBaseUrl: String = "",
        customSources: List<CustomBooruSource> = emptyList()
    ): List<RemoteMedia> {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed == "[]" || trimmed == "{}") return emptyList()

        val jsonArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("post")
                    ?: obj.optJSONArray("posts")
                    ?: obj.optJSONArray("images")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val results = mutableListOf<RemoteMedia>()

        for (i in 0 until jsonArray.length()) {
            val o = jsonArray.optJSONObject(i) ?: continue

            val ratingCode = when (o.optString("rating").lowercase().trim()) {
                "s", "safe", "general", "g" -> "safe"
                "q", "questionable", "sensitive" -> "questionable"
                "e", "explicit" -> "explicit"
                else -> "u"
            }

            val fileObj = o.optJSONObject("file")
            var fileUrl = o.optString("file_url")
                .ifBlank { o.optString("fileUrl") }
                .ifBlank { o.optString("jpeg_url") }
                .ifBlank { o.optString("high_res_url") }
                .ifBlank { fileObj?.optString("url") ?: "" }

            val id = o.optString("id", "")
            val directory = o.optString("directory").takeIf { it != "null" } ?: ""
            val image = o.optString("image").takeIf { it != "null" } ?: ""
            val hash = o.optString("hash").takeIf { it != "null" }
                ?: fileObj?.optString("md5")
                ?: o.optString("md5")
            val baseImgName = image.substringBeforeLast(".")

            if (fileUrl.isBlank() && directory.isNotBlank() && image.isNotBlank()) {
                val host = when (key) {
                    "safebooru" -> "https://safebooru.org"
                    "gelbooru"  -> "https://gelbooru.com"
                    "rule34"    -> "https://api-cdn.rule34.xxx"
                    "xbooru"    -> if (image.endsWith(".mp4")) "https://mp4.xbooru.com" else "https://img.xbooru.com"
                    "tbib"      -> "https://tbib.org"
                    else        -> ""
                }
                if (host.isNotBlank()) {
                    fileUrl = "$host/images/$directory/$image"
                }
            }

            if (fileUrl.isBlank() && !hash.isNullOrBlank() && hash.length >= 4 && (customBaseUrl.contains("e621") || customBaseUrl.contains("e926"))) {
                val ext = fileObj?.optString("ext") ?: "jpg"
                fileUrl = "https://static1.e621.net/data/${hash.take(2)}/${hash.substring(2, 4)}/$hash.$ext"
            }

            if (fileUrl.isBlank() ||
                fileUrl == "null" ||
                fileUrl.endsWith("/") ||
                fileUrl.endsWith("//") ||
                fileUrl.contains("/images//") ||
                fileUrl.contains("/thumbnails//") ||
                fileUrl.contains("/samples//")
            ) {
                continue
            }

            if (fileUrl.startsWith("/") && !fileUrl.startsWith("//") && customBaseUrl.isNotBlank()) {
                fileUrl = "${customBaseUrl.trimEnd('/')}$fileUrl"
            }
            if (fileUrl.startsWith("//")) {
                fileUrl = "https:$fileUrl"
            }

            val cleanFile = fileUrl.substringBefore("?").lowercase()
            val isVideo = cleanFile.endsWith(".mp4") || cleanFile.endsWith(".webm") || cleanFile.endsWith(".mkv") || cleanFile.endsWith(".mov") || (fileObj?.optString("ext")?.lowercase() in listOf("mp4", "webm", "mkv", "mov"))
            val isGif = cleanFile.endsWith(".gif") || image.substringBefore("?").lowercase().endsWith(".gif") || (fileObj?.optString("ext")?.lowercase() == "gif")

            val previewObj = o.optJSONObject("preview")
            var preview = o.optString("preview_url")
                .ifBlank { o.optString("previewUrl") }
                .ifBlank { o.optString("preview_file_url") }
                .ifBlank { previewObj?.optString("url") ?: "" }

            if (preview.isBlank() && directory.isNotBlank() && image.isNotBlank()) {
                val host = when (key) {
                    "safebooru" -> "https://safebooru.org"
                    "gelbooru"  -> "https://img3.gelbooru.com"
                    "rule34"    -> "https://api-cdn.rule34.xxx"
                    "xbooru"    -> "https://xbooru.com"
                    "tbib"      -> "https://tbib.org"
                    else        -> ""
                }
                if (host.isNotBlank()) {
                    val querySuffix = if (key == "xbooru" && id.isNotBlank()) "?$id" else ""
                    preview = "$host/thumbnails/$directory/thumbnail_$baseImgName.jpg$querySuffix"
                }
            }

            if (preview.isBlank() && !hash.isNullOrBlank() && hash.length >= 4 && (customBaseUrl.contains("e621") || customBaseUrl.contains("e926"))) {
                preview = "https://static1.e621.net/data/preview/${hash.take(2)}/${hash.substring(2, 4)}/$hash.jpg"
            }

            if (preview.startsWith("/") && !preview.startsWith("//") && customBaseUrl.isNotBlank()) {
                preview = "${customBaseUrl.trimEnd('/')}$preview"
            }
            if (preview.startsWith("//")) {
                preview = "https:$preview"
            }

            if (key == "xbooru" && id.isNotBlank() && !preview.contains("?")) {
                preview = "$preview?$id"
            }

            val sampleObj = o.optJSONObject("sample")
            var sample = o.optString("sample_url")
                .ifBlank { o.optString("sampleUrl") }
                .ifBlank { o.optString("large_file_url") }
                .ifBlank { sampleObj?.optString("url") ?: "" }

            if (isGif) {
                sample = fileUrl
            }

            val hasSample = o.optBoolean("sample", false) || o.optInt("sample", 0) == 1 || (sampleObj?.optBoolean("has", false) == true)
            if (sample.isBlank() && hasSample && directory.isNotBlank() && image.isNotBlank()) {
                val host = when (key) {
                    "safebooru" -> "https://safebooru.org"
                    "rule34"    -> "https://api-cdn.rule34.xxx"
                    "gelbooru"  -> "https://img3.gelbooru.com"
                    "xbooru"    -> "https://xbooru.com"
                    "tbib"      -> "https://tbib.org"
                    else        -> ""
                }
                if (host.isNotBlank()) {
                    val querySuffix = if (key == "xbooru" && id.isNotBlank()) "?$id" else ""
                    sample = "$host/samples/$directory/sample_$baseImgName.jpg$querySuffix"
                }
            }

            if (sample.startsWith("/") && !sample.startsWith("//") && customBaseUrl.isNotBlank()) {
                sample = "${customBaseUrl.trimEnd('/')}$sample"
            }
            if (sample.startsWith("//")) {
                sample = "https:$sample"
            }

            if (key == "xbooru" && id.isNotBlank() && !sample.contains("?")) {
                sample = "$sample?$id"
            }

            if (sample.isBlank() || sample == "null") {
                sample = fileUrl
            }

            if (preview.isBlank() || preview == "null" || preview.endsWith("/") || preview.contains("thumbnail_.jpg") || preview.contains("/thumbnails//")) {
                preview = sample
            }

            var tags = ""
            val tagsObj = o.optJSONObject("tags")
            if (tagsObj != null) {
                val tagList = mutableListOf<String>()
                val tagKeys = tagsObj.keys()
                while (tagKeys.hasNext()) {
                    val cat = tagKeys.next()
                    val catArray = tagsObj.optJSONArray(cat)
                    if (catArray != null) {
                        for (j in 0 until catArray.length()) {
                            val t = catArray.optString(j)
                            if (t.isNotBlank()) tagList.add(t)
                        }
                    }
                }
                tags = tagList.joinToString(" ")
            }
            if (tags.isBlank()) {
                tags = o.optString("tags")
                    .ifBlank { o.optString("tag_string") }
                    .ifBlank { o.optString("tag_string_general") }
            }

            if (noAi && com.booru.app.data.AiFilter.isAiGeneratedPost(tags)) {
                continue
            }

            val scoreObj = o.optJSONObject("score")
            val score = scoreObj?.optInt("total", 0) ?: o.optInt("score", 0)

            val width = fileObj?.optInt("width", 0)?.takeIf { it > 0 }
                ?: o.optInt("width", 0).takeIf { it > 0 }
                ?: o.optInt("image_width", 0)
            val height = fileObj?.optInt("height", 0)?.takeIf { it > 0 }
                ?: o.optInt("height", 0).takeIf { it > 0 }
                ?: o.optInt("image_height", 0)

            val createdAt: Long = when {
                o.has("created_at") -> TimestampParser.parseToEpochSeconds(o.opt("created_at"))
                o.has("change")     -> TimestampParser.parseToEpochSeconds(o.opt("change"))
                o.has("date")       -> TimestampParser.parseToEpochSeconds(o.opt("date"))
                else                -> 0L
            }

            val customMatch = customSources.find { it.key == key || it.id == key }
                ?: customSources.find { it.name.equals(key, ignoreCase = true) }
            val resolvedSourceId = customMatch?.id ?: key

            val media = RemoteMedia(
                id = id,
                url = fileUrl,
                preview = preview,
                sample = sample,
                tags = tags.trim(),
                score = score,
                source = getSourceDisplayName(key, customSources),
                rating = ratingCode,
                width = width,
                height = height,
                createdAt = createdAt,
                sourceId = resolvedSourceId
            )

            results.add(media)
        }

        return results
    }
}
