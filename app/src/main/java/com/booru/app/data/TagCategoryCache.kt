package com.booru.app.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.booru.app.BooruRepository
import com.booru.app.data.network.NetworkClient
import com.booru.app.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Real tag categories (artist, character, copyright…) looked up on Danbooru and kept on disk.
 * Post tags only come with names, so without this the app has to guess categories from how a
 * tag is spelled, which misses most characters and artists.
 */
object TagCategoryCache {
    private const val TAG = "TagCategoryCache"
    private const val FILE_NAME = "tag_categories.json"
    private const val BATCH_SIZE = 100
    private const val MAX_ENTRIES = 40_000
    private const val UNKNOWN = -1

    private val categories = ConcurrentHashMap<String, Int>()
    private val loadMutex = Mutex()
    private val writeMutex = Mutex()
    @Volatile private var loaded = false
    @Volatile private var file: File? = null

    /** Bumped whenever new categories arrive, so UI that groups tags can recompute. */
    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        file = File(context.applicationContext.filesDir, FILE_NAME)
    }

    /** Category hint for [TagClassifier.classify], or "" when the tag was never looked up. */
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

    /**
     * Looks up every tag in [tags] that is not cached yet. Failures are quiet: tags that could
     * not be fetched simply keep falling back to the spelling heuristic. Returns true when
     * anything new was learned.
     */
    suspend fun resolve(tags: Collection<String>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Boolean {
        ensureLoaded()
        val missing = tags.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && !it.contains(',') && !categories.containsKey(it) }
            .distinct()
            .toList()
        if (missing.isEmpty()) return false

        val batches = missing.chunked(BATCH_SIZE)
        var changed = false
        batches.forEachIndexed { index, batch ->
            val fetched = fetchBatch(batch) ?: return@forEachIndexed
            for (name in batch) categories[name] = fetched[name] ?: UNKNOWN
            changed = true
            onProgress(index + 1, batches.size)
        }
        if (changed) {
            withContext(Dispatchers.Main) { version++ }
            persist()
        }
        return changed
    }

    private suspend fun fetchBatch(names: List<String>): Map<String, Int>? = withContext(Dispatchers.IO) {
        try {
            val url = "https://danbooru.donmai.us/tags.json".toHttpUrl().newBuilder()
                .addQueryParameter("search[name_comma]", names.joinToString(","))
                .addQueryParameter("only", "name,category")
                .addQueryParameter("limit", "1000")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BooruRepository.USER_AGENT)
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
                if (categories.size > MAX_ENTRIES) {
                    categories.keys.toList().take(categories.size - MAX_ENTRIES).forEach { categories.remove(it) }
                }
                val obj = JSONObject()
                for ((k, v) in categories) obj.put(k, v)
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
