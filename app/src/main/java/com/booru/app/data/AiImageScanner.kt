package com.booru.app.data

import android.util.Log
import com.booru.app.BooruRepository
import com.booru.app.RemoteMedia
import com.booru.app.data.network.NetworkClient
import com.booru.app.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.util.Collections
import java.util.concurrent.TimeUnit

object AiImageScanner {
    private const val TAG = "AiImageScanner"
    private const val HEADER_BYTES = 64 * 1024L
    private const val PARALLELISM = 4
    private const val TIMEOUT_MS = 8_000L
    private const val CACHE_SIZE = 4_000

    private val client by lazy {
        NetworkClient.baseClient.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }
    private val permits = Semaphore(PARALLELISM)
    private val verdicts: MutableMap<String, Boolean> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > CACHE_SIZE
        }
    )

    private val asciiMarkers = listOf(
        "trainedAlgorithmicMedia",
        "NovelAI",
        "NAI Diffusion",
        "Stable Diffusion",
        "invokeai_metadata",
        "sd-metadata",
        "\"class_type\"",
        "fooocus_scheme",
        "Midjourney"
    ).map { it.toByteArray(Charsets.ISO_8859_1) }

    private val stepsMarker = "Steps: "
    private val paramMarkers = listOf("Sampler: ", "CFG scale: ", "Seed: ")

    fun isScannable(media: RemoteMedia): Boolean {
        if (media.isVideo || media.isGif) return false
        val clean = media.url.substringBefore('?').lowercase()
        return clean.startsWith("http") &&
            (clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".webp"))
    }

    fun cachedVerdict(media: RemoteMedia): Boolean? = verdicts[media.url]

    suspend fun scan(items: List<RemoteMedia>, onDetected: suspend (RemoteMedia) -> Unit) = coroutineScope {
        items.filter { isScannable(it) }
            .map { media ->
                async {
                    val verdict = verdicts[media.url] ?: permits.withPermit {
                        verdicts[media.url] ?: inspect(media.url)?.also { verdicts[media.url] = it }
                    }
                    if (verdict == true) onDetected(media)
                }
            }
            .awaitAll()
    }

    private suspend fun inspect(url: String): Boolean? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BooruRepository.USER_AGENT)
                    .header("Referer", com.booru.app.BooruVideoCache.refererFor(url))
                    .header("Range", "bytes=0-${HEADER_BYTES - 1}")
                    .build()
                client.newCall(request).await().use { response ->
                    if (!response.isSuccessful) return@use null
                    val source = response.body?.source() ?: return@use null
                    source.request(HEADER_BYTES)
                    val size = minOf(source.buffer.size, HEADER_BYTES)
                    val bytes = source.buffer.readByteArray(size)
                    containsAiTrace(bytes)
                }
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.d(TAG, "AI check skipped: ${e.javaClass.simpleName}")
                null
            }
        }
    }

    internal fun containsAiTrace(bytes: ByteArray): Boolean {
        if (asciiMarkers.any { indexOf(bytes, it) >= 0 }) return true
        for (charset in listOf(Charsets.ISO_8859_1, Charsets.UTF_16BE, Charsets.UTF_16LE)) {
            if (indexOf(bytes, stepsMarker.toByteArray(charset)) >= 0 &&
                paramMarkers.any { indexOf(bytes, it.toByteArray(charset)) >= 0 }
            ) return true
        }
        return false
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        val first = needle[0]
        val last = haystack.size - needle.size
        var i = 0
        while (i <= last) {
            if (haystack[i] == first) {
                var j = 1
                while (j < needle.size && haystack[i + j] == needle[j]) j++
                if (j == needle.size) return i
            }
            i++
        }
        return -1
    }
}
