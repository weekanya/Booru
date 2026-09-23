package com.booru.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booru.app.data.AppLanguage
import com.booru.app.data.AppUpdateInfo
import com.booru.app.data.BooruCacheManager
import com.booru.app.data.BooruPreferences
import com.booru.app.data.CustomBooruSource
import com.booru.app.data.ImageQuality
import com.booru.app.data.UpdateChecker
import com.booru.app.data.db.AppDatabase
import com.booru.app.data.db.FavoriteEntity
import com.booru.app.data.isBuiltInSourceName
import com.booru.app.data.isHttpsBooruUrl
import com.booru.app.data.sanitizeBooruBaseUrl
import com.booru.app.data.security.SecureCredentialsStorage
import com.booru.app.ui.AppPalette
import com.booru.app.ui.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class ContentType { PHOTOS, VIDEOS, GIFS }

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BooruRepository()
    val prefs = BooruPreferences(application)
    private val secureStorage = SecureCredentialsStorage(application)
    private val favoriteDao = AppDatabase.getDatabase(application).favoriteDao()

    var updateInfo by mutableStateOf<AppUpdateInfo?>(null); private set
    var isCheckingUpdate by mutableStateOf(false); private set
    var manualCheckResult by mutableStateOf<String?>(null); private set
    var isDownloadingUpdate by mutableStateOf(false); private set
    var updateDownloadProgress by mutableFloatStateOf(0f); private set
    var updateDownloadProgressText by mutableStateOf(""); private set
    var updateDownloadError by mutableStateOf<String?>(null); private set
    var downloadedApkFile by mutableStateOf<File?>(null); private set

    var cacheSizeFormatted by mutableStateOf("0 B"); private set
    var favoritesStorageSizeFormatted by mutableStateOf("0 B"); private set
    var isClearingCache by mutableStateOf(false); private set

    var results by mutableStateOf<List<RemoteMedia>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var loadingMore by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var isAuthError by mutableStateOf(false); private set
    var authErrorSource by mutableStateOf<String?>(null); private set
    var authErrorCode by mutableStateOf<Int?>(null); private set

    var query by mutableStateOf(""); private set
    var source by mutableStateOf(BooruRepository.SOURCE_ALL); private set
    var safeMode by mutableStateOf(false); private set
    var excludeSafe by mutableStateOf(false); private set
    var noAi by mutableStateOf(false); private set
    var sortOrder by mutableStateOf(SortOrder.NEWEST); private set

    var themeMode by mutableStateOf(ThemeMode.SYSTEM); private set
    var palette by mutableStateOf(AppPalette.MONET); private set
    var useDynamicColor by mutableStateOf(true); private set
    var language by mutableStateOf(AppLanguage.ENGLISH); private set

    var rule34UserId by mutableStateOf(""); private set
    var rule34ApiKey by mutableStateOf(""); private set
    var gelbooruUserId by mutableStateOf(""); private set
    var gelbooruApiKey by mutableStateOf(""); private set

    var favoritesList by mutableStateOf<List<RemoteMedia>>(emptyList()); private set
    var favoriteKeys by mutableStateOf<Set<String>>(emptySet()); private set

    var searchHistory by mutableStateOf<List<String>>(emptyList()); private set
    var tagSuggestions by mutableStateOf<List<TagSuggestion>>(emptyList()); private set
    var tagBlacklist by mutableStateOf<List<String>>(emptyList()); private set

    var imageQuality by mutableStateOf(ImageQuality.SAMPLE); private set
    var customSources by mutableStateOf<List<CustomBooruSource>>(emptyList()); private set
    var selectedContentTypes by mutableStateOf<Set<ContentType>>(emptySet()); private set
    var recommendationTags by mutableStateOf<List<String>>(emptyList()); private set

    private var currentPage = 0
    var hasMore by mutableStateOf(true); private set
    var activeTagCount by mutableStateOf(0); private set
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var suggestionJob: Job? = null
    private var currentSearchGeneration = 0L
    private val favoriteMutex = Mutex()

    init {
        viewModelScope.launch {
            if (secureStorage.isSecureStorageAvailable) {
                prefs.migrateLegacyCredentialsAndCustomSources(secureStorage)
            }
            rule34UserId = secureStorage.getRule34UserId()
            rule34ApiKey = secureStorage.getRule34ApiKey()
            gelbooruUserId = secureStorage.getGelbooruUserId()
            gelbooruApiKey = secureStorage.getGelbooruApiKey()

            runCatching {
                val legacyFavs = prefs.favorites.first()
                if (legacyFavs.isNotEmpty()) {
                    legacyFavs.forEach { fav ->
                        favoriteDao.insert(FavoriteEntity.fromRemoteMedia(fav))
                    }
                    prefs.clearLegacyFavorites()
                }
            }

            val initialSource = prefs.defaultSource.first()
            val initialSafe = prefs.safeMode.first()
            val initialExcludeSafe = prefs.excludeSafe.first()
            val initialNoAi = prefs.noAiFilter.first()
            val initialLang = prefs.language.first()
            val initialTheme = prefs.themeMode.first()
            val initialPalette = prefs.palette.first()
            val initialCustom = prefs.customSources.first()
            val initialBlacklist = prefs.tagBlacklist.first()
            val initialRecMap = prefs.recommendationTags.first()
            val sortedRec = initialRecMap.entries.sortedByDescending { it.value }.map { it.key }
            customSources = initialCustom
            val isCustomValid = initialCustom.any { (it.key == initialSource || it.id == initialSource) && it.enabled }
            val isBuiltInValid = BooruRepository.AVAILABLE_SOURCES.contains(initialSource)
            source = if (isBuiltInValid || isCustomValid) initialSource else BooruRepository.SOURCE_ALL
            safeMode = initialSafe
            excludeSafe = initialExcludeSafe
            noAi = initialNoAi
            language = initialLang
            themeMode = initialTheme
            palette = initialPalette
            tagBlacklist = initialBlacklist
            recommendationTags = sortedRec.take(15)

            updateCacheSize()
            runCatching {
                BooruCacheManager.pruneOrphanedFavoritesMedia(getApplication(), favoritesList)
            }

            search(source, "", safeMode)

            startLongLivedObservers()
        }
    }

    private fun startLongLivedObservers() {
        viewModelScope.launch {
            prefs.themeMode.collect { themeMode = it }
        }
        viewModelScope.launch {
            prefs.palette.collect { palette = it }
        }
        viewModelScope.launch {
            prefs.dynamicColor.collect { useDynamicColor = it }
        }
        viewModelScope.launch {
            prefs.language.collect { language = it }
        }
        viewModelScope.launch {
            prefs.safeMode.collect { safeMode = it }
        }
        viewModelScope.launch {
            prefs.excludeSafe.collect { excludeSafe = it }
        }
        viewModelScope.launch {
            prefs.noAiFilter.collect { noAi = it }
        }
        viewModelScope.launch {
            prefs.searchHistory.collect { searchHistory = it }
        }
        viewModelScope.launch {
            prefs.recommendationTags.collect { map ->
                val sorted = map.entries.sortedByDescending { it.value }.map { it.key }
                recommendationTags = sorted.take(15)
            }
        }
        viewModelScope.launch {
            prefs.imageQuality.collect { imageQuality = it }
        }
        viewModelScope.launch {
            prefs.customSources.collect { sources ->
                customSources = sources
            }
        }
        viewModelScope.launch {
            prefs.tagBlacklist.collect { bl ->
                tagBlacklist = bl
                if (results.isNotEmpty()) {
                    results = results.filterNot { isBlacklisted(it, bl) }
                }
            }
        }
        viewModelScope.launch {
            favoriteDao.getAllFavorites().collect { entities ->
                val mediaList = entities.map { it.toRemoteMedia() }
                updateFavoritesState(mediaList)
            }
        }
        viewModelScope.launch {
            checkForUpdates(isAutoCheck = true)
        }
    }

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdate = true
            manualCheckResult = null
            try {
                val currentVer = try {
                    val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                    pInfo.versionName ?: "5.3"
                } catch (_: Exception) {
                    "5.3"
                }

                val release = UpdateChecker.fetchLatestRelease()
                if (release == null) {
                    if (!isAutoCheck) {
                        manualCheckResult = "ERROR"
                    }
                } else if (UpdateChecker.isNewerVersion(release.latestVersion, currentVer)) {
                    if (isAutoCheck) {
                        val ignoredVersion = prefs.ignoredUpdateVersion.first()
                        if (ignoredVersion != release.latestVersion) {
                            updateInfo = release
                        }
                    } else {
                        updateInfo = release
                    }
                } else {
                    if (!isAutoCheck) {
                        manualCheckResult = "UP_TO_DATE"
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}", e)
                if (!isAutoCheck) {
                    manualCheckResult = "ERROR"
                }
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    private var updateDownloadJob: Job? = null

    fun cancelUpdateDownload() {
        updateDownloadJob?.cancel()
        updateDownloadJob = null
        isDownloadingUpdate = false
        updateDownloadProgress = 0f
        updateDownloadProgressText = "0%"
    }

    fun downloadAndInstallUpdate(context: Context, info: AppUpdateInfo) {
        downloadUpdate(context, info)
    }

    fun downloadUpdate(context: Context, info: AppUpdateInfo) {
        if (isDownloadingUpdate || info.apkDownloadUrl.isNullOrBlank()) return

        isDownloadingUpdate = true
        updateDownloadProgress = 0f
        updateDownloadProgressText = "0%"
        updateDownloadError = null

        val downloadDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(downloadDir, "booru_${info.latestVersion}.apk")
        val tempFile = File(downloadDir, "booru_${info.latestVersion}.apk.tmp")

        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(180_000L) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    val downloadUrl = info.apkDownloadUrl
                    val parsedUri = Uri.parse(downloadUrl)
                    val scheme = parsedUri.scheme ?: ""
                    val host = parsedUri.host?.lowercase() ?: ""
                    if (!scheme.equals("https", ignoreCase = true)) {
                        throw SecurityException("Insecure download protocol: $scheme")
                    }
                    val isTrustedHost = host == "github.com" || host.endsWith(".github.com") ||
                            host == "objects.githubusercontent.com" || host.endsWith(".githubusercontent.com")
                    if (!isTrustedHost) {
                        throw SecurityException("Untrusted download host: $host")
                    }

                    val client = OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .addNetworkInterceptor { chain ->
                            val reqUrl = chain.request().url
                            if (!reqUrl.isHttps) {
                                throw IOException("Insecure HTTP redirect blocked: $reqUrl")
                            }
                            val redirectHost = reqUrl.host.lowercase()
                            val allowedRedirect = redirectHost == "github.com" || redirectHost.endsWith(".github.com") ||
                                    redirectHost == "objects.githubusercontent.com" || redirectHost.endsWith(".githubusercontent.com")
                            if (!allowedRedirect) {
                                throw IOException("Redirect to untrusted host blocked: $redirectHost")
                            }
                            chain.proceed(chain.request())
                        }
                        .build()

                    val request = Request.Builder()
                        .url(downloadUrl)
                        .header("User-Agent", "BooruApp/${info.latestVersion}")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP error: ${response.code}")
                        }

                        val body = response.body ?: throw IOException("Empty response body")
                        val contentLength = body.contentLength()
                        val maxAllowedBytes = 100L * 1024L * 1024L
                        if (contentLength > maxAllowedBytes) {
                            throw SecurityException("Update package Content-Length $contentLength exceeds limit of $maxAllowedBytes bytes")
                        }

                        val inputStream = body.byteStream()
                        val outputStream = tempFile.outputStream()

                        val buffer = ByteArray(8192)
                        var bytesRead = 0
                        var totalRead = 0L
                        var lastUpdateMs = System.currentTimeMillis()

                        outputStream.use { out ->
                            inputStream.use { input ->
                                while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                                    totalRead += bytesRead
                                    if (totalRead > maxAllowedBytes) {
                                        throw SecurityException("Update package exceeds maximum allowed size")
                                    }
                                    out.write(buffer, 0, bytesRead)
                                    val now = System.currentTimeMillis()
                                    if (contentLength > 0 && now - lastUpdateMs > 100) {
                                        val progress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                        val readMb = String.format(java.util.Locale.US, "%.1f", totalRead / (1024f * 1024f))
                                        val totalMb = String.format(java.util.Locale.US, "%.1f", contentLength / (1024f * 1024f))
                                        withContext(Dispatchers.Main) {
                                            updateDownloadProgress = progress
                                            updateDownloadProgressText = "${(progress * 100).toInt()}% ($readMb MB / $totalMb MB)"
                                        }
                                        lastUpdateMs = now
                                    }
                                }
                            }
                        }

                        ensureActive()

                        if (!tempFile.exists() || tempFile.length() == 0L) {
                            throw IOException("Downloaded APK file is empty")
                        }

                        if (targetFile.exists()) targetFile.delete()
                        if (!tempFile.renameTo(targetFile)) {
                            throw IOException("Failed to rename temporary APK to target file")
                        }

                        withContext(Dispatchers.Main) {
                            updateDownloadProgress = 1f
                            updateDownloadProgressText = "100%"
                            downloadedApkFile = targetFile
                            isDownloadingUpdate = false
                            installApk(context, targetFile)
                        }
                    }
                    true
                } ?: throw IOException("Download timed out")
            } catch (c: CancellationException) {
                if (tempFile.exists()) tempFile.delete()
                if (targetFile.exists()) targetFile.delete()
                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                }
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                if (targetFile.exists()) targetFile.delete()
                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                    updateDownloadError = e.message ?: "Download failed"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                }
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            if (!verifyApkSignature(context, file)) {
                if (file.exists()) file.delete()
                updateDownloadError = "APK signature verification failed"
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            updateDownloadError = "Installation error: ${e.message}"
        }
    }

    fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            } ?: return false

            if (archiveInfo.packageName != context.packageName) {
                return false
            }

            val currentInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                currentInfo.versionCode.toLong()
            }
            val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }
            if (apkVersionCode < currentVersionCode) {
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val apkSigningInfo = archiveInfo.signingInfo ?: return false
                val curSigningInfo = currentInfo.signingInfo ?: return false

                if (apkSigningInfo.hasMultipleSigners() || curSigningInfo.hasMultipleSigners()) {
                    if (apkSigningInfo.hasMultipleSigners() != curSigningInfo.hasMultipleSigners()) return false
                    val apkSigners = apkSigningInfo.apkContentsSigners?.map { it.toByteArray() } ?: return false
                    val curSigners = curSigningInfo.apkContentsSigners?.map { it.toByteArray() } ?: return false
                    if (apkSigners.size != curSigners.size) return false
                    apkSigners.all { apkSig -> curSigners.any { curSig -> apkSig.contentEquals(curSig) } }
                } else {
                    val apkCurrent = apkSigningInfo.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return false
                    val curCurrent = curSigningInfo.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return false

                    if (apkCurrent.contentEquals(curCurrent)) {
                        true
                    } else {
                        val apkHistory = apkSigningInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
                        val curHistory = curSigningInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
                        curHistory.any { it.contentEquals(apkCurrent) } || apkHistory.any { it.contentEquals(curCurrent) }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val apkSignatures = archiveInfo.signatures?.map { it.toByteArray() } ?: return false
                @Suppress("DEPRECATION")
                val currentSignatures = currentInfo.signatures?.map { it.toByteArray() } ?: return false
                if (apkSignatures.isEmpty() || currentSignatures.isEmpty()) return false
                if (apkSignatures.size != currentSignatures.size) return false
                apkSignatures.all { apkSig ->
                    currentSignatures.any { curSig -> apkSig.contentEquals(curSig) }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun ignoreUpdate(version: String) {
        viewModelScope.launch {
            prefs.setIgnoredUpdateVersion(version)
            dismissUpdate()
        }
    }

    fun deleteDownloadedApk() {
        downloadedApkFile?.let { file ->
            if (file.exists()) file.delete()
        }
        downloadedApkFile = null
    }

    fun dismissUpdate() {
        updateInfo = null
        isDownloadingUpdate = false
    }

    fun clearManualCheckResult() {
        manualCheckResult = null
    }

    fun isBlacklisted(media: RemoteMedia, blacklist: List<String> = tagBlacklist): Boolean {
        if (blacklist.isEmpty()) return false
        val mediaTags = media.tagList.map { it.lowercase() }.toSet()
        val mediaTagsStripped = mediaTags.mapNotNull { if (it.contains(":")) it.substringAfter(":") else null }.toSet()

        return blacklist.any { bl ->
            val clean = bl.trim().lowercase()
            if (clean.isBlank()) return@any false
            if (clean.contains(":")) {
                clean in mediaTags
            } else {
                clean in mediaTags || clean in mediaTagsStripped
            }
        }
    }

    private fun updateFavoritesState(list: List<RemoteMedia>) {
        favoritesList = list
        favoriteKeys = list.map { it.mediaKey }.toSet()
    }

    fun selectSource(newSource: String) {
        val resolved = BooruRepository.AVAILABLE_SOURCES.firstOrNull { it.equals(newSource, ignoreCase = true) }
            ?: customSources.find { (it.id == newSource || it.key == newSource) && it.enabled }?.id
            ?: newSource
        if (source == resolved) {
            refresh()
            return
        }
        source = resolved
        viewModelScope.launch {
            prefs.setDefaultSource(resolved)
        }
        search(resolved, query, safeMode)
    }

    var needsFeedRefresh by mutableStateOf(false); private set

    fun getCredentials(): BooruCredentials {
        val customCreds = mutableMapOf<String, Pair<String, String>>()
        if (secureStorage.isSecureStorageAvailable) {
            for (cs in customSources) {
                val k = secureStorage.getCustomApiKey(cs.id)
                val u = secureStorage.getCustomUserId(cs.id)
                if (k.isNotBlank() || u.isNotBlank()) {
                    customCreds[cs.id] = Pair(k, u)
                    customCreds[cs.key] = Pair(k, u)
                }
            }
        }
        return BooruCredentials(
            rule34UserId = rule34UserId,
            rule34ApiKey = rule34ApiKey,
            gelbooruUserId = gelbooruUserId,
            gelbooruApiKey = gelbooruApiKey,
            customCredentials = customCreds
        )
    }

    fun refreshFeedIfNeeded() {
        if (needsFeedRefresh) {
            needsFeedRefresh = false
            search(source, query, safeMode)
        }
    }

    fun refresh() {
        search(source, query, safeMode)
    }

    fun search(
        source: String = this.source,
        tags: String = this.query,
        safeMode: Boolean = this.safeMode
    ) {
        val searchGen = ++currentSearchGeneration
        searchJob?.cancel()
        loadMoreJob?.cancel()

        this.source = source
        this.query = tags
        this.safeMode = safeMode
        needsFeedRefresh = false
        currentPage = 0
        hasMore = true
        loading = true
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
        results = emptyList()

        val trimmedTags = tags.trim()
        activeTagCount = if (trimmedTags.isNotEmpty()) {
            tagSuggestions.find { it.value.equals(trimmedTags, ignoreCase = true) }?.count ?: 0
        } else 0

        if (trimmedTags.isNotEmpty()) {
            viewModelScope.launch {
                prefs.saveSearchQuery(trimmedTags)
                prefs.recordSearchTags(trimmedTags.split(Regex("\\s+")))
            }
            if (activeTagCount == 0 && !trimmedTags.contains(" ")) {
                viewModelScope.launch {
                    val s = repo.getTagSuggestions(source, trimmedTags)
                    val m = s.find { it.value.equals(trimmedTags, ignoreCase = true) }
                    if (m != null && m.count > 0) {
                        activeTagCount = m.count
                    }
                }
            }
        }

        searchJob = viewModelScope.launch {
            try {
                val list = if (source == BooruRepository.SOURCE_ALL && tags.isBlank() && recommendationTags.isNotEmpty()) {
                    val ratio = getRecommendationRatio(recommendationTags.size)
                    val tagsToFetch = recommendationTags.take(3)
                    val dGen = async {
                        try {
                            repo.search(
                                source = source,
                                tags = "",
                                safeMode = safeMode,
                                excludeSafe = excludeSafe,
                                noAi = noAi,
                                page = 0,
                                sortOrder = sortOrder,
                                contentTypes = selectedContentTypes,
                                credentials = getCredentials(),
                                customSources = customSources
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    val dTags = tagsToFetch.map { recTag ->
                        async {
                            try {
                                repo.search(
                                    source = source,
                                    tags = recTag,
                                    safeMode = safeMode,
                                    excludeSafe = excludeSafe,
                                    noAi = noAi,
                                    page = 0,
                                    sortOrder = sortOrder,
                                    contentTypes = selectedContentTypes,
                                    credentials = getCredentials(),
                                    customSources = customSources
                                )
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val genList = dGen.await()
                    val tagLists = dTags.map { it.await() }
                    val sanitizedTagLists = if (!selectedContentTypes.contains(ContentType.VIDEOS)) {
                        tagLists.map { list -> list.filterNot { item -> item.isVideo } }
                    } else {
                        tagLists
                    }
                    blendRecommendationFeed(genList, sanitizedTagLists, ratio)
                } else {
                    repo.search(
                        source = source,
                        tags = tags,
                        safeMode = safeMode,
                        excludeSafe = excludeSafe,
                        noAi = noAi,
                        page = 0,
                        sortOrder = sortOrder,
                        contentTypes = selectedContentTypes,
                        credentials = getCredentials(),
                        customSources = customSources
                    )
                }

                if (searchGen != currentSearchGeneration) return@launch

                val accumulated = mutableListOf<RemoteMedia>()
                var lastPageSize = list.size
                accumulated.addAll(list)
                var lastFetchedPage = 0
                val targetCount = if (selectedContentTypes.isNotEmpty()) 24 else BooruRepository.PAGE_SIZE
                val maxPagesToAccumulate = if (selectedContentTypes.isNotEmpty()) 15 else 1

                fun filterItems(items: List<RemoteMedia>): List<RemoteMedia> {
                    return items.filterNot { isBlacklisted(it) }
                        .filter { item ->
                            if (selectedContentTypes.isEmpty()) true
                            else (
                                (selectedContentTypes.contains(ContentType.PHOTOS) && !item.isVideo && !item.isGif) ||
                                (selectedContentTypes.contains(ContentType.VIDEOS) && item.isVideo) ||
                                (selectedContentTypes.contains(ContentType.GIFS) && item.isGif)
                            )
                        }
                }

                var currentFiltered = filterItems(accumulated).distinctBy { it.mediaKey }

                while (selectedContentTypes.isNotEmpty() && currentFiltered.size < targetCount && lastPageSize > 0 && lastFetchedPage < maxPagesToAccumulate) {
                    lastFetchedPage++
                    val nextPageList = repo.search(
                        source = source,
                        tags = tags,
                        safeMode = safeMode,
                        excludeSafe = excludeSafe,
                        noAi = noAi,
                        page = lastFetchedPage,
                        sortOrder = sortOrder,
                        contentTypes = selectedContentTypes,
                        credentials = getCredentials(),
                        customSources = customSources
                    )
                    if (searchGen != currentSearchGeneration) return@launch
                    lastPageSize = nextPageList.size
                    accumulated.addAll(nextPageList)
                    currentFiltered = filterItems(accumulated).distinctBy { it.mediaKey }
                }

                currentPage = lastFetchedPage
                results = currentFiltered
                hasMore = lastPageSize > 0
            } catch (authEx: BooruAuthException) {
                if (searchGen == currentSearchGeneration) {
                    results = emptyList()
                    isAuthError = true
                    authErrorSource = authEx.sourceKey
                    authErrorCode = authEx.statusCode
                    error = authEx.message
                }
            } catch (httpEx: BooruHttpException) {
                if (searchGen == currentSearchGeneration) {
                    results = emptyList()
                    isAuthError = httpEx.statusCode == 401 || httpEx.statusCode == 403
                    authErrorSource = httpEx.sourceKey
                    authErrorCode = httpEx.statusCode
                    error = httpEx.message
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                if (searchGen == currentSearchGeneration) {
                    results = emptyList()
                    isAuthError = false
                    authErrorSource = null
                    authErrorCode = null
                    error = e.message ?: "Failed to load data"
                }
            } finally {
                if (searchGen == currentSearchGeneration) {
                    loading = false
                }
            }
        }
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        val searchGen = currentSearchGeneration
        val targetPage = currentPage + 1

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            loadingMore = true
            try {
                val list = if (source == BooruRepository.SOURCE_ALL && query.isBlank() && recommendationTags.isNotEmpty()) {
                    val ratio = getRecommendationRatio(recommendationTags.size)
                    val tagsCount = recommendationTags.size
                    val tagsToFetch = if (tagsCount <= 3) {
                        recommendationTags
                    } else {
                        val startIndex = (targetPage * 3) % tagsCount
                        (0 until minOf(3, tagsCount)).map { offset ->
                            recommendationTags[(startIndex + offset) % tagsCount]
                        }
                    }
                    val dGen = async {
                        try {
                            repo.search(
                                source = source,
                                tags = "",
                                safeMode = safeMode,
                                excludeSafe = excludeSafe,
                                noAi = noAi,
                                page = targetPage,
                                sortOrder = sortOrder,
                                contentTypes = selectedContentTypes,
                                credentials = getCredentials(),
                                customSources = customSources
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    val dTags = tagsToFetch.map { recTag ->
                        val subPage = if (tagsCount <= 3) targetPage else targetPage / 3
                        async {
                            try {
                                repo.search(
                                    source = source,
                                    tags = recTag,
                                    safeMode = safeMode,
                                    excludeSafe = excludeSafe,
                                    noAi = noAi,
                                    page = subPage,
                                    sortOrder = sortOrder,
                                    contentTypes = selectedContentTypes,
                                    credentials = getCredentials(),
                                    customSources = customSources
                                )
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val genList = dGen.await()
                    val tagLists = dTags.map { it.await() }
                    val existingKeys = results.map { it.mediaKey }.toSet()
                    val sanitizedTagLists = if (!selectedContentTypes.contains(ContentType.VIDEOS)) {
                        tagLists.map { list -> list.filterNot { item -> item.isVideo } }
                    } else {
                        tagLists
                    }
                    blendRecommendationFeed(genList, sanitizedTagLists, ratio, existingKeys)
                } else {
                    repo.search(
                        source = source,
                        tags = query,
                        safeMode = safeMode,
                        excludeSafe = excludeSafe,
                        noAi = noAi,
                        page = targetPage,
                        sortOrder = sortOrder,
                        contentTypes = selectedContentTypes,
                        credentials = getCredentials(),
                        customSources = customSources
                    )
                }

                if (searchGen != currentSearchGeneration) return@launch

                var lastPageSize = list.size
                var lastFetchedPage = targetPage
                val accumulatedNew = mutableListOf<RemoteMedia>()
                accumulatedNew.addAll(list)

                fun filterItems(items: List<RemoteMedia>): List<RemoteMedia> {
                    return items.filterNot { isBlacklisted(it) }
                        .filter { item ->
                            if (selectedContentTypes.isEmpty()) true
                            else (
                                (selectedContentTypes.contains(ContentType.PHOTOS) && !item.isVideo && !item.isGif) ||
                                (selectedContentTypes.contains(ContentType.VIDEOS) && item.isVideo) ||
                                (selectedContentTypes.contains(ContentType.GIFS) && item.isGif)
                            )
                        }
                }

                var currentFiltered = filterItems(accumulatedNew)
                val targetCount = if (selectedContentTypes.isNotEmpty()) 20 else BooruRepository.PAGE_SIZE
                var extraPagesFetched = 0
                val maxExtraPages = if (selectedContentTypes.isNotEmpty()) 10 else 0

                while (selectedContentTypes.isNotEmpty() && currentFiltered.size < targetCount && lastPageSize > 0 && extraPagesFetched < maxExtraPages) {
                    lastFetchedPage++
                    extraPagesFetched++
                    val nextPageList = repo.search(
                        source = source,
                        tags = query,
                        safeMode = safeMode,
                        excludeSafe = excludeSafe,
                        noAi = noAi,
                        page = lastFetchedPage,
                        sortOrder = sortOrder,
                        contentTypes = selectedContentTypes,
                        credentials = getCredentials(),
                        customSources = customSources
                    )
                    if (searchGen != currentSearchGeneration) return@launch
                    lastPageSize = nextPageList.size
                    accumulatedNew.addAll(nextPageList)
                    currentFiltered = filterItems(accumulatedNew)
                }

                currentPage = lastFetchedPage
                results = (results + currentFiltered).distinctBy { it.mediaKey }
                hasMore = lastPageSize > 0
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load more items: ${e.message}", e)
            } finally {
                if (searchGen == currentSearchGeneration) {
                    loadingMore = false
                }
            }
        }
    }

    fun applySort(order: SortOrder) {
        if (sortOrder == order) return
        sortOrder = order
        search(source, query, safeMode)
    }

    fun searchTag(tag: String, targetSource: String = source) {
        val cleanTag = tag.trim().removeSuffix(",").removePrefix(",").trim().replace(" ", "_")
        val finalSource = BooruRepository.AVAILABLE_SOURCES.firstOrNull { it.equals(targetSource, ignoreCase = true) }
            ?: customSources.find { (it.id == targetSource || it.key == targetSource) && it.enabled }?.id
            ?: source
        source = finalSource
        search(finalSource, cleanTag, safeMode)
    }

    fun fetchTagSuggestions(input: String) {
        suggestionJob?.cancel()
        if (input.trim().length < 2) {
            tagSuggestions = emptyList()
            return
        }

        suggestionJob = viewModelScope.launch {
            try {
                val suggestions = repo.getTagSuggestions(source, input)
                tagSuggestions = suggestions
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                tagSuggestions = emptyList()
            }
        }
    }

    fun clearTagSuggestions() {
        tagSuggestions = emptyList()
    }

    fun toggleFavorite(media: RemoteMedia) {
        viewModelScope.launch {
            favoriteMutex.withLock {
                val key = media.mediaKey
                val wasFav = isFavorite(media)
                if (wasFav) {
                    favoriteKeys = favoriteKeys - key
                    val remaining = favoritesList.filterNot { it.mediaKey == key }
                    favoritesList = remaining
                    favoriteDao.deleteByKey(key)
                    BooruCacheManager.removeFavoriteMedia(getApplication(), media, remaining)
                } else {
                    favoriteKeys = favoriteKeys + key
                    favoritesList = favoritesList + media
                    favoriteDao.insert(FavoriteEntity.fromRemoteMedia(media))
                    BooruCacheManager.saveFavoriteMedia(getApplication(), media)
                }
                updateCacheSize()
            }
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            favoriteMutex.withLock {
                favoriteDao.clearAll()
                favoritesList = emptyList()
                favoriteKeys = emptySet()
                BooruCacheManager.pruneOrphanedFavoritesMedia(getApplication(), emptyList())
                updateCacheSize()
            }
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val browsingBytes = BooruCacheManager.getBrowsingCacheSizeBytes(getApplication())
            val favsBytes = BooruCacheManager.getFavoritesStorageSizeBytes(getApplication())
            cacheSizeFormatted = BooruCacheManager.formatBytes(browsingBytes)
            favoritesStorageSizeFormatted = BooruCacheManager.formatBytes(favsBytes)
        }
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        if (isClearingCache) return
        viewModelScope.launch {
            isClearingCache = true
            BooruCacheManager.clearBrowsingCache(getApplication())
            withContext(Dispatchers.IO) {
                val browsingBytes = BooruCacheManager.getBrowsingCacheSizeBytes(getApplication())
                val favsBytes = BooruCacheManager.getFavoritesStorageSizeBytes(getApplication())
                cacheSizeFormatted = BooruCacheManager.formatBytes(browsingBytes)
                favoritesStorageSizeFormatted = BooruCacheManager.formatBytes(favsBytes)
            }
            isClearingCache = false
            onComplete()
        }
    }

    fun isFavorite(media: RemoteMedia): Boolean {
        return media.mediaKey in favoriteKeys
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
            themeMode = mode
        }
    }

    fun updatePalette(palette: AppPalette) {
        viewModelScope.launch {
            prefs.setPalette(palette)
            this@GalleryViewModel.palette = palette
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun updateLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(lang)
            language = lang
        }
    }

    fun applyAllFilters(
        contentTypes: Set<ContentType>,
        sortOrder: SortOrder,
        safeMode: Boolean,
        excludeSafe: Boolean,
        noAi: Boolean
    ) {
        val changed = this.selectedContentTypes != contentTypes ||
                this.sortOrder != sortOrder ||
                this.safeMode != safeMode ||
                this.excludeSafe != excludeSafe ||
                this.noAi != noAi
        this.selectedContentTypes = contentTypes
        this.sortOrder = sortOrder
        this.safeMode = safeMode
        this.excludeSafe = excludeSafe
        this.noAi = noAi
        if (changed) {
            viewModelScope.launch {
                prefs.setSafeMode(safeMode)
                prefs.setExcludeSafe(excludeSafe)
                prefs.setNoAiFilter(noAi)
            }
            refresh()
        }
    }

    fun setSafeModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setSafeMode(enabled)
            safeMode = enabled
            if (enabled) {
                excludeSafe = false
            }
            search(source, query, enabled)
        }
    }

    fun setExcludeSafeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setExcludeSafe(enabled)
            excludeSafe = enabled
            if (enabled) {
                safeMode = false
            }
            search(source, query, safeMode)
        }
    }

    fun setNoAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNoAiFilter(enabled)
            noAi = enabled
            search(source, query, safeMode)
        }
    }

    fun saveRule34Keys(userId: String, apiKey: String) {
        secureStorage.setRule34UserId(userId)
        secureStorage.setRule34ApiKey(apiKey)
        rule34UserId = userId.trim()
        rule34ApiKey = apiKey.trim()
        needsFeedRefresh = true
        if (source == BooruRepository.SOURCE_RULE34 || source == BooruRepository.SOURCE_ALL || isAuthError) {
            search(source, query, safeMode)
        }
    }

    fun saveGelbooruKeys(userId: String, apiKey: String) {
        secureStorage.setGelbooruUserId(userId)
        secureStorage.setGelbooruApiKey(apiKey)
        gelbooruUserId = userId.trim()
        gelbooruApiKey = apiKey.trim()
        needsFeedRefresh = true
        if (source == BooruRepository.SOURCE_GELBOORU || source == BooruRepository.SOURCE_ALL || isAuthError) {
            search(source, query, safeMode)
        }
    }

    fun removeFromHistory(query: String) {
        viewModelScope.launch { prefs.removeSearchQuery(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun toggleContentType(type: ContentType) {
        selectedContentTypes = if (selectedContentTypes.contains(type)) {
            selectedContentTypes - type
        } else {
            selectedContentTypes + type
        }
        refresh()
    }

    fun clearContentTypes() {
        selectedContentTypes = emptySet()
        refresh()
    }

    fun clearRecommendationMemory(onDone: () -> Unit = {}) {
        needsFeedRefresh = true
        source = BooruRepository.SOURCE_ALL
        query = ""
        recommendationTags = emptyList()
        viewModelScope.launch {
            prefs.setDefaultSource(BooruRepository.SOURCE_ALL)
            prefs.clearRecommendationData()
            onDone()
        }
    }

    fun addBlacklistedTag(tag: String) {
        needsFeedRefresh = true
        val clean = tag.trim().lowercase()
        if (clean.isNotBlank() && results.isNotEmpty()) {
            results = results.filterNot { isBlacklisted(it, tagBlacklist + clean) }
        }
        viewModelScope.launch {
            prefs.addTagToBlacklist(tag)
            refresh()
        }
    }

    fun removeBlacklistedTag(tag: String) {
        needsFeedRefresh = true
        viewModelScope.launch {
            prefs.removeTagFromBlacklist(tag)
            refresh()
        }
    }

    fun clearBlacklist() {
        needsFeedRefresh = true
        viewModelScope.launch {
            prefs.clearTagBlacklist()
            refresh()
        }
    }

    fun clearError() {
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
    }

    fun updateImageQuality(quality: ImageQuality) {
        imageQuality = quality
        viewModelScope.launch { prefs.setImageQuality(quality) }
    }

    fun getCustomSourceApiKey(sourceId: String): String =
        if (secureStorage.isSecureStorageAvailable) secureStorage.getCustomApiKey(sourceId) else ""

    fun getCustomSourceUserId(sourceId: String): String =
        if (secureStorage.isSecureStorageAvailable) secureStorage.getCustomUserId(sourceId) else ""

    fun addCustomSource(
        source: CustomBooruSource,
        apiKey: String = "",
        userId: String = ""
    ): Boolean {
        if (!isHttpsBooruUrl(source.baseUrl)) {
            error = "Custom source URL must use a valid HTTPS URL"
            return false
        }
        if (isBuiltInSourceName(source.name)) {
            error = "Custom source name cannot conflict with built-in sources"
            return false
        }
        if (source.name.isBlank()) {
            error = "Custom source name cannot be empty"
            return false
        }
        val safeSource = source.copy(baseUrl = sanitizeBooruBaseUrl(source.baseUrl))
        if (secureStorage.isSecureStorageAvailable) {
            if (apiKey.isNotBlank()) secureStorage.setCustomApiKey(safeSource.id, apiKey)
            else secureStorage.removeCustomApiKey(safeSource.id)

            if (userId.isNotBlank()) secureStorage.setCustomUserId(safeSource.id, userId)
            else secureStorage.removeCustomUserId(safeSource.id)
        }
        val updated = customSources.filterNot { it.id == safeSource.id } + safeSource
        customSources = updated
        viewModelScope.launch { prefs.saveCustomSources(updated) }
        return true
    }

    fun removeCustomSource(sourceId: String) {
        val target = customSources.find { it.id == sourceId }
        val updated = customSources.filterNot { it.id == sourceId }
        customSources = updated
        viewModelScope.launch {
            prefs.saveCustomSources(updated)
            secureStorage.removeCustomCredentials(sourceId)
        }
        val isCurrentSourceDeleted = target != null && (
                source.equals(target.id, ignoreCase = true) ||
                        source.equals(target.key, ignoreCase = true)
                )
        if (isCurrentSourceDeleted || availableSources.none { it.equals(source, ignoreCase = true) }) {
            source = BooruRepository.SOURCE_ALL
            viewModelScope.launch { prefs.setDefaultSource(BooruRepository.SOURCE_ALL) }
            refresh()
        }
    }

    val availableSources: List<String>
        get() = BooruRepository.AVAILABLE_SOURCES + customSources.filter { it.enabled }.map { it.id }

    fun resolveMediaUrl(media: RemoteMedia): String = when (imageQuality) {
        ImageQuality.ORIGINAL -> media.url.ifBlank { media.sample.ifBlank { media.preview } }
        ImageQuality.SAVER    -> media.preview.ifBlank { media.sample.ifBlank { media.url } }
        ImageQuality.SAMPLE   -> media.sample.ifBlank { media.url.ifBlank { media.preview } }
    }

    fun resolveVideoUrl(media: RemoteMedia): String {
        if (!media.isVideo) return resolveMediaUrl(media)
        return when (imageQuality) {
            ImageQuality.ORIGINAL -> media.url.ifBlank { media.sample }
            ImageQuality.SAMPLE, ImageQuality.SAVER -> {
                val sampleClean = media.sample.substringBefore("?").lowercase()
                if (sampleClean.endsWith(".mp4") || sampleClean.endsWith(".webm") || sampleClean.endsWith(".mkv")) {
                    media.sample
                } else {
                    media.url
                }
            }
        }
    }

    fun resolveThumbnailUrl(media: RemoteMedia): String = when (imageQuality) {
        ImageQuality.SAVER -> media.preview.ifBlank { media.sample.ifBlank { media.url } }
        ImageQuality.ORIGINAL -> media.sample.ifBlank { media.url.ifBlank { media.preview } }
        ImageQuality.SAMPLE -> media.sample.ifBlank { media.preview.ifBlank { media.url } }
    }

    fun getSourceDisplayName(key: String): String {
        if (key == BooruRepository.SOURCE_ALL || key.equals("all sources", ignoreCase = true)) {
            return com.booru.app.data.Strings.sourceRecommendations(language)
        }
        return BooruRepository.getSourceDisplayName(key, customSources)
    }

    var fullscreenState by mutableStateOf<FullscreenState?>(null)
        private set

    fun openFullscreen(list: List<RemoteMedia>, index: Int) {
        if (list.isNotEmpty()) {
            fullscreenState = FullscreenState(
                list = list,
                index = index.coerceIn(0, list.size - 1),
                isFromResults = (list === results)
            )
        }
    }

    fun closeFullscreen() {
        fullscreenState = null
    }

    companion object {
        private const val TAG = "GalleryViewModel"

        fun getRecommendationRatio(tagCount: Int): Float {
            return when (tagCount) {
                0 -> 0.0f
                1 -> 0.30f
                2 -> 0.40f
                3 -> 0.50f
                else -> 0.60f
            }
        }

        fun blendRecommendationFeed(
            generalPosts: List<RemoteMedia>,
            tagPosts: List<List<RemoteMedia>>,
            ratio: Float,
            existingKeys: Set<String> = emptySet()
        ): List<RemoteMedia> {
            val nonNullGeneral = generalPosts.filterNot { it.mediaKey in existingKeys }
            val nonNullTagPosts = tagPosts.map { list -> list.filterNot { it.mediaKey in existingKeys } }

            if (nonNullGeneral.isEmpty() && nonNullTagPosts.all { it.isEmpty() }) return emptyList()
            if (nonNullTagPosts.all { it.isEmpty() }) return nonNullGeneral

            val seen = existingKeys.toMutableSet()
            val recQueue = ArrayDeque<RemoteMedia>()
            val tagQueues = nonNullTagPosts.map { ArrayDeque(it) }

            while (tagQueues.any { it.isNotEmpty() }) {
                for (q in tagQueues) {
                    if (q.isNotEmpty()) {
                        val item = q.removeFirst()
                        if (seen.add(item.mediaKey)) {
                            recQueue.add(item)
                        }
                    }
                }
            }

            if (nonNullGeneral.isEmpty()) {
                return recQueue.toList()
            }

            val genQueue = ArrayDeque<RemoteMedia>()
            for (item in nonNullGeneral) {
                if (seen.add(item.mediaKey)) {
                    genQueue.add(item)
                }
            }

            val result = mutableListOf<RemoteMedia>()
            var recAcc = 0f
            val clampedRatio = ratio.coerceIn(0.1f, 0.9f)

            while (genQueue.isNotEmpty() || recQueue.isNotEmpty()) {
                recAcc += clampedRatio
                val pickRec = if (recAcc >= 1f && recQueue.isNotEmpty()) {
                    recAcc -= 1f
                    true
                } else if (genQueue.isEmpty()) {
                    true
                } else {
                    false
                }

                val item = if (pickRec && recQueue.isNotEmpty()) {
                    recQueue.removeFirst()
                } else if (genQueue.isNotEmpty()) {
                    genQueue.removeFirst()
                } else if (recQueue.isNotEmpty()) {
                    recQueue.removeFirst()
                } else null

                if (item != null) {
                    result.add(item)
                }
            }

            return result
        }
    }
}

data class FullscreenState(
    val list: List<RemoteMedia>,
    val index: Int,
    val isFromResults: Boolean = false
)
