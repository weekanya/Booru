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
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import com.booru.app.data.network.NetworkClient
import com.booru.app.data.network.await
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

class BooruTimeoutException(
    val sourceKey: String,
    message: String = "Request to $sourceKey timed out"
) : BooruException(message)

class BooruNetworkException(cause: Throwable) : BooruException(cause.message ?: "Network unavailable", cause)

data class SearchPage(
    val items: List<RemoteMedia>,
    val rawCount: Int,
    val failedSources: List<String> = emptyList()
)

class BooruRepository(
    private val client: OkHttpClient = NetworkClient.baseClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
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
        private const val MAX_CONCURRENT_REQUESTS = 8
        private const val MAX_RETRY_AFTER_SECONDS = 2
        private const val SINGLE_SOURCE_TIMEOUT_MS = 9000L
        private const val MULTI_SOURCE_TIMEOUT_MS = 4500L
        private const val DANBOORU_FREE_TAG_LIMIT = 2

        val EXPLICIT_RATINGS = setOf("e", "explicit", "q", "questionable")
        val SAFE_RATINGS = setOf("s", "safe", "g", "general")
        val MEDIA_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif", "mp4", "webm", "mkv", "mov", "zip")

        const val SOURCE_ALL = "All sources"
        const val SOURCE_SAFEBOORU = "Safebooru"
        const val SOURCE_YANDE = "Yande.re"
        const val SOURCE_RULE34 = "Rule34"
        const val SOURCE_GELBOORU = "Gelbooru"
        const val SOURCE_REALBOORU = "Realbooru"
        const val SOURCE_TBIB = "TBIB"
        const val SOURCE_KONACHAN = "Konachan"

        val AVAILABLE_SOURCES = listOf(
            SOURCE_ALL,
            SOURCE_RULE34,
            SOURCE_GELBOORU,
            SOURCE_REALBOORU,
            SOURCE_TBIB,
            SOURCE_YANDE,
            SOURCE_KONACHAN,
            SOURCE_SAFEBOORU
        )

        fun isAiGeneratedPost(tags: String): Boolean {
            return com.booru.app.data.AiFilter.isAiGeneratedPost(tags)
        }

        fun getSourceDisplayName(key: String, customSources: List<CustomBooruSource> = emptyList()): String {
            val custom = customSources.find { it.id == key || it.key == key }
            if (custom != null) return custom.name
            if (key == SOURCE_ALL || key.equals("all sources", ignoreCase = true)) return "Recommendations"
            return when (key.lowercase()) {
                "rule34"    -> SOURCE_RULE34
                "gelbooru"  -> SOURCE_GELBOORU
                "realbooru" -> SOURCE_REALBOORU
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
        customSources: List<CustomBooruSource> = emptyList(),
        limit: Int = PAGE_SIZE,
        excludeSources: Set<String> = emptySet()
    ): List<RemoteMedia> = searchPage(
        source, tags, safeMode, excludeSafe, noAi, page, sortOrder, contentTypes, credentials, customSources, limit, excludeSources
    ).items

    suspend fun searchPage(
        source: String,
        tags: String,
        safeMode: Boolean,
        excludeSafe: Boolean = false,
        noAi: Boolean = false,
        page: Int = 0,
        sortOrder: SortOrder = SortOrder.NEWEST,
        contentTypes: Set<ContentType> = emptySet(),
        credentials: BooruCredentials = BooruCredentials(),
        customSources: List<CustomBooruSource> = emptyList(),
        limit: Int = PAGE_SIZE,
        excludeSources: Set<String> = emptySet()
    ): SearchPage = withContext(Dispatchers.IO) {
        val customMatch = customSources.find { it.id == source || it.key == source }
        val wantsOnlyVideos = contentTypes.contains(ContentType.VIDEOS) && !contentTypes.contains(ContentType.PHOTOS) && !contentTypes.contains(ContentType.GIFS)
        val allTargets = when {
            customMatch != null -> if (!customMatch.enabled) emptyList() else listOf(customMatch.id)
            source == SOURCE_SAFEBOORU -> if (excludeSafe || wantsOnlyVideos) emptyList() else listOf("safebooru")
            source == SOURCE_YANDE     -> if (wantsOnlyVideos) emptyList() else listOf("yande")
            source == SOURCE_RULE34    -> listOf("rule34")
            source == SOURCE_GELBOORU  -> listOf("gelbooru")
            source == SOURCE_REALBOORU -> listOf("realbooru")
            source == SOURCE_TBIB      -> if (wantsOnlyVideos) emptyList() else listOf("tbib")
            source == SOURCE_KONACHAN  -> if (wantsOnlyVideos) emptyList() else listOf("konachan")
            else -> {
                if (wantsOnlyVideos) {
                    listOf("rule34", "gelbooru", "realbooru")
                } else if (excludeSafe) {
                    listOf("rule34", "gelbooru", "realbooru", "tbib", "yande", "konachan")
                } else {
                    listOf("rule34", "gelbooru", "realbooru", "tbib", "yande", "konachan", "safebooru")
                }
            }
        }
        val keyless = buildSet {
            if (allTargets.size > 1) {
                if (credentials.rule34ApiKey.isBlank()) add("rule34")
                if (credentials.gelbooruApiKey.isBlank()) add("gelbooru")
            }
        }
        val targets = allTargets.filterNot { it in excludeSources || it in keyless }.ifEmpty { allTargets }

        if (targets.isEmpty()) {
            return@withContext SearchPage(emptyList(), 0)
        }

        val timeoutMs = if (targets.size == 1) SINGLE_SOURCE_TIMEOUT_MS else MULTI_SOURCE_TIMEOUT_MS
        val pageLimit = limit.coerceIn(1, PAGE_SIZE)
        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        val outcomes = coroutineScope {
            targets.map { key ->
                async {
                    semaphore.withPermit {
                        try {
                            val list = withTimeoutOrNull(timeoutMs) {
                                requestSourceWithRetry(key, tags.trim(), safeMode, excludeSafe, noAi, page, sortOrder, contentTypes, credentials, customSources, pageLimit)
                            } ?: throw BooruTimeoutException(key, "${getSourceDisplayName(key, customSources)} timed out")
                            key to Result.success(list)
                        } catch (c: kotlinx.coroutines.CancellationException) {
                            throw c
                        } catch (e: Exception) {
                            Log.e(TAG, "[$key] Error: ${sanitizeErrorMessage(e.message)}")
                            key to Result.failure(e)
                        }
                    }
                }
            }.awaitAll()
        }

        val allResults = mutableListOf<RemoteMedia>()
        val failures = mutableListOf<Pair<String, Throwable>>()
        for ((key, res) in outcomes) {
            res.onSuccess { allResults.addAll(it) }
            res.onFailure { failures.add(key to it) }
        }

        if (allResults.isEmpty() && failures.isNotEmpty()) {
            val errors = failures.map { it.second }
            val auth = errors.filterIsInstance<BooruAuthException>().firstOrNull()
            if (errors.size == targets.size) {
                val network = errors.firstOrNull { it is IOException || it is BooruTimeoutException }
                when {
                    auth != null && (targets.size == 1 || errors.all { it is BooruAuthException }) -> throw auth
                    targets.size == 1 -> throw errors.first()
                    network != null && errors.all { it is IOException || it is BooruTimeoutException || it is BooruAuthException } ->
                        throw BooruNetworkException(network)
                    auth != null -> throw auth
                    else -> throw BooruException(errors.joinToString("\n") { sanitizeErrorMessage(it.message ?: "Load failed") })
                }
            }
        }

        val filteredResults = filterMediaList(allResults, safeMode, excludeSafe, noAi)

        if (targets.size > 1 && filteredResults.isNotEmpty()) {
            if (sortOrder == SortOrder.RANDOM) {
                filteredResults.shuffle()
            } else {
                val comparator = if (sortOrder == SortOrder.SCORE) {
                    compareByDescending<RemoteMedia> { it.score }
                } else {
                    compareByDescending<RemoteMedia> { it.createdAt > 0 }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.score }
                }
                val queues = filteredResults
                    .groupBy { it.source }
                    .values
                    .map { ArrayDeque(it.sortedWith(comparator)) }
                val merged = ArrayList<RemoteMedia>(filteredResults.size)
                while (queues.any { it.isNotEmpty() }) {
                    for (queue in queues) queue.removeFirstOrNull()?.let { merged.add(it) }
                }
                filteredResults.clear()
                filteredResults.addAll(merged)
            }
        }
        SearchPage(
            items = filteredResults.distinctBy { it.mediaKey },
            rawCount = allResults.size,
            failedSources = failures.map { it.first }
        )
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
        val q = query.trim().lowercase().replace(' ', '_')
        if (q.length < 2) return@withContext emptyList()

        val customMatch = customSources.find { it.id == source || it.key == source }
        if (customMatch != null && !customMatch.enabled) {
            return@withContext emptyList()
        }

        val suggestions = try {
            if (customMatch != null) {
                val base = customMatch.cleanBaseUrl
                when (customMatch.engine) {
                    BooruEngine.DANBOORU -> {
                        if (base.contains("e621") || base.contains("e926")) {
                            fetchE621Suggestions(base, q)
                        } else {
                            fetchDanbooruSuggestions(base, q)
                        }
                    }
                    BooruEngine.MOEBOORU -> fetchMoebooruSuggestions(base, q)
                    BooruEngine.GELBOORU -> fetchGelbooruSuggestions(base, q)
                }
            } else {
                when {
                    source == SOURCE_GELBOORU -> fetchGelbooruSuggestions("https://gelbooru.com", q)
                    source == SOURCE_YANDE -> fetchMoebooruSuggestions("https://yande.re", q)
                    source == SOURCE_KONACHAN -> fetchMoebooruSuggestions("https://konachan.net", q)
                    source == SOURCE_SAFEBOORU -> withGelbooruCategories(q) { fetchRule34StyleSuggestions("https://safebooru.org/autocomplete.php", q) }
                    else -> withGelbooruCategories(q) { fetchRule34StyleSuggestions("https://api.rule34.xxx/autocomplete.php", q) }
                }
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Log.w(TAG, "Tag suggestions failed: ${sanitizeErrorMessage(e.message)}")
            emptyList()
        }

        suggestions
            .filter { it.value.isNotBlank() }
            .distinctBy { it.value.lowercase() }
            .sortedWith(
                compareByDescending<TagSuggestion> { it.value.equals(q, ignoreCase = true) }
                    .thenByDescending { it.value.startsWith(q, ignoreCase = true) }
                    .thenByDescending { it.count }
            )
            .take(15)
    }

    private suspend fun fetchJsonArray(url: HttpUrl, referer: String? = null): JSONArray? {
        val builder = Request.Builder().url(url).header("User-Agent", USER_AGENT)
        if (referer != null) builder.header("Referer", referer)
        return client.newCall(builder.build()).await().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string()?.trim().orEmpty()
            if (!body.startsWith("[")) return@use null
            runCatching { JSONArray(body) }.getOrNull()
        }
    }

    private fun parseAutocompleteArray(arr: JSONArray?, countKeys: List<String>, typeKeys: List<String>): List<TagSuggestion> {
        if (arr == null) return emptyList()
        val list = mutableListOf<TagSuggestion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val value = o.optString("value").ifBlank { o.optString("name") }
            if (value.isBlank()) continue
            val label = o.optString("label").ifBlank { value }
            val count = countKeys.firstNotNullOfOrNull { k ->
                if (o.has(k)) o.optString(k).replace(",", "").toIntOrNull() else null
            } ?: 0
            val type = typeKeys.firstNotNullOfOrNull { k ->
                o.optString(k).takeIf { it.isNotBlank() && it != "null" }
            }.orEmpty()
            list.add(TagSuggestion(value, label, count, type))
        }
        return list
    }

    private fun categoryName(type: Int): String = when (type) {
        1 -> "artist"
        3 -> "copyright"
        4 -> "character"
        5 -> "meta"
        else -> "general"
    }

    private suspend fun fetchDanbooruSuggestions(base: String, q: String): List<TagSuggestion> {
        val url = "$base/autocomplete.json".toHttpUrl().newBuilder()
            .addQueryParameter("search[query]", q)
            .addQueryParameter("search[type]", "tag_query")
            .addQueryParameter("limit", "15")
            .build()
        return parseAutocompleteArray(fetchJsonArray(url), listOf("post_count"), listOf("category")).map { s ->
            s.type.toIntOrNull()?.let { s.copy(type = categoryName(it)) } ?: s
        }
    }

    private suspend fun fetchE621Suggestions(base: String, q: String): List<TagSuggestion> {
        val url = "$base/tags/autocomplete.json".toHttpUrl().newBuilder()
            .addQueryParameter("search[name_matches]", q)
            .addQueryParameter("limit", "15")
            .build()
        val arr = fetchJsonArray(url, "$base/") ?: return emptyList()
        val list = mutableListOf<TagSuggestion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            val type = when (o.optInt("category", 0)) {
                1 -> "artist"
                3 -> "copyright"
                4 -> "character"
                5 -> "species"
                7 -> "meta"
                else -> "general"
            }
            list.add(TagSuggestion(name, name, o.optInt("post_count", 0), type))
        }
        return list
    }

    private suspend fun fetchMoebooruSuggestions(base: String, q: String): List<TagSuggestion> {
        val url = "$base/tag.json".toHttpUrl().newBuilder()
            .addQueryParameter("name", "$q*")
            .addQueryParameter("order", "count")
            .addQueryParameter("limit", "15")
            .build()
        val arr = fetchJsonArray(url, "$base/") ?: return emptyList()
        val list = mutableListOf<TagSuggestion>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isNotBlank()) list.add(TagSuggestion(name, name, o.optInt("count", 0), categoryName(o.optInt("type", 0))))
        }
        return list
    }

    private suspend fun fetchGelbooruSuggestions(base: String, q: String): List<TagSuggestion> {
        val url = "$base/index.php".toHttpUrl().newBuilder()
            .addQueryParameter("page", "autocomplete2")
            .addQueryParameter("term", q)
            .build()
        return parseAutocompleteArray(fetchJsonArray(url, "$base/"), listOf("post_count", "total"), listOf("category", "type"))
    }

    private suspend fun fetchRule34StyleSuggestions(endpoint: String, q: String): List<TagSuggestion> {
        val url = endpoint.toHttpUrl().newBuilder().addQueryParameter("q", q).build()
        return parseAutocompleteArray(fetchJsonArray(url), listOf("total", "post_count"), listOf("type", "category"))
    }

    private suspend fun withGelbooruCategories(
        q: String,
        primary: suspend () -> List<TagSuggestion>
    ): List<TagSuggestion> = coroutineScope {
        val categories = async {
            runCatching {
                fetchGelbooruSuggestions("https://gelbooru.com", q)
                    .filter { it.type.isNotBlank() }
                    .associate { it.value.lowercase() to it.type }
            }.getOrDefault(emptyMap())
        }
        val list = primary()
        val catMap = categories.await()
        list.map { s ->
            if (s.type.isBlank()) catMap[s.value.lowercase()]?.let { s.copy(type = it) } ?: s else s
        }
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

        val custom = customSources.find { it.id == key || it.key == key }

        if (contentTypes.isNotEmpty()) {
            val wantsPhotos = contentTypes.contains(ContentType.PHOTOS)
            val wantsVideos = contentTypes.contains(ContentType.VIDEOS)
            val wantsGifs = contentTypes.contains(ContentType.GIFS)

            if (wantsGifs && !wantsVideos && !wantsPhotos) {
                if (!cleaned.contains("animated")) {
                    when (key) {
                        "rule34", "gelbooru", "safebooru", "tbib", "realbooru" -> parts.add("animated")
                        else -> if (custom != null) parts.add("animated")
                    }
                }
            } else if (wantsVideos && !wantsGifs && !wantsPhotos) {
                if (!cleaned.contains("video")) {
                    when (key) {
                        "rule34", "gelbooru", "realbooru" -> parts.add("video")
                        else -> {
                            if (custom != null) {
                                when (custom.engine) {
                                    BooruEngine.DANBOORU -> parts.add("mp4")
                                    else -> parts.add("video")
                                }
                            }
                        }
                    }
                }
            } else if (wantsPhotos && !wantsVideos && !wantsGifs) {
                when (key) {
                    "rule34", "gelbooru", "tbib", "realbooru" -> {
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
                        "rule34", "gelbooru", "safebooru", "tbib", "realbooru" -> parts.add("animated")
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
                    "rule34", "tbib", "realbooru" -> parts.add("rating:safe")
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
                    "rule34", "tbib", "realbooru" -> parts.add("-rating:safe")
                    "safebooru"         -> Unit
                }
            }
        }

        val isStrictTagLimit = key == "gelbooru" || key == "safebooru"

        if (noAi && !isStrictTagLimit) {
            for (queryTag in com.booru.app.data.AiFilter.EXCLUDE_QUERY_TAGS) {
                if (!cleaned.contains(queryTag)) {
                    parts.add(queryTag)
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
                        "rule34", "tbib" -> parts.add("sort:score:desc")
                        "gelbooru", "safebooru" -> {
                            if (parts.size < 2) parts.add("sort:score:desc")
                        }
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
                        "rule34" -> parts.add("sort:random")
                        "gelbooru" -> {
                            if (parts.size < 2) parts.add("sort:random")
                        }
                        "safebooru", "realbooru", "tbib" -> Unit
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
                            "rule34", "tbib", "realbooru" -> parts.add("sort:id:desc")
                            "gelbooru", "safebooru" -> {
                                if (parts.size < 2) {
                                    parts.add("sort:id:desc")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (custom != null && custom.engine == BooruEngine.DANBOORU && !custom.cleanBaseUrl.contains("e621") && !custom.cleanBaseUrl.contains("e926")) {
            return limitDanbooruTags(cleaned, parts)
        }

        return parts.joinToString(" ")
    }

    private fun limitDanbooruTags(cleaned: String, parts: List<String>): String {
        val result = cleaned.split(' ').filter { it.isNotBlank() }.toMutableList()
        val extras = parts.drop(if (cleaned.isNotBlank()) 1 else 0)
            .sortedBy { e ->
                when {
                    e.startsWith("rating:") || e.startsWith("-rating:") -> 0
                    e.startsWith("order:") -> 1
                    else -> 2
                }
            }
        for (extra in extras) {
            if (result.size >= DANBOORU_FREE_TAG_LIMIT) break
            result.add(extra)
        }
        return result.joinToString(" ")
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
        customSources: List<CustomBooruSource> = emptyList(),
        limit: Int = PAGE_SIZE
    ): List<RemoteMedia> {
        val maxAttempts = 2
        var lastException: Exception? = null

        for (attempt in 0 until maxAttempts) {
            val retryDelay: Long
            try {
                return requestSource(key, userTags, safe, excludeSafe, noAi, page, sortOrder, contentTypes, credentials, customSources, limit)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (auth: BooruAuthException) {
                throw auth
            } catch (http: BooruHttpException) {
                if (http.statusCode in 400..499 && http.statusCode != 408 && http.statusCode != 429) {
                    throw http
                }
                lastException = http
                retryDelay = if (http.statusCode == 429 && http.retryAfterSec != null) {
                    http.retryAfterSec.coerceIn(1, MAX_RETRY_AFTER_SECONDS) * 1000L
                } else {
                    backoffDelay(attempt)
                }
            } catch (e: SocketTimeoutException) {
                throw e
            } catch (e: java.net.UnknownHostException) {
                throw e
            } catch (e: IOException) {
                lastException = e
                retryDelay = backoffDelay(attempt)
            }
            if (attempt < maxAttempts - 1) delay(retryDelay)
        }
        throw lastException ?: BooruException("Failed to fetch from $key")
    }

    private fun backoffDelay(attempt: Int): Long =
        (400L * (1L shl attempt) + Random.nextLong(0, 100)).coerceAtMost(1500L)

    private suspend fun requestSource(
        key: String,
        userTags: String,
        safe: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean,
        page: Int,
        sortOrder: SortOrder,
        contentTypes: Set<ContentType> = emptySet(),
        credentials: BooruCredentials,
        customSources: List<CustomBooruSource> = emptyList(),
        limit: Int = PAGE_SIZE
    ): List<RemoteMedia> {
        val tagQuery = buildTagQuery(userTags, safe, excludeSafe, noAi, key, sortOrder, customSources, contentTypes)

        val custom = customSources.find { it.id == key || it.key == key }
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
                        addQueryParameter("limit", limit.toString())
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
                        addQueryParameter("limit", limit.toString())
                        addQueryParameter("page", (page + 1).toString())
                    }.build()
                }
                BooruEngine.DANBOORU -> {
                    "$base/posts.json".toHttpUrl().newBuilder().apply {
                        if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
                        addQueryParameter("limit", limit.toString())
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

            return client.newCall(req).await().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    if (code == 429) {
                        val retryAfter = parseRetryAfter(response.header("Retry-After"))
                        throw BooruHttpException(sourceKey = custom.id, statusCode = code, retryAfterSec = retryAfter, message = "Rate limited by ${custom.name}")
                    }
                    throw BooruHttpException(sourceKey = custom.id, statusCode = code, message = "HTTP $code from ${custom.name}")
                }
                parseSuccessBody(custom.id, custom.name, body, base, customSources)
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

            return client.newCall(req).await().use { response ->
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
                RealbooruHtmlParser.parse(body, false)
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
            "tbib"      -> "https://tbib.org/index.php?page=dapi&s=post&q=index&json=1"
            "gelbooru"  -> "https://gelbooru.com/index.php?page=dapi&s=post&q=index&json=1"
            "rule34"    -> "https://api.rule34.xxx/index.php?page=dapi&s=post&q=index&json=1"
            else        -> error("Unknown source: $key")
        }

        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        if (tagQuery.isNotBlank()) {
            urlBuilder.addQueryParameter("tags", tagQuery)
        }

        urlBuilder.addQueryParameter("limit", limit.toString())

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
            "tbib"      -> reqBuilder.header("Referer", "https://tbib.org/")
            "yande"     -> reqBuilder.header("Referer", "https://yande.re/")
            "konachan"  -> reqBuilder.header("Referer", "https://konachan.net/")
        }

        val req = reqBuilder.build()

        return client.newCall(req).await().use { response ->
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

            val trimmedBody = body.trimStart()
            val looksLikeData = trimmedBody.startsWith("[") || trimmedBody.startsWith("{") || trimmedBody.startsWith("<?xml") || trimmedBody.startsWith("<posts")
            if (!looksLikeData && (
                body.contains("Access denied", ignoreCase = true) ||
                body.contains("Authentication failed", ignoreCase = true) ||
                body.contains("api_key is invalid", ignoreCase = true) ||
                body.contains("invalid api key", ignoreCase = true) ||
                body.contains("user_id is invalid", ignoreCase = true) ||
                body.contains("invalid user id", ignoreCase = true) ||
                body.contains("login failed", ignoreCase = true) ||
                body.contains("Missing authentication", ignoreCase = true))
            ) {
                throw BooruAuthException(
                    sourceKey = key,
                    statusCode = 401,
                    message = "Invalid credentials for ${getSourceDisplayName(key, customSources)}"
                )
            }

            parseSuccessBody(key, getSourceDisplayName(key, customSources), body, "", customSources)
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

    private fun sanitizeErrorMessage(msg: String?): String {
        if (msg == null) return "Unknown error"
        return msg.replace(Regex("(?i)(api[-_]?key|user[-_]?id|password|login|token|secret|auth|pass)=[^&\\s]+"), "$1=[REDACTED]")
    }

    private fun parseSuccessBody(
        key: String,
        displayName: String,
        body: String,
        customBaseUrl: String,
        customSources: List<CustomBooruSource>
    ): List<RemoteMedia> {
        val trimmed = body.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("[") && !trimmed.startsWith("{") && !trimmed.startsWith("<")) {
            throw BooruException("Unexpected response from $displayName: ${sanitizeErrorMessage(trimmed.take(80))}")
        }
        if (trimmed.startsWith("<") && !trimmed.contains("<posts", ignoreCase = true) && !trimmed.contains("<post ", ignoreCase = true)) {
            throw BooruException("Unexpected response from $displayName")
        }
        return parseResponse(key, trimmed, false, customBaseUrl, customSources)
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

        val jsonArray = extractJsonPosts(trimmed)
        val results = mutableListOf<RemoteMedia>()

        for (i in 0 until jsonArray.length()) {
            val o = jsonArray.optJSONObject(i) ?: continue
            val media = parseJsonPost(o, key, customBaseUrl, customSources, noAi)
            if (media != null) {
                results.add(media)
            }
        }

        return results
    }

    private fun extractJsonPosts(trimmed: String): JSONArray {
        return when {
            trimmed.startsWith("[") -> runCatching { JSONArray(trimmed) }.getOrDefault(JSONArray())
            trimmed.startsWith("{") -> {
                val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return JSONArray()
                obj.optJSONArray("post")
                    ?: obj.optJSONArray("posts")
                    ?: obj.optJSONArray("images")
                    ?: JSONArray()
            }
            trimmed.startsWith("<") -> xmlPostsToJson(trimmed)
            else -> JSONArray()
        }
    }

    private fun xmlPostsToJson(xml: String): JSONArray {
        val arr = JSONArray()
        val attrRegex = Regex("([A-Za-z_]+)=\"([^\"]*)\"")
        val selfClosing = Regex("<post\\s([^>]*?)/?>", RegexOption.IGNORE_CASE)
        for (m in selfClosing.findAll(xml)) {
            val o = JSONObject()
            for (a in attrRegex.findAll(m.groupValues[1])) {
                o.put(a.groupValues[1], unescapeXml(a.groupValues[2]))
            }
            if (o.length() > 0) arr.put(o)
        }
        if (arr.length() > 0) return arr
        val element = Regex("<post>(.*?)</post>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val child = Regex("<([A-Za-z_]+)>([^<]*)</\\1>")
        for (m in element.findAll(xml)) {
            val o = JSONObject()
            for (c in child.findAll(m.groupValues[1])) {
                o.put(c.groupValues[1], unescapeXml(c.groupValues[2].trim()))
            }
            if (o.length() > 0) arr.put(o)
        }
        return arr
    }

    private fun unescapeXml(value: String): String = value
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    private fun extractRatingCode(o: JSONObject, danbooruRatings: Boolean = false): String {
        val raw = o.optString("rating").lowercase().trim()
        if (danbooruRatings && (raw == "s" || raw == "sensitive")) return "questionable"
        return when (raw) {
            "s", "safe", "general", "g" -> "safe"
            "q", "questionable", "sensitive" -> "questionable"
            "e", "explicit" -> "explicit"
            else -> "u"
        }
    }

    private fun JSONObject.optClean(name: String): String {
        if (isNull(name)) return ""
        val value = optString(name).trim()
        return if (value == "null") "" else value
    }

    private fun extractTags(o: JSONObject): String {
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
            if (tagList.isNotEmpty()) {
                return tagList.joinToString(" ")
            }
        }
        return o.optString("tags")
            .ifBlank { o.optString("tag_string") }
            .ifBlank { o.optString("tag_string_general") }
    }

    private fun parseJsonPost(
        o: JSONObject,
        key: String,
        customBaseUrl: String,
        customSources: List<CustomBooruSource>,
        noAi: Boolean
    ): RemoteMedia? {
        val tags = extractTags(o).trim()
        if (noAi && com.booru.app.data.AiFilter.isAiGeneratedPost(tags)) {
            return null
        }

        val customSource = customSources.find { it.id == key || it.key == key }
        val isE621 = customBaseUrl.contains("e621") || customBaseUrl.contains("e926")
        val usesDanbooruRatings = customSource?.engine == BooruEngine.DANBOORU && !isE621
        val ratingCode = extractRatingCode(o, usesDanbooruRatings)
        val fileObj = o.optJSONObject("file")
        var fileUrl = o.optClean("file_url")
            .ifBlank { o.optClean("fileUrl") }
            .ifBlank { o.optClean("jpeg_url") }
            .ifBlank { o.optClean("high_res_url") }
            .ifBlank { fileObj?.optClean("url") ?: "" }

        val id = o.optClean("id")
        val directory = o.optClean("directory")
        val image = o.optClean("image")
        val hash = o.optClean("hash")
            .ifBlank { fileObj?.optClean("md5") ?: "" }
            .ifBlank { o.optClean("md5") }
        val baseImgName = image.substringBeforeLast(".")

        if (fileUrl.isBlank() && directory.isNotBlank() && image.isNotBlank()) {
            val host = when (key) {
                "safebooru" -> "https://safebooru.org"
                "gelbooru"  -> "https://gelbooru.com"
                "rule34"    -> "https://api-cdn.rule34.xxx"
                "tbib"      -> "https://tbib.org"
                else        -> ""
            }
            if (host.isNotBlank()) {
                fileUrl = "$host/images/$directory/$image"
            } else if (customSource?.engine == BooruEngine.GELBOORU && customBaseUrl.isNotBlank()) {
                fileUrl = "${customBaseUrl.trimEnd('/')}/images/$directory/$image"
            }
        }

        if (fileUrl.isBlank() && hash.length >= 4 && isE621) {
            val ext = fileObj?.optClean("ext")?.ifBlank { null } ?: "jpg"
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
            return null
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
        var preview = o.optClean("preview_url")
            .ifBlank { o.optClean("previewUrl") }
            .ifBlank { o.optClean("preview_file_url") }
            .ifBlank { previewObj?.optClean("url") ?: "" }

        if (preview.isBlank() && directory.isNotBlank() && image.isNotBlank()) {
            val host = when (key) {
                "safebooru" -> "https://safebooru.org"
                "gelbooru"  -> "https://img3.gelbooru.com"
                "rule34"    -> "https://api-cdn.rule34.xxx"
                "tbib"      -> "https://tbib.org"
                else        -> ""
            }
            if (host.isNotBlank()) {
                preview = "$host/thumbnails/$directory/thumbnail_$baseImgName.jpg"
            } else if (customSource?.engine == BooruEngine.GELBOORU && customBaseUrl.isNotBlank()) {
                preview = "${customBaseUrl.trimEnd('/')}/thumbnails/$directory/thumbnail_$baseImgName.jpg"
            }
        }

        if (preview.isBlank() && hash.length >= 4 && isE621) {
            preview = "https://static1.e621.net/data/preview/${hash.take(2)}/${hash.substring(2, 4)}/$hash.jpg"
        }

        if (preview.startsWith("/") && !preview.startsWith("//") && customBaseUrl.isNotBlank()) {
            preview = "${customBaseUrl.trimEnd('/')}$preview"
        }
        if (preview.startsWith("//")) {
            preview = "https:$preview"
        }

        val sampleObj = o.optJSONObject("sample")
        var sample = o.optClean("sample_url")
            .ifBlank { o.optClean("sampleUrl") }
            .ifBlank { o.optClean("large_file_url") }
            .ifBlank { sampleObj?.optClean("url") ?: "" }

        if (isGif) {
            sample = fileUrl
        }

        val hasSample = o.optBoolean("sample", false) || o.optInt("sample", 0) == 1 || (sampleObj?.optBoolean("has", false) == true)
        if (sample.isBlank() && hasSample && directory.isNotBlank() && image.isNotBlank()) {
            val host = when (key) {
                "safebooru" -> "https://safebooru.org"
                "rule34"    -> "https://api-cdn.rule34.xxx"
                "gelbooru"  -> "https://img3.gelbooru.com"
                "tbib"      -> "https://tbib.org"
                else        -> ""
            }
            if (host.isNotBlank()) {
                sample = "$host/samples/$directory/sample_$baseImgName.jpg"
            }
        }

        if (sample.startsWith("/") && !sample.startsWith("//") && customBaseUrl.isNotBlank()) {
            sample = "${customBaseUrl.trimEnd('/')}$sample"
        }
        if (sample.startsWith("//")) {
            sample = "https:$sample"
        }

        if (sample.isBlank() || sample == "null") {
            sample = fileUrl
        }

        if (preview.isBlank() || preview == "null" || preview.endsWith("/") || preview.contains("thumbnail_.jpg") || preview.contains("/thumbnails//")) {
            preview = sample
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

        val resolvedSourceId = customSource?.id ?: key

        return RemoteMedia(
            id = id,
            url = fileUrl,
            preview = preview,
            sample = sample,
            tags = tags,
            score = score,
            source = getSourceDisplayName(key, customSources),
            rating = ratingCode,
            width = width,
            height = height,
            createdAt = createdAt,
            sourceId = resolvedSourceId
        )
    }
}
