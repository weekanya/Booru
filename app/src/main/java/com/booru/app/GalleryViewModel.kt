package com.booru.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booru.app.data.AppLanguage
import com.booru.app.data.AppUpdateInfo
import com.booru.app.data.BooruCacheManager
import com.booru.app.data.BooruPreferences
import com.booru.app.data.UpdateChecker
import com.booru.app.data.db.AppDatabase
import com.booru.app.data.db.FavoriteEntity
import com.booru.app.data.security.SecureCredentialsStorage
import com.booru.app.ui.AppPalette
import com.booru.app.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BooruRepository()
    val prefs = BooruPreferences(application)
    private val secureStorage = SecureCredentialsStorage(application)
    private val favoriteDao = AppDatabase.getDatabase(application).favoriteDao()

    var updateInfo by mutableStateOf<AppUpdateInfo?>(null); private set
    var isCheckingUpdate by mutableStateOf(false); private set
    var manualCheckResult by mutableStateOf<String?>(null); private set

    var cacheSizeFormatted by mutableStateOf("0 B"); private set
    var isClearingCache by mutableStateOf(false); private set

    var results     by mutableStateOf<List<RemoteMedia>>(emptyList()); private set
    var loading     by mutableStateOf(false);                           private set
    var loadingMore by mutableStateOf(false);                           private set
    var error           by mutableStateOf<String?>(null);  private set
    var isAuthError     by mutableStateOf(false);          private set
    var authErrorSource by mutableStateOf<String?>(null);  private set
    var authErrorCode   by mutableStateOf<Int?>(null);     private set

    var query       by mutableStateOf("");                             private set
    var source      by mutableStateOf(BooruRepository.SOURCE_ALL);    private set
    var safeMode    by mutableStateOf(false);                          private set
    var excludeSafe by mutableStateOf(false);                          private set
    var noAi        by mutableStateOf(false);                          private set
    var sortOrder   by mutableStateOf(SortOrder.NEWEST);             private set

    var themeMode       by mutableStateOf(ThemeMode.SYSTEM); private set
    var palette         by mutableStateOf(AppPalette.MONET); private set
    var useDynamicColor by mutableStateOf(true);             private set
    var language        by mutableStateOf(AppLanguage.ENGLISH); private set

    var rule34UserId   by mutableStateOf(""); private set
    var rule34ApiKey   by mutableStateOf(""); private set
    var gelbooruUserId by mutableStateOf(""); private set
    var gelbooruApiKey by mutableStateOf(""); private set

    var favoritesList by mutableStateOf<List<RemoteMedia>>(emptyList()); private set
    var favoriteIds   by mutableStateOf<Set<String>>(emptySet());        private set
    var favoriteUrls  by mutableStateOf<Set<String>>(emptySet());        private set

    var searchHistory  by mutableStateOf<List<String>>(emptyList());        private set
    var tagSuggestions by mutableStateOf<List<TagSuggestion>>(emptyList()); private set
    var tagBlacklist   by mutableStateOf<List<String>>(emptyList());        private set

    private var currentPage = 0
    private var hasMore = true
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    init {

        rule34UserId = secureStorage.getRule34UserId()
        rule34ApiKey = secureStorage.getRule34ApiKey()
        gelbooruUserId = secureStorage.getGelbooruUserId()
        gelbooruApiKey = secureStorage.getGelbooruApiKey()

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
                viewModelScope.launch(Dispatchers.IO) {
                    mediaList.forEach { media ->
                        BooruCacheManager.saveFavoriteMedia(getApplication(), media)
                    }
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                val legacyFavs = prefs.favorites.first()
                if (legacyFavs.isNotEmpty()) {
                    legacyFavs.forEach { fav ->
                        favoriteDao.insert(FavoriteEntity.fromRemoteMedia(fav))
                    }
                    prefs.saveFavorites(emptyList())
                }
            }
        }

        viewModelScope.launch {
            val initialSource = prefs.defaultSource.first()
            val initialSafe = prefs.safeMode.first()
            val initialExcludeSafe = prefs.excludeSafe.first()
            val initialNoAi = prefs.noAiFilter.first()
            val initialLang = prefs.language.first()
            val initialTheme = prefs.themeMode.first()
            val initialPalette = prefs.palette.first()
            source = initialSource
            safeMode = initialSafe
            excludeSafe = initialExcludeSafe
            noAi = initialNoAi
            language = initialLang
            themeMode = initialTheme
            palette = initialPalette
            search(source, "", safeMode)
        }

        viewModelScope.launch {
            checkForUpdates(isAutoCheck = true)
        }

        updateCacheSize()
    }

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdate = true
            manualCheckResult = null
            try {
                val currentVer = try {
                    val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                    pInfo.versionName ?: "3.0"
                } catch (e: Exception) {
                    "3.0"
                }

                val release = UpdateChecker.fetchLatestRelease()
                if (release != null && UpdateChecker.isNewerVersion(release.latestVersion, currentVer)) {
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
            } catch (e: Exception) {
                if (!isAutoCheck) {
                    manualCheckResult = "FAILED"
                }
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    fun ignoreUpdate(version: String) {
        viewModelScope.launch {
            prefs.setIgnoredUpdateVersion(version)
            updateInfo = null
        }
    }

    fun dismissUpdate() {
        updateInfo = null
    }

    fun clearManualCheckResult() {
        manualCheckResult = null
    }

    fun isBlacklisted(media: RemoteMedia, blacklist: List<String> = tagBlacklist): Boolean {
        if (blacklist.isEmpty()) return false
        val mediaTags = media.tagList.map { it.lowercase() }.toSet()
        return blacklist.any { bl ->
            val clean = bl.trim().lowercase()
            clean.isNotBlank() && clean in mediaTags
        }
    }

    private fun updateFavoritesState(list: List<RemoteMedia>) {
        favoritesList = list
        favoriteIds = list.mapNotNull { it.id.ifBlank { null } }.toSet()
        favoriteUrls = list.mapNotNull { it.url.ifBlank { null } }.toSet()
    }

    fun selectSource(newSource: String) {
        if (source == newSource) return
        source = newSource
        viewModelScope.launch {
            prefs.setDefaultSource(newSource)
        }
        search(newSource, query, safeMode)
    }

    var needsFeedRefresh by mutableStateOf(false);                     private set

    fun getCredentials(): BooruCredentials {
        return BooruCredentials(
            rule34UserId = rule34UserId,
            rule34ApiKey = rule34ApiKey,
            gelbooruUserId = gelbooruUserId,
            gelbooruApiKey = gelbooruApiKey
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
        searchJob?.cancel()
        this.source = source
        this.query = tags
        this.safeMode = safeMode
        currentPage = 0
        hasMore = true
        loading = true
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
        results = emptyList()

        val trimmedTags = tags.trim()
        if (trimmedTags.isNotEmpty()) {
            viewModelScope.launch { prefs.saveSearchQuery(trimmedTags) }
        }

        searchJob = viewModelScope.launch {
            try {
                val list = repo.search(
                    source = source,
                    tags = tags,
                    safeMode = safeMode,
                    excludeSafe = excludeSafe,
                    noAi = noAi,
                    page = 0,
                    sortOrder = sortOrder,
                    credentials = getCredentials()
                )
                val filtered = list.filterNot { isBlacklisted(it) }
                results = filtered
                hasMore = list.size >= BooruRepository.PAGE_SIZE
            } catch (authEx: BooruAuthException) {
                results = emptyList()
                isAuthError = true
                authErrorSource = authEx.sourceKey
                authErrorCode = authEx.statusCode
                error = authEx.message
            } catch (httpEx: BooruHttpException) {
                results = emptyList()
                isAuthError = httpEx.statusCode == 401 || httpEx.statusCode == 403
                authErrorSource = httpEx.sourceKey
                authErrorCode = httpEx.statusCode
                error = httpEx.message
            } catch (c: kotlinx.coroutines.CancellationException) {

            } catch (e: Exception) {
                results = emptyList()
                isAuthError = false
                authErrorSource = null
                authErrorCode = null
                error = e.message ?: "Failed to load data"
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        currentPage++

        viewModelScope.launch {
            loadingMore = true
            try {
                val list = repo.search(
                    source = source,
                    tags = query,
                    safeMode = safeMode,
                    excludeSafe = excludeSafe,
                    noAi = noAi,
                    page = currentPage,
                    sortOrder = sortOrder,
                    credentials = getCredentials()
                )
                val filtered = list.filterNot { isBlacklisted(it) }
                results = results + filtered
                hasMore = list.size >= BooruRepository.PAGE_SIZE
            } catch (c: kotlinx.coroutines.CancellationException) {

            } catch (e: Exception) {
                currentPage--
            } finally {
                loadingMore = false
            }
        }
    }

    fun applySort(order: SortOrder) {
        if (sortOrder == order) return
        sortOrder = order
        search(source, query, safeMode)
    }

    fun searchTag(tag: String) {
        search(source, tag.trim(), safeMode)
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
            } catch (c: kotlinx.coroutines.CancellationException) {

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
            if (isFavorite(media)) {
                favoriteDao.deleteByUrl(media.url)
                BooruCacheManager.removeFavoriteMedia(getApplication(), media)
            } else {
                favoriteDao.insert(FavoriteEntity.fromRemoteMedia(media))
                BooruCacheManager.saveFavoriteMedia(getApplication(), media)
            }
            updateCacheSize()
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            val allFavs = favoritesList
            favoriteDao.clearAll()
            allFavs.forEach { BooruCacheManager.removeFavoriteMedia(getApplication(), it) }
            updateCacheSize()
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = BooruCacheManager.getCacheSizeBytes(getApplication())
            cacheSizeFormatted = BooruCacheManager.formatBytes(bytes)
        }
    }

    fun clearCache(onComplete: () -> Unit = {}) {
        if (isClearingCache) return
        viewModelScope.launch {
            isClearingCache = true
            BooruCacheManager.clearBrowsingCache(getApplication(), favoritesList)
            updateCacheSize()
            isClearingCache = false
            onComplete()
        }
    }

    fun isFavorite(media: RemoteMedia): Boolean {
        return (media.id.isNotBlank() && media.id in favoriteIds) ||
               (media.url.isNotBlank() && media.url in favoriteUrls)
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

    fun addBlacklistedTag(tag: String) {
        needsFeedRefresh = true
        viewModelScope.launch { prefs.addTagToBlacklist(tag) }
    }

    fun removeBlacklistedTag(tag: String) {
        needsFeedRefresh = true
        viewModelScope.launch { prefs.removeTagFromBlacklist(tag) }
    }

    fun clearBlacklist() {
        needsFeedRefresh = true
        viewModelScope.launch { prefs.clearTagBlacklist() }
    }

    fun clearError() {
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
    }

    fun getSourceDisplayName(key: String): String = BooruRepository.getSourceDisplayName(key)
}
