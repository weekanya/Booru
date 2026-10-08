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
import androidx.compose.runtime.mutableIntStateOf
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
import com.booru.app.data.TagCategory
import com.booru.app.data.TagClassifier
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import com.booru.app.data.network.NetworkClient
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
    var isRefreshing by mutableStateOf(false); private set
    var isPullRefreshing by mutableStateOf(false); private set
    var isTabRefreshing by mutableStateOf(false); private set
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
    private var recordedRecTagsMap: Map<String, Int> = emptyMap()
    var scrollToTopTrigger by mutableStateOf(0L); private set
    var refreshSeed by mutableStateOf(0); private set
    var isIncognito by mutableStateOf(false); private set
    var recommendationRatio by mutableFloatStateOf(0.5f); private set
    var biometricLockEnabled by mutableStateOf(false); private set
    var biometricLockTimeoutMin by mutableIntStateOf(0); private set
    var favoriteFolders by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var customFolders by mutableStateOf<Set<String>>(emptySet()); private set
    var gridColumnsCount by mutableIntStateOf(0); private set

    private var currentPage = 0
    var hasMore by mutableStateOf(true); private set
    var activeTagCount by mutableStateOf(0); private set
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var suggestionJob: Job? = null
    private var currentSearchGeneration = 0L
    private var loadMoreToken = 0L
    private var emptyLoadMoreStreak = 0
    var loadMoreError by mutableStateOf(false); private set
    var isNetworkError by mutableStateOf(false); private set
    var resultsEpoch by mutableIntStateOf(0); private set
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
            recordedRecTagsMap = initialRecMap
            val initialFavorites = runCatching {
                withContext(Dispatchers.IO) { favoriteDao.getAllFavoritesList().map { it.toRemoteMedia() } }
            }.getOrNull()
            if (initialFavorites != null) {
                favoritesList = initialFavorites
                favoriteKeys = initialFavorites.mapTo(HashSet()) { it.mediaKey }
            }
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
            recommendationTags = withContext(Dispatchers.Default) {
                computeRecommendationTags(favoritesList, tagBlacklist, recordedRecTagsMap)
            }
            recommendationRatio = prefs.recommendationRatio.first()
            biometricLockEnabled = prefs.biometricLockEnabled.first()
            biometricLockTimeoutMin = prefs.biometricLockTimeoutMin.first()
            favoriteFolders = prefs.favoriteFolders.first()
            customFolders = prefs.customFolders.first()
            gridColumnsCount = prefs.gridColumnsCount.first()
            imageQuality = prefs.imageQuality.first()

            search(source, "", safeMode)

            startLongLivedObservers()

            if (initialFavorites != null) {
                launch(Dispatchers.IO) {
                    runCatching { BooruCacheManager.pruneOrphanedFavoritesMedia(getApplication(), initialFavorites) }
                    updateCacheSize()
                }
            } else {
                updateCacheSize()
            }
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
                recordedRecTagsMap = map
                recalculateRecommendationTags()
            }
        }
        viewModelScope.launch {
            prefs.imageQuality.collect { imageQuality = it }
        }
        viewModelScope.launch {
            prefs.disabledSources.collect { disabledSources = it }
        }
        viewModelScope.launch {
            prefs.customSources.collect { sources ->
                if (sources != customSources) {
                    customSources = sources
                    cachedCredentials = null
                }
            }
        }
        viewModelScope.launch {
            prefs.tagBlacklist.collect { bl ->
                if (bl == tagBlacklist) return@collect
                tagBlacklist = bl
                if (results.isNotEmpty()) {
                    val current = results
                    val filtered = withContext(Dispatchers.Default) {
                        current.filterNot { com.booru.app.data.TagBlacklistMatcher.isBlacklisted(it, bl) }
                    }
                    if (results === current) results = filtered
                }
                recalculateRecommendationTags()
            }
        }
        viewModelScope.launch {
            favoriteDao.getAllFavorites().collect { entities ->
                val mediaList = entities.map { it.toRemoteMedia() }
                updateFavoritesState(mediaList)
            }
        }
        viewModelScope.launch {
            prefs.recommendationRatio.collect { recommendationRatio = it }
        }
        viewModelScope.launch {
            prefs.biometricLockEnabled.collect { biometricLockEnabled = it }
        }
        viewModelScope.launch {
            prefs.biometricLockTimeoutMin.collect { biometricLockTimeoutMin = it }
        }
        viewModelScope.launch {
            prefs.favoriteFolders.collect { favoriteFolders = it }
        }
        viewModelScope.launch {
            prefs.customFolders.collect { customFolders = it }
        }
        viewModelScope.launch {
            prefs.gridColumnsCount.collect { gridColumnsCount = it }
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
        if (isDownloadingUpdate) return
        if (info.apkDownloadUrl.isNullOrBlank()) {
            updateDownloadError = "No APK file found in this release"
            return
        }
        val context: Context = context.applicationContext

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

                    val client = NetworkClient.baseClient.newBuilder()
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
        val context: Context = context.applicationContext
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
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
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
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
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
        return com.booru.app.data.TagBlacklistMatcher.isBlacklisted(media, blacklist)
    }

    private fun updateFavoritesState(list: List<RemoteMedia>) {
        if (list == favoritesList) return
        favoritesList = list
        favoriteKeys = list.mapTo(HashSet()) { it.mediaKey }
        recalculateRecommendationTags()
    }

    private var recommendationJob: Job? = null

    private fun recalculateRecommendationTags() {
        val favorites = favoritesList
        val blacklist = tagBlacklist
        val recorded = recordedRecTagsMap
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            recommendationTags = withContext(Dispatchers.Default) {
                computeRecommendationTags(favorites, blacklist, recorded)
            }
        }
    }

    private fun computeRecommendationTags(
        favorites: List<RemoteMedia>,
        blacklist: List<String>,
        recorded: Map<String, Int>
    ): List<String> {
        val blocked = blacklist.mapTo(HashSet()) { it.lowercase() }
        val tagWeights = mutableMapOf<String, Float>()
        val docFreq = HashMap<String, Int>()
        for (fav in favorites) {
            val tags = if (fav.tagList.isNotEmpty()) fav.tagList else fav.tags.split(Regex("[\\s,]+"))
            val seenInFav = HashSet<String>()
            for (rawTag in tags) {
                val tag = rawTag.trim().lowercase().trim(',', ';', '.', '(', ')', '"', '\'')
                if (!TagClassifier.isRecommendationCandidate(tag)) continue
                if (tag in blocked) continue
                val category = TagClassifier.classify(tag).category
                val multiplier = when (category) {
                    TagCategory.CHARACTER -> 5.0f
                    TagCategory.COPYRIGHT -> 4.0f
                    TagCategory.ARTIST -> 3.5f
                    TagCategory.GENERAL -> 1.0f
                    else -> 0.2f
                }
                tagWeights[tag] = (tagWeights[tag] ?: 0f) + (3f * multiplier)
                if (seenInFav.add(tag)) docFreq[tag] = (docFreq[tag] ?: 0) + 1
            }
        }
        if (favorites.size >= 8) {
            for ((tag, df) in docFreq) {
                val share = df.toFloat() / favorites.size
                if (share > 0.4f) tagWeights[tag] = (tagWeights[tag] ?: 0f) * 0.3f
            }
        }
        for ((tag, count) in recorded) {
            val clean = tag.trim().lowercase().trim(',', ';', '.', '(', ')', '"', '\'')
            if (!TagClassifier.isRecommendationCandidate(clean)) continue
            if (clean in blocked) continue
            val category = TagClassifier.classify(clean).category
            val multiplier = when (category) {
                TagCategory.CHARACTER -> 2.5f
                TagCategory.COPYRIGHT -> 2.0f
                TagCategory.ARTIST -> 2.0f
                else -> 1.0f
            }
            tagWeights[clean] = (tagWeights[clean] ?: 0f) + (count.toFloat() * multiplier)
        }
        return tagWeights.entries
            .sortedByDescending { it.value }
            .take(60)
            .map { it.key }
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

    private var cachedCredentials: BooruCredentials? = null

    fun getCredentials(): BooruCredentials {
        cachedCredentials?.let { return it }
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
        ).also { cachedCredentials = it }
    }

    fun refreshFeedIfNeeded() {
        if (needsFeedRefresh) {
            needsFeedRefresh = false
            search(source, query, safeMode)
        }
    }

    fun scrollToTop() {
        scrollToTopTrigger = System.currentTimeMillis()
    }

    fun refresh(isPull: Boolean = false) {
        refreshSeed++
        if (isPull) {
            isPullRefreshing = true
            isTabRefreshing = false
        } else {
            isTabRefreshing = true
            isPullRefreshing = false
        }
        search(source, query, safeMode, isPullRefresh = true)
    }

    private data class FeedParams(
        val source: String,
        val tags: String,
        val safeMode: Boolean,
        val excludeSafe: Boolean,
        val noAi: Boolean,
        val sortOrder: SortOrder,
        val contentTypes: Set<ContentType>,
        val blacklist: List<String>,
        val credentials: BooruCredentials,
        val customSources: List<CustomBooruSource>,
        val disabledSourceKeys: Set<String>
    )

    private data class FeedBatch(
        val items: List<RemoteMedia>,
        val lastPage: Int,
        val hasMore: Boolean
    )

    private fun snapshotParams(source: String, tags: String, safeMode: Boolean) = FeedParams(
        source = source,
        tags = tags,
        safeMode = safeMode,
        excludeSafe = excludeSafe,
        noAi = noAi,
        sortOrder = sortOrder,
        contentTypes = selectedContentTypes,
        blacklist = tagBlacklist,
        credentials = getCredentials(),
        customSources = customSources,
        disabledSourceKeys = disabledSources
    )

    private val recTagPages = HashMap<String, Int>()

    private fun advanceRecTagPages(tags: List<String>) {
        for (tag in tags) recTagPages[tag] = (recTagPages[tag] ?: 0) + 1
    }

    private fun activeRecommendationTags(): List<String> {
        val blocked = tagBlacklist.map { it.lowercase() }.toHashSet()
        return recommendationTags
            .filter { TagClassifier.isRecommendationCandidate(it) && it.lowercase() !in blocked }
            .take(REC_ACTIVE_POOL)
    }

    private suspend fun FeedParams.page(tags: String, page: Int, sort: SortOrder = sortOrder, limit: Int = BooruRepository.PAGE_SIZE, excludeSources: Set<String> = emptySet()): SearchPage =
        repo.searchPage(
            source = source,
            tags = tags,
            safeMode = safeMode,
            excludeSafe = excludeSafe,
            noAi = noAi,
            page = page,
            sortOrder = sort,
            contentTypes = contentTypes,
            credentials = credentials,
            customSources = customSources,
            limit = limit,
            excludeSources = excludeSources + disabledSourceKeys
        )

    private fun FeedParams.accepts(item: RemoteMedia): Boolean {
        if (com.booru.app.data.TagBlacklistMatcher.isBlacklisted(item, blacklist)) return false
        if (contentTypes.isEmpty()) return true
        return (contentTypes.contains(ContentType.PHOTOS) && !item.isVideo && !item.isGif) ||
            (contentTypes.contains(ContentType.VIDEOS) && item.isVideo) ||
            (contentTypes.contains(ContentType.GIFS) && item.isGif)
    }

    private suspend fun FeedParams.recommendationPage(page: Int, recTags: List<String>, tagPages: Map<String, Int>, existingKeys: Set<String>): Pair<List<RemoteMedia>, Boolean> = coroutineScope {
        val general = async {
            runCatching { page(tags = "", page = page, sort = if (sortOrder == SortOrder.RANDOM) SortOrder.RANDOM else SortOrder.NEWEST) }
        }
        val tagged = recTags.map { recTag ->
            async {
                runCatching {
                    page(tags = recTag, page = tagPages[recTag] ?: 0, limit = REC_TAG_PAGE_LIMIT, excludeSources = REC_TAG_EXCLUDED_SOURCES)
                }
            }
        }
        val generalResult = general.await()
        val taggedResults = tagged.awaitAll()
        if (generalResult.isFailure && taggedResults.none { it.isSuccess }) {
            throw generalResult.exceptionOrNull() ?: BooruException("Failed to load data")
        }
        val hideVideos = contentTypes.isNotEmpty() && !contentTypes.contains(ContentType.VIDEOS)
        val genList = generalResult.getOrNull()?.items.orEmpty().let { list -> if (hideVideos) list.filterNot { it.isVideo } else list }
        val tagLists = taggedResults.map { r -> r.getOrNull()?.items.orEmpty().let { list -> if (hideVideos) list.filterNot { it.isVideo } else list } }
        val blended = withContext(Dispatchers.Default) {
            blendRecommendationFeed(
                genList,
                tagLists,
                recommendationRatio,
                existingKeys,
                maxPerTag = (REC_MAX_PER_TAG * (0.5f + recommendationRatio)).toInt().coerceIn(3, REC_TAG_PAGE_LIMIT)
            )
        }
        val hasMore = (generalResult.getOrNull()?.rawCount ?: 0) > 0 || taggedResults.any { (it.getOrNull()?.rawCount ?: 0) > 0 }
        blended to hasMore
    }

    private suspend fun FeedParams.fetchFeed(startPage: Int, existingKeys: Set<String>, recTags: List<String>?, tagPages: Map<String, Int> = emptyMap()): FeedBatch {
        val (firstItems, firstHasMore) = if (recTags != null) {
            recommendationPage(startPage, recTags, tagPages, existingKeys)
        } else {
            val result = page(tags = tags, page = startPage)
            result.items to (result.rawCount > 0)
        }

        val seen = existingKeys.toHashSet()
        val collected = mutableListOf<RemoteMedia>()
        suspend fun absorb(items: List<RemoteMedia>): Int {
            val accepted = withContext(Dispatchers.Default) { items.filter { accepts(it) } }
            var added = 0
            for (item in accepted) {
                if (seen.add(item.mediaKey)) {
                    collected.add(item)
                    added++
                }
            }
            return added
        }
        absorb(firstItems)

        var lastPage = startPage
        var hasMore = firstHasMore
        val target = if (contentTypes.isNotEmpty()) FILTERED_TARGET_COUNT else 1
        val maxExtraPages = if (contentTypes.isNotEmpty()) 10 else 3
        var extraPages = 0
        var dryPages = 0
        while (hasMore && collected.size < target && extraPages < maxExtraPages && dryPages < 3) {
            lastPage++
            extraPages++
            val next = page(tags = tags, page = lastPage)
            hasMore = next.rawCount > 0
            if (absorb(next.items) == 0) dryPages++ else dryPages = 0
        }
        return FeedBatch(collected, lastPage, hasMore)
    }

    private fun applyFailure(e: Throwable, keepResults: Boolean) {
        if (!keepResults) {
            results = emptyList()
            hasMore = false
        }
        isNetworkError = e is BooruNetworkException || e is IOException
        when (e) {
            is BooruAuthException -> {
                isAuthError = true
                authErrorSource = e.sourceKey
                authErrorCode = e.statusCode
            }
            is BooruHttpException -> {
                isAuthError = e.statusCode == 401 || e.statusCode == 403
                authErrorSource = e.sourceKey
                authErrorCode = e.statusCode
            }
            else -> {
                isAuthError = false
                authErrorSource = null
                authErrorCode = null
            }
        }
        error = e.message ?: "Failed to load data"
    }

    fun search(
        source: String = this.source,
        tags: String = this.query,
        safeMode: Boolean = this.safeMode,
        isPullRefresh: Boolean = false
    ) {
        val searchGen = ++currentSearchGeneration
        recTagPages.clear()
        searchJob?.cancel()
        loadMoreJob?.cancel()
        loadMoreToken++
        loadingMore = false
        loadMoreError = false
        emptyLoadMoreStreak = 0

        this.source = source
        this.query = tags
        this.safeMode = safeMode
        needsFeedRefresh = false
        currentPage = 0
        hasMore = true
        val keepResults = isPullRefresh && results.isNotEmpty()
        if (isPullRefresh) {
            isRefreshing = true
        } else {
            loading = true
            isRefreshing = false
            isPullRefreshing = false
            isTabRefreshing = false
            results = emptyList()
        }
        error = null
        isNetworkError = false
        isAuthError = false
        authErrorSource = null
        authErrorCode = null

        val trimmedTags = tags.trim()
        activeTagCount = if (trimmedTags.isNotEmpty()) {
            tagSuggestions.find { it.value.equals(trimmedTags, ignoreCase = true) }?.count ?: 0
        } else 0

        if (trimmedTags.isNotEmpty() && !isIncognito) {
            viewModelScope.launch {
                prefs.saveSearchQuery(trimmedTags)
                prefs.recordSearchTags(trimmedTags.split(Regex("[\\s,]+")))
            }
        }
        if (trimmedTags.isNotEmpty() && activeTagCount == 0 && !trimmedTags.contains(" ")) {
            val lookupSources = customSources
            viewModelScope.launch {
                val match = runCatching { repo.getTagSuggestions(source, trimmedTags, lookupSources) }
                    .getOrDefault(emptyList())
                    .find { it.value.equals(trimmedTags, ignoreCase = true) }
                if (match != null && match.count > 0 && searchGen == currentSearchGeneration) {
                    activeTagCount = match.count
                }
            }
        }

        val params = snapshotParams(source, tags, safeMode)
        val recTags = if (trimmedTags.isEmpty() && recommendationRatio > 0.05f) {
            val active = activeRecommendationTags()
            if (active.size <= REC_TAGS_PER_PAGE) active else active.shuffled(java.util.Random(System.nanoTime() + refreshSeed)).take(REC_TAGS_PER_PAGE)
        } else emptyList()

        searchJob = viewModelScope.launch {
            try {
                var batch = params.fetchFeed(0, emptySet(), recTags.ifEmpty { null })
                if (batch.items.isEmpty() && recTags.isNotEmpty()) {
                    batch = params.fetchFeed(0, emptySet(), null)
                }
                if (searchGen != currentSearchGeneration) return@launch
                advanceRecTagPages(recTags)
                currentPage = batch.lastPage
                results = batch.items
                hasMore = batch.hasMore
                resultsEpoch++
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                if (searchGen == currentSearchGeneration) applyFailure(e, keepResults)
            } finally {
                if (searchGen == currentSearchGeneration) {
                    loading = false
                    isRefreshing = false
                    isPullRefreshing = false
                    isTabRefreshing = false
                }
            }
        }
    }

    fun loadMore() {
        if (loading || isRefreshing || loadingMore || !hasMore || loadMoreError) return
        val searchGen = currentSearchGeneration
        val token = ++loadMoreToken
        val targetPage = currentPage + 1
        val params = snapshotParams(source, query, safeMode)
        val recTags = if (query.isBlank() && recommendationRatio > 0.05f) {
            val active = activeRecommendationTags()
            if (active.size <= REC_TAGS_PER_PAGE) {
                active
            } else {
                val startIndex = (targetPage * REC_TAGS_PER_PAGE) % active.size
                (0 until REC_TAGS_PER_PAGE).map { offset -> active[(startIndex + offset) % active.size] }
            }
        } else emptyList()
        val existingKeys = results.mapTo(HashSet()) { it.mediaKey }
        val tagPages = recTags.associateWith { recTagPages[it] ?: 0 }

        loadMoreJob?.cancel()
        loadingMore = true
        loadMoreJob = viewModelScope.launch {
            try {
                val batch = params.fetchFeed(targetPage, existingKeys, recTags.ifEmpty { null }, tagPages)
                if (searchGen != currentSearchGeneration) return@launch
                advanceRecTagPages(recTags)
                currentPage = batch.lastPage
                if (batch.items.isNotEmpty()) {
                    emptyLoadMoreStreak = 0
                    results = (results + batch.items).distinctBy { it.mediaKey }
                    hasMore = batch.hasMore
                } else {
                    emptyLoadMoreStreak++
                    hasMore = batch.hasMore && emptyLoadMoreStreak < 3
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load more items: ${e.message}", e)
                if (searchGen == currentSearchGeneration) {
                    val isAuthFailure = e is BooruAuthException || (e is BooruHttpException && (e.statusCode == 401 || e.statusCode == 403))
                    if (isAuthFailure || results.isEmpty()) hasMore = false else loadMoreError = true
                }
            } finally {
                if (token == loadMoreToken) loadingMore = false
            }
        }
    }

    fun retryLoadMore() {
        loadMoreError = false
        loadMore()
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

    var suggestionsLoading by mutableStateOf(false); private set
    private val suggestionCache = object : LinkedHashMap<String, List<TagSuggestion>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<TagSuggestion>>?): Boolean = size > 48
    }

    fun fetchTagSuggestions(input: String) {
        suggestionJob?.cancel()
        val token = input.trim().trimStart('-', '~', '+').trimEnd('*').lowercase()
        if (token.length < 2) {
            tagSuggestions = emptyList()
            suggestionsLoading = false
            return
        }
        val cacheKey = "$source|$token"
        suggestionCache[cacheKey]?.let {
            tagSuggestions = it
            suggestionsLoading = false
            return
        }
        val lookupSource = source
        val lookupCustom = customSources
        suggestionJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            suggestionsLoading = true
            try {
                val suggestions = repo.getTagSuggestions(lookupSource, token, lookupCustom)
                if (suggestions.isNotEmpty()) suggestionCache[cacheKey] = suggestions
                tagSuggestions = suggestions
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                tagSuggestions = emptyList()
            } finally {
                suggestionsLoading = false
            }
        }
    }

    fun clearTagSuggestions() {
        suggestionJob?.cancel()
        suggestionsLoading = false
        tagSuggestions = emptyList()
    }

    private val favoriteMediaJobs = HashMap<String, Job>()

    fun toggleFavorite(media: RemoteMedia) {
        val key = media.mediaKey
        val wasFav = key in favoriteKeys
        if (wasFav) {
            favoriteKeys = favoriteKeys - key
            favoritesList = favoritesList.filterNot { it.mediaKey == key }
        } else {
            favoriteKeys = favoriteKeys + key
            favoritesList = listOf(media) + favoritesList
        }
        val recordTags = !wasFav && !isIncognito
        viewModelScope.launch {
            favoriteMutex.withLock {
                if (wasFav) {
                    favoriteDao.deleteByKey(key)
                } else {
                    favoriteDao.insert(FavoriteEntity.fromRemoteMedia(media))
                }
            }
            if (recordTags) prefs.recordFavoriteTags(media.tags)
        }
        favoriteMediaJobs.remove(key)?.cancel()
        val job = viewModelScope.launch {
            try {
                if (wasFav) {
                    BooruCacheManager.removeFavoriteMedia(getApplication(), media, favoritesList)
                } else {
                    BooruCacheManager.saveFavoriteMedia(getApplication(), media)
                }
                updateCacheSize()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "Favorite media sync failed: ${e.message}")
            } finally {
                if (favoriteMediaJobs[key] === coroutineContext[Job]) favoriteMediaJobs.remove(key)
            }
        }
        favoriteMediaJobs[key] = job
    }

    fun clearFavorites() {
        favoriteMediaJobs.values.forEach { it.cancel() }
        favoriteMediaJobs.clear()
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
            search(source, query, safeMode, isPullRefresh = false)
        }
    }

    fun toggleIncognito() {
        isIncognito = !isIncognito
    }

    fun updateRecommendationRatio(ratio: Float) {
        recommendationRatio = ratio.coerceIn(0f, 1f)
        viewModelScope.launch {
            prefs.setRecommendationRatio(ratio)
        }
    }

    var hasUnlockedSession = false; private set
    var lastBackgroundAt = 0L; private set

    fun markSessionUnlocked(now: Long) {
        hasUnlockedSession = true
        lastBackgroundAt = now
    }

    fun markSessionLocked() {
        hasUnlockedSession = false
    }

    fun markBackgrounded(now: Long) {
        lastBackgroundAt = now
    }

    fun setBiometricLock(enabled: Boolean, timeoutMin: Int = biometricLockTimeoutMin) {
        biometricLockEnabled = enabled
        biometricLockTimeoutMin = timeoutMin
        viewModelScope.launch {
            prefs.setBiometricLockEnabled(enabled)
            prefs.setBiometricLockTimeoutMin(timeoutMin)
        }
    }

    fun setBiometricLockTimeout(timeoutMin: Int) {
        biometricLockTimeoutMin = timeoutMin
        viewModelScope.launch {
            prefs.setBiometricLockTimeoutMin(timeoutMin)
        }
    }

    fun getMediaFolder(media: RemoteMedia): String? =
        favoriteFolders[media.mediaKey] ?: media.id.takeIf { it.isNotBlank() }?.let { favoriteFolders[it] }

    fun setMediaFolder(media: RemoteMedia, folder: String?) {
        val map = favoriteFolders.toMutableMap()
        val legacyKey = media.id.takeIf { it.isNotBlank() && map.containsKey(it) }
        if (legacyKey != null) map.remove(legacyKey)
        if (folder == null) map.remove(media.mediaKey) else map[media.mediaKey] = folder
        favoriteFolders = map
        viewModelScope.launch {
            if (legacyKey != null) prefs.setMediaFolder(legacyKey, null)
            prefs.setMediaFolder(media.mediaKey, folder)
        }
    }

    fun addCustomFolder(name: String) {
        if (name.isBlank() || customFolders.any { it.equals(name, ignoreCase = true) }) return
        if (name !in customFolders) {
            customFolders = customFolders + name
        }
        viewModelScope.launch {
            prefs.addCustomFolder(name)
        }
    }

    fun removeCustomFolder(name: String) {
        customFolders = customFolders - name
        val orphaned = favoriteFolders.filterValues { it == name }.keys
        if (orphaned.isNotEmpty()) favoriteFolders = favoriteFolders - orphaned
        viewModelScope.launch {
            prefs.removeCustomFolder(name)
            orphaned.forEach { prefs.setMediaFolder(it, null) }
        }
    }

    fun setGridColumns(cols: Int) {
        gridColumnsCount = cols.coerceIn(0, 4)
        viewModelScope.launch {
            prefs.setGridColumnsCount(cols)
        }
    }

    fun removeSearchHistoryItem(item: String) {
        viewModelScope.launch {
            prefs.removeSearchQuery(item)
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
        cachedCredentials = null
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
        cachedCredentials = null
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
        recordedRecTagsMap = emptyMap()
        viewModelScope.launch {
            prefs.setDefaultSource(BooruRepository.SOURCE_ALL)
            prefs.clearRecommendationData()
            recalculateRecommendationTags()
            onDone()
        }
    }

    fun addBlacklistedTag(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isBlank()) return
        if (results.isNotEmpty()) {
            results = results.filterNot { isBlacklisted(it, tagBlacklist + clean) }
        }
        if (results.size < BooruRepository.PAGE_SIZE / 2) needsFeedRefresh = true
        viewModelScope.launch {
            prefs.addTagToBlacklist(clean)
        }
    }

    fun removeBlacklistedTag(tag: String) {
        needsFeedRefresh = true
        viewModelScope.launch {
            prefs.removeTagFromBlacklist(tag)
        }
    }

    fun clearBlacklist() {
        needsFeedRefresh = true
        viewModelScope.launch {
            prefs.clearTagBlacklist()
        }
    }

    fun clearError() {
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
    }

    var disabledSources by mutableStateOf<Set<String>>(emptySet()); private set

    val toggleableSources: List<String>
        get() = BooruRepository.AVAILABLE_SOURCES.filter { it in BUILT_IN_SOURCE_KEYS }

    fun isSourceEnabled(source: String): Boolean =
        BUILT_IN_SOURCE_KEYS[source]?.let { it !in disabledSources } ?: true

    fun setSourceEnabled(source: String, enabled: Boolean) {
        val key = BUILT_IN_SOURCE_KEYS[source] ?: return
        val updated = if (enabled) disabledSources - key else disabledSources + key
        if (updated.size >= BUILT_IN_SOURCE_KEYS.size) return
        disabledSources = updated
        viewModelScope.launch { prefs.setDisabledSources(updated) }
        if (!enabled && this.source == source) {
            selectSource(BooruRepository.SOURCE_ALL)
        } else if (this.source == BooruRepository.SOURCE_ALL) {
            needsFeedRefresh = true
        }
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
        cachedCredentials = null
        viewModelScope.launch { prefs.saveCustomSources(updated) }
        return true
    }

    fun removeCustomSource(sourceId: String) {
        val target = customSources.find { it.id == sourceId }
        val updated = customSources.filterNot { it.id == sourceId }
        customSources = updated
        cachedCredentials = null
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
        get() = BooruRepository.AVAILABLE_SOURCES.filter { isSourceEnabled(it) } + customSources.filter { it.enabled }.map { it.id }

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
    private var fullscreenOpenCounter = 0L

    fun openFullscreen(list: List<RemoteMedia>, index: Int) {
        if (list.isNotEmpty()) {
            fullscreenState = FullscreenState(
                list = list,
                index = index.coerceIn(0, list.size - 1),
                isFromResults = (list === results),
                openId = ++fullscreenOpenCounter
            )
        }
    }

    var activeDownloads by mutableStateOf<Set<String>>(emptySet()); private set
    var isSettingWallpaper by mutableStateOf(false); private set

    private fun toast(message: String, long: Boolean = false) {
        android.widget.Toast.makeText(
            getApplication(),
            message,
            if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun downloadMedia(media: RemoteMedia) {
        val key = media.mediaKey
        if (key in activeDownloads) return
        activeDownloads = activeDownloads + key
        val lang = language
        toast(com.booru.app.data.Strings.loadingOriginal(lang))
        viewModelScope.launch {
            try {
                com.booru.app.ui.MediaActionHandler.downloadMedia(getApplication(), media, ImageQuality.ORIGINAL)
                    .onSuccess { filename -> toast("${com.booru.app.data.Strings.downloadSuccess(lang)}: $filename", long = true) }
                    .onFailure { e -> toast("${com.booru.app.data.Strings.downloadFailed(lang)}: ${e.message}", long = true) }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                toast("${com.booru.app.data.Strings.downloadFailed(lang)}: ${e.message}", long = true)
            } finally {
                activeDownloads = activeDownloads - key
            }
        }
    }

    fun applyWallpaper(target: Int, media: RemoteMedia) {
        if (isSettingWallpaper) return
        isSettingWallpaper = true
        val lang = language
        toast(com.booru.app.data.Strings.settingWallpaper(lang))
        viewModelScope.launch {
            try {
                com.booru.app.ui.MediaActionHandler.applyWallpaper(getApplication(), target, media)
                    .onSuccess { toast(com.booru.app.data.Strings.wallpaperSuccess(lang)) }
                    .onFailure { e -> toast("${com.booru.app.data.Strings.wallpaperFailed(lang)}: ${e.message}") }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                toast("${com.booru.app.data.Strings.wallpaperFailed(lang)}: ${e.message}")
            } finally {
                isSettingWallpaper = false
            }
        }
    }

    fun updateFullscreenIndex(index: Int) {
        val state = fullscreenState ?: return
        if (state.index != index && index >= 0) {
            fullscreenState = state.copy(index = index)
        }
    }

    fun closeFullscreen() {
        fullscreenState = null
    }

    companion object {
        private const val TAG = "GalleryViewModel"
        private const val SUGGESTION_DEBOUNCE_MS = 220L
        private const val REC_TAGS_PER_PAGE = 4
        private const val REC_ACTIVE_POOL = 24
        private const val REC_MAX_PER_TAG = 6
        private const val REC_TAG_PAGE_LIMIT = 15
        private const val FILTERED_TARGET_COUNT = 24
        private val REC_TAG_EXCLUDED_SOURCES = setOf("realbooru")
        private val BUILT_IN_SOURCE_KEYS = mapOf(
            BooruRepository.SOURCE_RULE34 to "rule34",
            BooruRepository.SOURCE_GELBOORU to "gelbooru",
            BooruRepository.SOURCE_REALBOORU to "realbooru",
            BooruRepository.SOURCE_XBOORU to "xbooru",
            BooruRepository.SOURCE_TBIB to "tbib",
            BooruRepository.SOURCE_YANDE to "yande",
            BooruRepository.SOURCE_KONACHAN to "konachan",
            BooruRepository.SOURCE_SAFEBOORU to "safebooru"
        )

        fun getRecommendationRatio(tagCount: Int): Float {
            return when (tagCount) {
                0 -> 0.0f
                1 -> 0.65f
                2 -> 0.75f
                3 -> 0.80f
                else -> 0.85f
            }
        }

        fun blendRecommendationFeed(
            generalPosts: List<RemoteMedia>,
            tagPosts: List<List<RemoteMedia>>,
            ratio: Float,
            existingKeys: Set<String> = emptySet(),
            maxPerTag: Int = Int.MAX_VALUE
        ): List<RemoteMedia> {
            val nonNullGeneral = generalPosts.filterNot { it.mediaKey in existingKeys }
            val nonNullTagPosts = tagPosts.map { list ->
                list.filterNot { it.mediaKey in existingKeys }.take(maxPerTag)
            }

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
            if (recQueue.size >= 6) {
                val maxGeneral = kotlin.math.ceil(recQueue.size * (1f - clampedRatio) / clampedRatio).toInt()
                while (genQueue.size > maxGeneral) genQueue.removeLast()
            }

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
    val isFromResults: Boolean = false,
    val openId: Long = 0L
)
