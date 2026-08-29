package com.example.boorugallery

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RemoteMedia(
    val url: String,
    val preview: String,
    val sample: String = "",
    val tags: String,
    val score: Int,
    val source: String,
    val rating: String,
    val id: String = "",
    val width: Int = 0,
    val height: Int = 0
) {
    val tagList: List<String> by lazy {
        tags.split(" ").map { it.trim() }.filter { it.isNotBlank() }
    }

    val isVideo: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".mkv") || clean.endsWith(".mov")
        }

    val isGif: Boolean
        get() {
            val clean = url.substringBefore("?").lowercase()
            return clean.endsWith(".gif")
        }
}

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
    val danbooruLogin: String = "",
    val danbooruApiKey: String = ""
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
    message: String = "HTTP $statusCode from $sourceKey"
) : BooruException(message)

class BooruRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val TAG = "BooruRepo"
        val EXPLICIT_RATINGS = setOf("e", "explicit", "q", "questionable")
        val SAFE_RATINGS = setOf("s", "safe", "g", "general")
        val MEDIA_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "avif", "mp4", "webm", "mkv", "mov", "zip")

        const val SOURCE_ALL = "All sources"
        const val SOURCE_SAFEBOORU = "Safebooru"
        const val SOURCE_YANDE = "Yande.re"
        const val SOURCE_RULE34 = "Rule34"
        const val SOURCE_GELBOORU = "Gelbooru"
        const val SOURCE_XBOORU = "Xbooru"
        const val SOURCE_TBIB = "TBIB"
        const val SOURCE_KONACHAN = "Konachan"

        val AVAILABLE_SOURCES = listOf(
            SOURCE_ALL,
            SOURCE_RULE34,
            SOURCE_GELBOORU,
            SOURCE_XBOORU,
            SOURCE_TBIB,
            SOURCE_YANDE,
            SOURCE_KONACHAN,
            SOURCE_SAFEBOORU
        )
    }

    suspend fun search(
        source: String,
        tags: String,
        safeMode: Boolean,
        excludeSafe: Boolean = false,
        noAi: Boolean = false,
        page: Int = 0,
        sortOrder: SortOrder = SortOrder.NEWEST,
        credentials: BooruCredentials = BooruCredentials()
    ): List<RemoteMedia> = withContext(Dispatchers.IO) {
        val targets = when (source) {
            SOURCE_SAFEBOORU -> if (excludeSafe) emptyList() else listOf("safebooru")
            SOURCE_YANDE     -> listOf("yande")
            SOURCE_RULE34    -> listOf("rule34")
            SOURCE_GELBOORU  -> listOf("gelbooru")
            SOURCE_XBOORU    -> listOf("xbooru")
            SOURCE_TBIB      -> listOf("tbib")
            SOURCE_KONACHAN  -> listOf("konachan")
            else             -> if (excludeSafe) listOf("rule34", "gelbooru", "xbooru") else listOf("rule34", "gelbooru", "xbooru", "tbib", "yande")
        }

        if (targets.isEmpty() && excludeSafe && source == SOURCE_SAFEBOORU) {
            return@withContext emptyList()
        }

        val errors = mutableListOf<String>()
        val allResults = mutableListOf<RemoteMedia>()
        var firstAuthEx: BooruAuthException? = null

        for (key in targets) {
            try {
                val list = requestSource(key, tags.trim(), safeMode, excludeSafe, noAi, page, sortOrder, credentials)
                allResults.addAll(list)
            } catch (auth: BooruAuthException) {
                Log.e(TAG, "[$key] Auth Error: ${auth.message}", auth)
                if (firstAuthEx == null) firstAuthEx = auth
                errors.add("${getSourceDisplayName(key)}: ${auth.message}")
            } catch (e: Exception) {
                Log.e(TAG, "[$key] Error: ${e.message}", e)
                errors.add("${getSourceDisplayName(key)}: ${e.message ?: "Load failed"}")
            }
        }

        if (allResults.isEmpty()) {
            if (firstAuthEx != null && (targets.size == 1 || errors.size == targets.size)) {
                throw firstAuthEx
            }
            if (errors.isNotEmpty()) {
                throw BooruException(errors.joinToString("\n"))
            }
        }

        if (source == SOURCE_ALL && sortOrder == SortOrder.NEWEST && allResults.isNotEmpty()) {
            allResults.sortedByDescending { it.score }
        } else {
            allResults
        }
    }

    suspend fun getTagSuggestions(source: String, query: String): List<TagSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.length < 2) return@withContext emptyList()

        val endpointKey = when (source) {
            SOURCE_RULE34, SOURCE_XBOORU -> "rule34"
            SOURCE_GELBOORU -> "gelbooru"
            SOURCE_YANDE -> "yande"
            else -> "safebooru"
        }

        runCatching {
            when (endpointKey) {
                "rule34" -> {
                    val url = "https://api.rule34.xxx/autocomplete.php?q=$q"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string()?.trim() ?: return@use emptyList()
                        if (body.startsWith("[")) {
                            val arr = JSONArray(body)
                            (0 until arr.length()).mapNotNull { i ->
                                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                                val v = o.optString("value").ifBlank { o.optString("label") }
                                if (v.isNotBlank()) TagSuggestion(value = v, label = o.optString("label", v)) else null
                            }
                        } else emptyList()
                    }
                }
                "safebooru" -> {
                    val url = "https://safebooru.org/autocomplete.php?q=$q"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string()?.trim() ?: return@use emptyList()
                        if (body.startsWith("[")) {
                            val arr = JSONArray(body)
                            (0 until arr.length()).mapNotNull { i ->
                                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                                val v = o.optString("value").ifBlank { o.optString("label") }
                                if (v.isNotBlank()) TagSuggestion(value = v, label = o.optString("label", v)) else null
                            }
                        } else emptyList()
                    }
                }
                "gelbooru" -> {
                    val url = "https://gelbooru.com/index.php?page=dapi&s=tag&q=index&json=1&name_pattern=%$q%&limit=8"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string()?.trim() ?: return@use emptyList()
                        val arr = when {
                            body.startsWith("[") -> JSONArray(body)
                            body.startsWith("{") -> JSONObject(body).optJSONArray("tag")
                            else -> null
                        } ?: return@use emptyList()

                        (0 until arr.length()).mapNotNull { i ->
                            val o = arr.optJSONObject(i) ?: return@mapNotNull null
                            val name = o.optString("name").ifBlank { o.optString("tag") }
                            val count = o.optInt("count", 0)
                            if (name.isNotBlank()) TagSuggestion(value = name, label = if (count > 0) "$name ($count)" else name, count = count) else null
                        }
                    }
                }
                "yande" -> {
                    val url = "https://yande.re/tag.json?name=$q*&limit=8"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { res ->
                        val body = res.body?.string()?.trim() ?: return@use emptyList()
                        if (body.startsWith("[")) {
                            val arr = JSONArray(body)
                            (0 until arr.length()).mapNotNull { i ->
                                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                                val name = o.optString("name")
                                val count = o.optInt("count", 0)
                                if (name.isNotBlank()) TagSuggestion(value = name, label = if (count > 0) "$name ($count)" else name, count = count) else null
                            }
                        } else emptyList()
                    }
                }
                else -> emptyList()
            }
        }.getOrElse {
            Log.e(TAG, "Autocomplete error: ${it.message}")
            emptyList()
        }
    }

    private fun normalizeUserTags(raw: String): String {
        return raw.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun buildTagQuery(userTags: String, safe: Boolean, excludeSafe: Boolean, noAi: Boolean, key: String, sortOrder: SortOrder): String {
        val cleaned = normalizeUserTags(userTags)
        val parts = mutableListOf<String>()
        if (cleaned.isNotBlank()) {
            parts.add(cleaned)
        }

        if (safe) {
            when (key) {
                "yande", "konachan" -> parts.add("rating:s")
                "gelbooru"          -> parts.add("rating:general")
                "rule34", "xbooru", "tbib" -> parts.add("rating:safe")
                "safebooru"         -> Unit
            }
        } else if (excludeSafe) {
            when (key) {
                "yande", "konachan" -> parts.add("-rating:s")
                "gelbooru"          -> parts.add("-rating:general")
                "rule34", "xbooru", "tbib" -> parts.add("-rating:safe")
                "safebooru"         -> Unit
            }
        }

        if (noAi) {
            when (key) {
                "gelbooru", "rule34", "xbooru", "tbib", "safebooru" -> {
                    parts.add("-ai_generated")
                    parts.add("-novelai")
                }
                "yande", "konachan" -> {
                    parts.add("-ai_generated")
                }
            }
        }

        when (sortOrder) {
            SortOrder.SCORE -> {
                when (key) {
                    "yande", "konachan" -> parts.add("order:score")
                    "gelbooru", "rule34", "xbooru", "tbib", "safebooru" -> parts.add("sort:score:desc")
                }
            }
            SortOrder.RANDOM -> {
                when (key) {
                    "yande", "konachan" -> parts.add("order:random")
                    "gelbooru", "rule34", "xbooru", "tbib", "safebooru" -> parts.add("sort:random")
                }
            }
            SortOrder.NEWEST -> Unit
        }

        return parts.joinToString(" ")
    }

    private fun requestSource(
        key: String,
        userTags: String,
        safe: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean,
        page: Int,
        sortOrder: SortOrder,
        credentials: BooruCredentials
    ): List<RemoteMedia> {
        val tagQuery = buildTagQuery(userTags, safe, excludeSafe, noAi, key, sortOrder)

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

        val urlBuilder = baseUrl.toHttpUrl().newBuilder().apply {
            if (tagQuery.isNotBlank()) addQueryParameter("tags", tagQuery)
            addQueryParameter("limit", "40")

            when (key) {
                "yande", "konachan" -> addQueryParameter("page", (page + 1).toString())
                "safebooru", "gelbooru", "rule34", "xbooru", "tbib" -> addQueryParameter("pid", page.toString())
            }

            if (key == "rule34") {
                if (credentials.rule34UserId.isNotBlank() && credentials.rule34ApiKey.isNotBlank()) {
                    addQueryParameter("user_id", credentials.rule34UserId.trim())
                    addQueryParameter("api_key", credentials.rule34ApiKey.trim())
                }
            } else if (key == "gelbooru") {
                if (credentials.gelbooruUserId.isNotBlank() && credentials.gelbooruApiKey.isNotBlank()) {
                    addQueryParameter("user_id", credentials.gelbooruUserId.trim())
                    addQueryParameter("api_key", credentials.gelbooruApiKey.trim())
                }
            }
        }

        val fullUrl = urlBuilder.build()
        Log.d(TAG, "[$key] GET $fullUrl")

        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 BooruClient/1.0"

        val reqBuilder = Request.Builder()
            .url(fullUrl)
            .header("User-Agent", userAgent)

        val req = reqBuilder.build()

        return client.newCall(req).execute().use { response ->
            val code = response.code
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (code == 401 || code == 403) {
                    throw BooruAuthException(sourceKey = key, statusCode = code, message = "Authentication required for ${getSourceDisplayName(key)} (HTTP $code)")
                }
                throw BooruHttpException(sourceKey = key, statusCode = code, message = "HTTP $code from ${getSourceDisplayName(key)}")
            }

            if (body.contains("<title>401 Unauthorized</title>") ||
                body.contains("401 Unauthorized") ||
                body.contains("User-Id or Api-Key is incorrect") ||
                body.contains("Access denied")
            ) {
                throw BooruAuthException(sourceKey = key, statusCode = 401, message = "Invalid API Key or Access Denied for ${getSourceDisplayName(key)}")
            }

            parseResponse(key, body, noAi)
        }
    }

    private fun parseResponse(key: String, body: String, noAi: Boolean): List<RemoteMedia> {
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

            val rating = when (o.optString("rating").lowercase()) {
                "s", "safe", "general", "g" -> "safe"
                "q", "questionable", "sensitive" -> "questionable"
                "e", "explicit" -> "explicit"
                else -> "safe"
            }

            var fileUrl = o.optString("file_url")
                .ifBlank { o.optString("fileUrl") }
                .ifBlank { o.optString("jpeg_url") }
                .ifBlank { o.optString("high_res_url") }

            val directory = o.optString("directory")
            val image = o.optString("image")
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

            if (fileUrl.isBlank()) continue

            if (fileUrl.startsWith("//")) {
                fileUrl = "https:$fileUrl"
            }

            val isVideo = fileUrl.lowercase().endsWith(".mp4") || fileUrl.lowercase().endsWith(".webm")
            val isGif = fileUrl.lowercase().endsWith(".gif")

            var preview = o.optString("preview_url")
                .ifBlank { o.optString("previewUrl") }
                .ifBlank { o.optString("preview_file_url") }

            if (preview.isBlank() && directory.isNotBlank() && image.isNotBlank()) {
                val baseImgName = image.substringBeforeLast(".")
                val host = when (key) {
                    "safebooru" -> "https://safebooru.org"
                    "gelbooru"  -> "https://img3.gelbooru.com"
                    "rule34"    -> "https://api-cdn.rule34.xxx"
                    "xbooru"    -> "https://xbooru.com"
                    "tbib"      -> "https://tbib.org"
                    else        -> ""
                }
                if (host.isNotBlank()) {
                    preview = if (key == "tbib") {
                        "$host/thumbnails/$directory/thumbnail_$image"
                    } else {
                        "$host/thumbnails/$directory/thumbnail_$baseImgName.jpg"
                    }
                }
            }

            if (preview.startsWith("//")) {
                preview = "https:$preview"
            }

            var sample = o.optString("sample_url")
                .ifBlank { o.optString("sampleUrl") }
                .ifBlank { o.optString("large_file_url") }

            if (isGif) {
                sample = fileUrl
            }

            val hasSample = o.optBoolean("sample", false) || o.optInt("sample", 0) == 1
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
                    val baseImgName = if (isVideo) "${image.substringBeforeLast(".")}.jpg" else "sample_$image"
                    sample = "$host/samples/$directory/$baseImgName"
                }
            }

            if (sample.isBlank()) {
                sample = if (isVideo) preview.ifBlank { fileUrl } else fileUrl
            }

            if (sample.startsWith("//")) {
                sample = "https:$sample"
            }

            val rawTags = o.optString("tags").trim()
            if (noAi) {
                val tagList = rawTags.split(" ", ",").map { it.trim().lowercase() }
                if (tagList.any { it == "ai_generated" || it == "novelai" || it == "ai" }) continue
            }

            results.add(
                RemoteMedia(
                    url = fileUrl,
                    preview = preview.ifBlank { sample },
                    sample = sample,
                    tags = rawTags,
                    score = o.optInt("score", 0),
                    source = getSourceDisplayName(key),
                    rating = rating,
                    id = "${key}_${o.optString("id")}",
                    width = o.optInt("width", 0),
                    height = o.optInt("height", 0)
                )
            )
        }

        return results
    }

    fun getSourceDisplayName(key: String): String {
        return when (key) {
            "safebooru" -> SOURCE_SAFEBOORU
            "yande"     -> SOURCE_YANDE
            "rule34"    -> SOURCE_RULE34
            "gelbooru"  -> SOURCE_GELBOORU
            "xbooru"    -> SOURCE_XBOORU
            "tbib"      -> SOURCE_TBIB
            "konachan"  -> SOURCE_KONACHAN
            else        -> key.replaceFirstChar { it.uppercase() }
        }
    }
}
