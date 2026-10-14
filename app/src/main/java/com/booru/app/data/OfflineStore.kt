package com.booru.app.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.booru.app.BooruRepository
import com.booru.app.BooruVideoCache
import com.booru.app.RemoteMedia
import com.booru.app.data.network.NetworkClient
import com.booru.app.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object OfflineStore {
    private const val TAG = "OfflineStore"
    private const val DIR_NAME = "offline_media"
    private const val INDEX_NAME = "offline_index.json"
    private const val MAX_FILE_BYTES = 80L * 1024 * 1024
    private const val PARALLELISM = 4
    private val EXTENSION = Regex("[a-z0-9]{1,5}")

    private val client by lazy {
        NetworkClient.baseClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.MINUTES)
            .build()
    }
    private val urlToFile = ConcurrentHashMap<String, String>()
    private val entryFiles = ConcurrentHashMap<String, Map<String, String>>()
    private val indexMutex = Mutex()
    private val activeCalls = ConcurrentHashMap.newKeySet<okhttp3.Call>()
    @Volatile private var dir: File? = null
    @Volatile private var indexFile: File? = null
    private val indexLoaded = kotlinx.coroutines.CompletableDeferred<Unit>()

    var posts by mutableStateOf<List<RemoteMedia>>(emptyList())
        private set
    var sizeBytes by mutableLongStateOf(0L)
        private set

    fun init(context: Context) {
        val base = context.applicationContext.filesDir
        dir = File(base, DIR_NAME).apply { mkdirs() }
        indexFile = File(base, INDEX_NAME)
    }

    suspend fun load() {
        try {
            loadIndex()
        } finally {
            indexLoaded.complete(Unit)
        }
    }

    private suspend fun loadIndex() = withContext(Dispatchers.IO) {
        val file = indexFile ?: return@withContext
        if (!file.exists()) return@withContext
        runCatching {
            val array = JSONObject(file.readText()).optJSONArray("posts") ?: JSONArray()
            val loaded = ArrayList<RemoteMedia>(array.length())
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val media = mediaFromJson(o)
                val files = o.optJSONObject("files")?.let { f -> f.keys().asSequence().associateWith { f.optString(it) } }.orEmpty()
                if (files.isEmpty()) continue
                entryFiles[media.mediaKey] = files
                files.forEach { (url, name) -> urlToFile[url] = name }
                loaded.add(media)
            }
            withContext(Dispatchers.Main) { posts = loaded }
            refreshSize()
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            Log.w(TAG, "Failed to read offline index", it)
        }
    }

    fun fileForUrl(url: String): File? {
        if (url.isBlank()) return null
        val name = urlToFile[url] ?: return null
        val file = File(dir ?: return null, name)
        return if (file.exists() && file.length() > 0) file else null
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        indexLoaded.await()
        indexMutex.withLock {
            urlToFile.clear()
            entryFiles.clear()
            dir?.listFiles()?.forEach { it.delete() }
            indexFile?.delete()
        }
        withContext(Dispatchers.Main) {
            posts = emptyList()
            sizeBytes = 0L
        }
    }

    fun cancelActive() {
        activeCalls.forEach { runCatching { it.cancel() } }
    }

    fun contains(mediaKey: String): Boolean = entryFiles.containsKey(mediaKey)

    suspend fun download(
        items: List<RemoteMedia>,
        preferOriginal: Boolean,
        onProgress: suspend (done: Int, bytes: Long) -> Unit
    ) = coroutineScope {
        indexLoaded.await()
        val previous = posts
        val known = previous.mapTo(HashSet()) { it.mediaKey }
        val knownUrls = previous.mapTo(HashSet()) { it.url }
        val fresh = items
            .filter { it.mediaKey !in known && (it.url.isBlank() || it.url !in knownUrls) }
            .distinctBy { it.mediaKey }
        val completedKeys = ConcurrentHashMap.newKeySet<String>()
        val bytes = AtomicLong(0L)
        val doneCount = AtomicInteger(0)
        val permits = Semaphore(PARALLELISM)
        fun snapshot() = fresh.filter { it.mediaKey in completedKeys } + previous
        val baseSize = sizeBytes
        val ticker = launch {
            var lastSaved = 0
            while (isActive) {
                val completed = completedKeys.size
                val live = if (completed != posts.size - previous.size) snapshot() else null
                withContext(Dispatchers.Main) {
                    if (live != null) posts = live
                    sizeBytes = baseSize + bytes.get()
                }
                onProgress(doneCount.get(), bytes.get())
                if (completed - lastSaved >= 10) {
                    lastSaved = completed
                    persist()
                }
                delay(200)
            }
        }
        try {
            fresh.map { media ->
                async(Dispatchers.IO) {
                    val ok = permits.withPermit { savePost(media, bytes, preferOriginal) }
                    if (ok) completedKeys.add(media.mediaKey)
                    doneCount.incrementAndGet()
                }
            }.awaitAll()
        } finally {
            ticker.cancel()
            withContext(NonCancellable) {
                commit(snapshot())
                onProgress(doneCount.get(), bytes.get())
            }
        }
        fresh.size
    }

    private suspend fun commit(result: List<RemoteMedia>) {
        val keys = result.mapTo(HashSet()) { it.mediaKey }
        entryFiles.keys.retainAll(keys)
        urlToFile.clear()
        entryFiles.values.forEach { files -> files.forEach { (url, name) -> urlToFile[url] = name } }
        withContext(Dispatchers.Main) { posts = result }
        persist()
        withContext(Dispatchers.IO) {
            val referenced = urlToFile.values.toHashSet()
            dir?.listFiles()?.forEach { file -> if (file.name !in referenced) file.delete() }
        }
        refreshSize()
    }

    private suspend fun savePost(media: RemoteMedia, bytes: AtomicLong, preferOriginal: Boolean): Boolean {
        val folder = dir ?: return false
        val animated = media.isVideo || media.isGif
        val mainUrl = if (animated || preferOriginal) media.url.ifBlank { media.sample } else media.sample.ifBlank { media.url }
        if (mainUrl.isBlank()) return false
        val mainName = fetch(mainUrl, folder, bytes) ?: return false
        val files = HashMap<String, String>()
        files[mainUrl] = mainName
        if (media.url.isNotBlank()) files.putIfAbsent(media.url, mainName)
        val previewName = media.preview.takeIf { it.isNotBlank() && it != mainUrl }?.let { fetch(it, folder, bytes) }
        if (previewName != null) files[media.preview] = previewName
        if (media.sample.isNotBlank() && media.sample !in files) {
            val sampleExt = extensionOf(media.sample, "")
            val sampleIsMotion = sampleExt in setOf("mp4", "webm", "mkv", "mov", "gif")
            files[media.sample] = if (animated && !sampleIsMotion) previewName ?: mainName else mainName
        }
        if (media.preview.isNotBlank() && media.preview !in files) files[media.preview] = mainName
        entryFiles[media.mediaKey] = files
        files.forEach { (url, name) -> urlToFile[url] = name }
        return true
    }

    private suspend fun fetch(url: String, folder: File, bytes: AtomicLong): String? {
        val name = hash(url) + "." + extensionOf(url, "jpg")
        val target = File(folder, name)
        if (target.exists() && target.length() > 0) return name
        val temp = try {
            File.createTempFile(name, ".tmp", folder)
        } catch (e: IOException) {
            Log.d(TAG, "Offline temp file failed: ${e.javaClass.simpleName}")
            return null
        }
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BooruRepository.USER_AGENT)
                .header("Referer", BooruVideoCache.refererFor(url))
                .build()
            val call = client.newCall(request)
            activeCalls.add(call)
            try {
            call.await().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                if (body.contentLength() > MAX_FILE_BYTES) return null
                body.byteStream().use { input ->
                    temp.outputStream().use { out ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            total += read
                            if (total > MAX_FILE_BYTES) throw IOException("File too large")
                            out.write(buffer, 0, read)
                            bytes.addAndGet(read.toLong())
                            kotlin.coroutines.coroutineContext.ensureActive()
                        }
                    }
                }
            }
            } finally {
                activeCalls.remove(call)
            }
            when {
                temp.length() <= 0 -> { temp.delete(); null }
                target.exists() && target.length() > 0 -> { temp.delete(); name }
                temp.renameTo(target) -> name
                else -> { temp.delete(); null }
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            temp.delete()
            throw c
        } catch (e: Exception) {
            Log.d(TAG, "Offline fetch failed: ${e.javaClass.simpleName}")
            temp.delete()
            null
        }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        indexMutex.withLock {
            val file = indexFile ?: return@withLock
            runCatching {
                val array = JSONArray()
                for (media in posts) {
                    val files = entryFiles[media.mediaKey] ?: continue
                    array.put(mediaToJson(media).put("files", JSONObject(files)))
                }
                val temp = File(file.parentFile, "$INDEX_NAME.tmp")
                temp.writeText(JSONObject().put("posts", array).toString())
                if (!temp.renameTo(file)) {
                    file.writeText(temp.readText())
                    temp.delete()
                }
            }.onFailure { Log.w(TAG, "Failed to save offline index", it) }
        }
    }

    private suspend fun refreshSize() {
        val total = withContext(Dispatchers.IO) { dir?.listFiles()?.sumOf { it.length() } ?: 0L }
        withContext(Dispatchers.Main) { sizeBytes = total }
    }

    private fun extensionOf(url: String, fallback: String): String {
        val ext = url.substringBefore('?').substringBefore('#').substringAfterLast('/').substringAfterLast('.', "").lowercase()
        return if (EXTENSION.matches(ext)) ext else fallback
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(40)

    private fun mediaToJson(m: RemoteMedia) = JSONObject()
        .put("url", m.url).put("preview", m.preview).put("sample", m.sample).put("tags", m.tags)
        .put("score", m.score).put("source", m.source).put("rating", m.rating).put("id", m.id)
        .put("width", m.width).put("height", m.height).put("createdAt", m.createdAt).put("sourceId", m.sourceId)

    private fun mediaFromJson(o: JSONObject) = RemoteMedia(
        url = o.optString("url"),
        preview = o.optString("preview"),
        sample = o.optString("sample"),
        tags = o.optString("tags"),
        score = o.optInt("score"),
        source = o.optString("source"),
        rating = o.optString("rating", "u"),
        id = o.optString("id"),
        width = o.optInt("width"),
        height = o.optInt("height"),
        createdAt = o.optLong("createdAt"),
        sourceId = o.optString("sourceId")
    )
}
