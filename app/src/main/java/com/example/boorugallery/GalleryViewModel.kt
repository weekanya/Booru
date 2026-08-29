package com.example.boorugallery

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.boorugallery.data.AppLanguage
import com.example.boorugallery.data.BooruPreferences
import com.example.boorugallery.ui.AppPalette
import com.example.boorugallery.ui.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BooruRepository()
    val prefs = BooruPreferences(application)

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
            prefs.rule34UserId.collect { rule34UserId = it }
        }
        viewModelScope.launch {
            prefs.rule34ApiKey.collect { rule34ApiKey = it }
        }
        viewModelScope.launch {
            prefs.gelbooruUserId.collect { gelbooruUserId = it }
        }
        viewModelScope.launch {
            prefs.gelbooruApiKey.collect { gelbooruApiKey = it }
        }
        viewModelScope.launch {
            prefs.favorites.collect { list ->
                updateFavoritesState(list)
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
    }

    private fun updateFavoritesState(list: List<RemoteMedia>) {
        favoritesList = list
        favoriteIds = list.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }.toSet()
        favoriteUrls = list.mapNotNull { it.url.takeIf { url -> url.isNotBlank() } }.toSet()
    }

    fun getSourceDisplayName(key: String): String = repo.getSourceDisplayName(key)

    private fun getCredentials() = BooruCredentials(
        rule34UserId = rule34UserId,
        rule34ApiKey = rule34ApiKey,
        gelbooruUserId = gelbooruUserId,
        gelbooruApiKey = gelbooruApiKey
    )

    fun isBlacklisted(media: RemoteMedia, blacklist: List<String> = tagBlacklist): Boolean {
        if (blacklist.isEmpty()) return false
        val mediaTags = media.tags.lowercase().split("\\s+".toRegex()).toSet()
        return blacklist.any { bl ->
            val clean = bl.trim().lowercase()
            clean.isNotBlank() && (clean in mediaTags || media.tags.contains(clean, ignoreCase = true))
        }
    }

    fun search(newSource: String, newQuery: String, newSafeMode: Boolean) {
        source   = newSource
        query    = newQuery
        safeMode = newSafeMode
        currentPage = 0
        hasMore  = true
        error    = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null

        if (newQuery.isNotBlank()) {
            viewModelScope.launch { prefs.saveSearchQuery(newQuery) }
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            loading = true
            results = emptyList()
            try {
                val list = repo.search(
                    source = source,
                    tags = query,
                    safeMode = safeMode,
                    excludeSafe = excludeSafe,
                    noAi = noAi,
                    page = 0,
                    sortOrder = sortOrder,
                    credentials = getCredentials()
                )
                results = list.filterNot { isBlacklisted(it) }
                hasMore = list.size >= 20
            } catch (authEx: BooruAuthException) {
                isAuthError = true
                authErrorSource = authEx.sourceKey
                authErrorCode = authEx.statusCode
                error = authEx.message
            } catch (httpEx: BooruHttpException) {
                isAuthError = false
                authErrorSource = httpEx.sourceKey
                authErrorCode = httpEx.statusCode
                error = httpEx.message
            } catch (c: kotlinx.coroutines.CancellationException) {

            } catch (e: Exception) {
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
                hasMore = list.size >= 20
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
        val current = favoritesList.toMutableList()
        val existing = current.indexOfFirst {
            (media.id.isNotBlank() && it.id == media.id) ||
            (media.url.isNotBlank() && it.url == media.url)
        }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(0, media)
        }
        updateFavoritesState(current)
        viewModelScope.launch {
            prefs.saveFavorites(current)
        }
    }

    fun clearFavorites() {
        updateFavoritesState(emptyList())
        viewModelScope.launch {
            prefs.saveFavorites(emptyList())
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
        viewModelScope.launch {
            prefs.setRule34Credentials(userId, apiKey)
        }
    }

    fun saveGelbooruKeys(userId: String, apiKey: String) {
        viewModelScope.launch {
            prefs.setGelbooruCredentials(userId, apiKey)
        }
    }

    fun removeFromHistory(query: String) {
        viewModelScope.launch { prefs.removeSearchQuery(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun addBlacklistedTag(tag: String) {
        viewModelScope.launch { prefs.addTagToBlacklist(tag) }
    }

    fun removeBlacklistedTag(tag: String) {
        viewModelScope.launch { prefs.removeTagFromBlacklist(tag) }
    }

    fun clearBlacklist() {
        viewModelScope.launch { prefs.clearTagBlacklist() }
    }

    fun clearError() {
        error = null
        isAuthError = false
        authErrorSource = null
        authErrorCode = null
    }
}
