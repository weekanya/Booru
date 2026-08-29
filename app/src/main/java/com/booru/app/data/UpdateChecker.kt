package com.booru.app.data

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

    data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String? = null
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)

            if (preRelease == null && other.preRelease != null) return 1
            if (preRelease != null && other.preRelease == null) return -1
            if (preRelease != null && other.preRelease != null) {
                return preRelease.compareTo(other.preRelease)
            }
            return 0
        }

        companion object {
            fun parse(raw: String): SemVer {
                val clean = raw.trim().removePrefix("v").removePrefix("V")
                val versionPart = clean.substringBefore("-")
                val prePart = if (clean.contains("-")) clean.substringAfter("-") else null

                val numbers = versionPart.split(".").mapNotNull { it.toIntOrNull() }
                val major = numbers.getOrElse(0) { 0 }
                val minor = numbers.getOrElse(1) { 0 }
                val patch = numbers.getOrElse(2) { 0 }

                return SemVer(major, minor, patch, prePart)
            }
        }
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        return try {
            val remote = SemVer.parse(remoteTag)
            val local = SemVer.parse(currentVersion)
            remote > local
        } catch (_: Exception) {
            false
        }
    }
}
