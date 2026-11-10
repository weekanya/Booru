package com.booru.app.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.booru.app.data.network.NetworkClient
import com.booru.app.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object TagCategoryCache {
    private const val TAG = "TagCategoryCache"
    private const val FILE_NAME = "tag_categories.json"
    private const val BATCH_SIZE = 100
    private const val MAX_ENTRIES = 40_000
    private const val UNKNOWN = -1
    private const val FAILURE_BACKOFF_MS = 10 * 60 * 1000L
    private const val FALLBACK_BATCH = 40
    private const val FALLBACK_PARALLELISM = 4

    private val categories: MutableMap<String, Int> = java.util.Collections.synchronizedMap(LinkedHashMap())
    private val loadMutex = Mutex()
    private val writeMutex = Mutex()
    @Volatile private var loaded = false
    @Volatile private var file: File? = null
    @Volatile private var pausedUntil = 0L
    @Volatile private var primaryPausedUntil = 0L
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val fallbackChecked = ConcurrentHashMap.newKeySet<String>()

    private const val LOOKUP_USER_AGENT = "BooruApp/7.0 (Android; +https://github.com/weekanya/Booru)"
    private val TAG_ELEMENT = Regex("<tag\\s[^>]*>")
    private val ATTRIBUTE = Regex("(\\w+)=\"([^\"]*)\"")

    var version by mutableIntStateOf(0)
        private set

    var lookupFailed by mutableStateOf(false)
        private set

    fun clearBackoff() {
        pausedUntil = 0L
        primaryPausedUntil = 0L
    }

    fun init(context: Context) {
        file = File(context.applicationContext.filesDir, FILE_NAME)
    }

    fun hint(tag: String): String = when (categories[tag.lowercase()]) {
        0 -> "general"
        1 -> "artist"
        3 -> "copyright"
        4 -> "character"
        5 -> "meta"
        else -> ""
    }

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                runCatching {
                    val f = file ?: return@runCatching
                    if (!f.exists()) return@runCatching
                    val obj = JSONObject(f.readText())
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        categories[k] = obj.optInt(k, UNKNOWN)
                    }
                }.onFailure { Log.w(TAG, "Failed to read tag categories", it) }
            }
            loaded = true
        }
    }

    suspend fun resolve(tags: Collection<String>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Boolean {
        ensureLoaded()
        if (System.currentTimeMillis() < pausedUntil) return false
        val missing = tags.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && !it.contains(',') && !categories.containsKey(it) }
            .distinct()
            .filter { inFlight.add(it) }
            .toList()
        if (missing.isEmpty()) return false

        val batches = missing.chunked(BATCH_SIZE)
        var changed = false
        try {
            for ((index, batch) in batches.withIndex()) {
                val primary = if (System.currentTimeMillis() < primaryPausedUntil) null else fetchFromDanbooru(batch)
                if (primary != null) {
                    if (lookupFailed) withContext(Dispatchers.Main) { lookupFailed = false }
                    for (name in batch) categories[name] = primary[name] ?: UNKNOWN
                    changed = true
                    onProgress(index + 1, batches.size)
                    continue
                }
                if (System.currentTimeMillis() >= primaryPausedUntil) {
                    primaryPausedUntil = System.currentTimeMillis() + FAILURE_BACKOFF_MS
                }
                val pending = batch.filterNot { it in fallbackChecked }.take(FALLBACK_BATCH)
                val fallback = if (pending.isEmpty()) emptyMap() else fetchFromSafebooru(pending)
                if (fallback == null) {
                    pausedUntil = System.currentTimeMillis() + FAILURE_BACKOFF_MS
                    withContext(Dispatchers.Main) { lookupFailed = true }
                    break
                }
                fallbackChecked.addAll(fallback.keys)
                for ((name, category) in fallback) {
                    if (category != UNKNOWN) {
                        categories[name] = category
                        changed = true
                    }
                }
                val covered = batches.size == 1 && batch.all { it in fallbackChecked }
                if (lookupFailed == covered) withContext(Dispatchers.Main) { lookupFailed = !covered }
                onProgress(index + 1, batches.size)
                break
            }
        } finally {
            inFlight.removeAll(missing.toSet())
        }
        if (changed) {
            withContext(Dispatchers.Main) { version++ }
            persist()
        }
        return changed
    }

    private suspend fun fetchFromSafebooru(names: List<String>): Map<String, Int>? = coroutineScope {
        val permits = Semaphore(FALLBACK_PARALLELISM)
        val answers = names.map { name ->
            async { permits.withPermit { fetchSafebooruTag(name) } }
        }.awaitAll()
        if (answers.all { it == null }) return@coroutineScope null
        val result = HashMap<String, Int>()
        for ((name, category) in names.zip(answers)) {
            if (category != null) result[name] = category
        }
        result
    }

    private suspend fun fetchSafebooruTag(name: String): Int? = withContext(Dispatchers.IO) {
        try {
            val url = "https://safebooru.org/index.php".toHttpUrl().newBuilder()
                .addQueryParameter("page", "dapi")
                .addQueryParameter("s", "tag")
                .addQueryParameter("q", "index")
                .addQueryParameter("name", name)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", LOOKUP_USER_AGENT)
                .build()
            NetworkClient.baseClient.newCall(request).await().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (!body.contains("<tags")) return@use null
                for (element in TAG_ELEMENT.findAll(body)) {
                    val attrs = ATTRIBUTE.findAll(element.value).associate { it.groupValues[1] to it.groupValues[2] }
                    val found = attrs["name"]?.let { org.jsoup.parser.Parser.unescapeEntities(it, true) }?.lowercase() ?: continue
                    val type = attrs["type"]?.toIntOrNull() ?: continue
                    if (found == name) return@use type
                }
                UNKNOWN
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Log.w(TAG, "Fallback tag lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun fetchFromDanbooru(names: List<String>): Map<String, Int>? = withContext(Dispatchers.IO) {
        try {
            val url = "https://danbooru.donmai.us/tags.json".toHttpUrl().newBuilder()
                .addQueryParameter("search[name_comma]", names.joinToString(","))
                .addQueryParameter("only", "name,category")
                .addQueryParameter("limit", "1000")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", LOOKUP_USER_AGENT)
                .build()
            NetworkClient.baseClient.newCall(request).await().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()?.trim().orEmpty()
                if (!body.startsWith("[")) return@use null
                val arr = JSONArray(body)
                val result = HashMap<String, Int>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name").lowercase()
                    if (name.isNotBlank()) result[name] = o.optInt("category", 0)
                }
                result
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            Log.w(TAG, "Tag category lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun persist() = writeMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val f = file ?: return@runCatching
                val obj = JSONObject()
                synchronized(categories) {
                    val iterator = categories.keys.iterator()
                    var excess = categories.size - MAX_ENTRIES
                    while (excess > 0 && iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                        excess--
                    }
                    for ((k, v) in categories) obj.put(k, v)
                }
                val tmp = File(f.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(obj.toString())
                if (!tmp.renameTo(f)) {
                    f.writeText(obj.toString())
                    tmp.delete()
                }
            }.onFailure { Log.w(TAG, "Failed to save tag categories", it) }
        }
    }
}
