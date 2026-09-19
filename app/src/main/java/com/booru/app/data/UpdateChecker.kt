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

    fun selectReleaseApk(candidates: List<Pair<String, String>>): String? {
        if (candidates.isEmpty()) return null
        val nonDebug = candidates.filterNot { it.first.contains("debug", ignoreCase = true) }
        if (nonDebug.isEmpty()) return null

        val exactRelease = nonDebug.find {
            it.first.equals("app-release.apk", ignoreCase = true) ||
            it.first.equals("booru-release.apk", ignoreCase = true)
        }
        if (exactRelease != null) return exactRelease.second

        val endsWithRelease = nonDebug.find {
            it.first.endsWith("-release.apk", ignoreCase = true) ||
            it.first.endsWith("_release.apk", ignoreCase = true)
        }
        if (endsWithRelease != null) return endsWithRelease.second

        val containsRelease = nonDebug.find { it.first.contains("release", ignoreCase = true) }
        if (containsRelease != null) return containsRelease.second

        return nonDebug.firstOrNull()?.second
    }

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
                    val apkCandidates = mutableListOf<Pair<String, String>>()
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url").trim()
                        if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                            apkCandidates.add(assetName to downloadUrl)
                        }
                    }
                    apkDownloadUrl = selectReleaseApk(apkCandidates)
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
