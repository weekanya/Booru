package com.example.boorugallery.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseName: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?
)

object UpdateChecker {
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/weekanya/Booru/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("User-Agent", "BooruApp")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val bodyStr = response.body?.string() ?: return@runCatching null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "").trim()
                val name = json.optString("name", "Booru $tagName")
                val body = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/weekanya/Booru/releases")

                var apkDownloadUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }

                if (tagName.isNotEmpty()) {
                    AppUpdateInfo(
                        latestVersion = tagName,
                        releaseName = name,
                        releaseNotes = body,
                        releaseUrl = htmlUrl,
                        apkDownloadUrl = apkDownloadUrl
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanLocal = currentVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
