package com.booru.app.data

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский")
}

object Strings {
    fun navExplore(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Explore"
        AppLanguage.RUSSIAN -> "Обзор"
    }

    fun navFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Favorites"
        AppLanguage.RUSSIAN -> "Избранное"
    }

    fun navSettings(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.RUSSIAN -> "Настройки"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search tags..."
        AppLanguage.RUSSIAN -> "Поиск по тегам..."
    }

    fun tagSuggestions(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tag Suggestions"
        AppLanguage.RUSSIAN -> "Подсказки тегов"
    }

    fun recentSearches(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Recent Searches"
        AppLanguage.RUSSIAN -> "Недавние запросы"
    }

    fun clearAll(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear all"
        AppLanguage.RUSSIAN -> "Очистить всё"
    }

    fun allPosts(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All posts"
        AppLanguage.RUSSIAN -> "Все посты"
    }

    fun sortNewest(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Newest"
        AppLanguage.RUSSIAN -> "Новые"
    }

    fun sortScore(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "By Score"
        AppLanguage.RUSSIAN -> "По рейтингу"
    }

    fun sortRandom(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Random"
        AppLanguage.RUSSIAN -> "Случайно"
    }

    fun only18Badge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "18+ Only"
        AppLanguage.RUSSIAN -> "Только 18+"
    }

    fun safeModeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Safe Mode"
        AppLanguage.RUSSIAN -> "Безопасный"
    }

    fun allRatings(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All ratings"
        AppLanguage.RUSSIAN -> "Все рейтинги"
    }

    fun noAiBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No AI"
        AppLanguage.RUSSIAN -> "Без ИИ"
    }

    fun selectSourceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Choose Booru Source"
        AppLanguage.RUSSIAN -> "Выберите источник"
    }

    fun downloadBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.RUSSIAN -> "Скачать"
    }

    fun ratingSafe(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Safe"
        AppLanguage.RUSSIAN -> "Safe (0+)"
    }

    fun ratingQuestionable(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Questionable"
        AppLanguage.RUSSIAN -> "Questionable (16+)"
    }

    fun ratingExplicit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Explicit"
        AppLanguage.RUSSIAN -> "Explicit (18+)"
    }

    fun authSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "AUTHENTICATION & API"
        AppLanguage.RUSSIAN -> "АВТОРИЗАЦИЯ И API"
    }

    fun contentSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "CONTENT & FILTERING"
        AppLanguage.RUSSIAN -> "КОНТЕНТ И ФИЛЬТРАЦИЯ"
    }

    fun appearanceSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "APPEARANCE & THEMES"
        AppLanguage.RUSSIAN -> "ВНЕШНИЙ ВИД И ТЕМЫ"
    }

    fun languageSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "LANGUAGE"
        AppLanguage.RUSSIAN -> "ЯЗЫК ИНТЕРФЕЙСА"
    }

    fun languageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "App Language"
        AppLanguage.RUSSIAN -> "Язык приложения"
    }

    fun darkThemeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Theme Mode"
        AppLanguage.RUSSIAN -> "Режим темы"
    }

    fun colorPaletteTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Color Palette"
        AppLanguage.RUSSIAN -> "Цветовая схема"
    }

    fun dataSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "DATA & ABOUT"
        AppLanguage.RUSSIAN -> "ДАННЫЕ И О ПРИЛОЖЕНИИ"
    }

    fun aboutAppTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "About Booru"
        AppLanguage.RUSSIAN -> "О приложении"
    }

    fun aboutAppDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Booru • Version 3.3"
        AppLanguage.RUSSIAN -> "Booru • Версия 3.3"
    }

    fun checkUpdatesTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Check for Updates"
        AppLanguage.RUSSIAN -> "Проверить обновления"
    }

    fun checkUpdatesDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tap to check GitHub for new releases"
        AppLanguage.RUSSIAN -> "Нажмите для проверки свежих релизов"
    }

    fun updateAvailableTitle(lang: AppLanguage, version: String) = when (lang) {
        AppLanguage.ENGLISH -> "Update Available ($version)"
        AppLanguage.RUSSIAN -> "Доступно обновление ($version)"
    }

    fun updateAvailableDesc(lang: AppLanguage, version: String) = when (lang) {
        AppLanguage.ENGLISH -> "A new version ($version) is available on GitHub. Would you like to download it?"
        AppLanguage.RUSSIAN -> "На GitHub вышла новая версия ($version). Хотите скачать обновление?"
    }

    fun updateButton(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Download & Install"
        AppLanguage.RUSSIAN -> "Скачать и обновить"
    }

    fun downloadingUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Downloading update..."
        AppLanguage.RUSSIAN -> "Загрузка обновления..."
    }

    fun installUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.RUSSIAN -> "Установить"
    }

    fun updateDownloadFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Failed to download update APK"
        AppLanguage.RUSSIAN -> "Не удалось скачать APK обновления"
    }

    fun openInBrowser(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Open in Browser"
        AppLanguage.RUSSIAN -> "Открыть в браузере"
    }

    fun dontRemindAgain(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Don't remind again"
        AppLanguage.RUSSIAN -> "Больше не напоминать"
    }

    fun upToDateTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Latest Version Installed"
        AppLanguage.RUSSIAN -> "Установлена последняя версия"
    }

    fun upToDateDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "You are already using the newest version of Booru."
        AppLanguage.RUSSIAN -> "У вас уже установлена самая актуальная версия Booru."
    }

    fun updateCheckFailedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Check Failed"
        AppLanguage.RUSSIAN -> "Ошибка проверки"
    }

    fun updateCheckFailedDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Could not reach GitHub servers. Please check your internet connection."
        AppLanguage.RUSSIAN -> "Не удалось связаться с серверами GitHub. Проверьте интернет-соединение."
    }

    fun savedPostsCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "$count saved posts"
        AppLanguage.RUSSIAN -> "$count сохранённых постов"
    }

    fun favoritesEmptyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Favorites is empty"
        AppLanguage.RUSSIAN -> "В избранном пока пусто"
    }

    fun favoritesEmptyDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tap the heart icon on any post to save it here permanently."
        AppLanguage.RUSSIAN -> "Нажмите на иконку сердечка на карточке любого поста, чтобы сохранить его сюда насовсем."
    }

    fun goToExplore(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Go to Explore"
        AppLanguage.RUSSIAN -> "Перейти к обзору"
    }

    fun filterFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Filter favorites by tag..."
        AppLanguage.RUSSIAN -> "Фильтр по тегам в избранном..."
    }

    fun clearFavoritesConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear all favorites?"
        AppLanguage.RUSSIAN -> "Очистить избранное?"
    }

    fun clearFavoritesDesc(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All $count saved posts will be removed from favorites."
        AppLanguage.RUSSIAN -> "Все сохранённые посты ($count) будут удалены из избранного."
    }

    fun loadingOriginal(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Downloading original image..."
        AppLanguage.RUSSIAN -> "Скачивание в полном качестве начато..."
    }

    fun copyTag(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Copy Tag"
        AppLanguage.RUSSIAN -> "Скопировать тег"
    }

    fun tagCopied(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tag copied to clipboard"
        AppLanguage.RUSSIAN -> "Тег скопирован в буфер"
    }

    fun addToBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add to Blacklist"
        AppLanguage.RUSSIAN -> "В чёрный список"
    }

    fun removeFromBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Remove from Blacklist"
        AppLanguage.RUSSIAN -> "Удалить из чёрного списка"
    }

    fun tagAddedToBlacklist(tag: String, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "\"$tag\" added to blacklist"
        AppLanguage.RUSSIAN -> "«$tag» добавлен в чёрный список"
    }

    fun tagRemovedFromBlacklist(tag: String, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "\"$tag\" removed from blacklist"
        AppLanguage.RUSSIAN -> "«$tag» удален из чёрного списка"
    }

    fun saveBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.RUSSIAN -> "Сохранить"
    }

    fun cancelBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.RUSSIAN -> "Отмена"
    }

    fun clearBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.RUSSIAN -> "Очистить"
    }

    fun retryBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.RUSSIAN -> "Повторить"
    }

    fun refreshBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Refresh"
        AppLanguage.RUSSIAN -> "Обновить"
    }

    fun closeBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.RUSSIAN -> "Закрыть"
    }

    fun loadingText(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Loading..."
        AppLanguage.RUSSIAN -> "Загрузка..."
    }

    fun nothingFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Nothing found"
        AppLanguage.RUSSIAN -> "Ничего не найдено"
    }

    fun nothingFoundDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Try changing search tags, change mode, or choosing another source."
        AppLanguage.RUSSIAN -> "Попробуйте изменить поисковые теги, выбрать другой режим или выбрать другой источник."
    }

    fun authErrorTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Authentication Required"
        AppLanguage.RUSSIAN -> "Требуется авторизация"
    }

    fun authErrorDesc(source: String, code: Int? = null, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> if (code != null) {
            "Access restricted ($code). An API Key is required in Settings for $source."
        } else {
            "Source $source requires User ID and API Key. Open Settings to configure your credentials."
        }
        AppLanguage.RUSSIAN -> if (code != null) {
            "Доступ ограничен ($code). Требуется указать API Key в Настройках для $source."
        } else {
            "Для $source требуется User ID и API Key. Перейдите в Настройки для ввода ключей."
        }
    }

    fun httpErrorDesc(source: String, code: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Server returned HTTP $code from $source."
        AppLanguage.RUSSIAN -> "Сервер $source вернул ошибку HTTP $code."
    }

    fun genericErrorTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Error"
        AppLanguage.RUSSIAN -> "Ошибка"
    }

    fun enterApiKeyBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Enter API Key"
        AppLanguage.RUSSIAN -> "Ввести API Key"
    }

    fun failedToLoad(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Failed to load data. Please check your internet connection."
        AppLanguage.RUSSIAN -> "Не удалось загрузить данные. Проверьте интернет-соединение."
    }

    fun scoreLabel(score: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "$score score"
        AppLanguage.RUSSIAN -> "$score очков"
    }

    fun tagsLabel(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tags ($count)"
        AppLanguage.RUSSIAN -> "Теги ($count)"
    }

    fun keysSavedToast(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Keys saved!"
        AppLanguage.RUSSIAN -> "Ключи сохранены!"
    }

    fun rule34DialogDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Provide User ID and API Key to search Rule34."
        AppLanguage.RUSSIAN -> "Укажите User ID и API Key для поиска на Rule34."
    }

    fun gelbooruDialogDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Provide User ID and API Key to search Gelbooru."
        AppLanguage.RUSSIAN -> "Укажите User ID и API Key для поиска на Gelbooru."
    }

    fun getKeyFromSite(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Get key from website"
        AppLanguage.RUSSIAN -> "Получить ключ на сайте"
    }

    fun tapToEnterKeys(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tap to enter User ID & API Key"
        AppLanguage.RUSSIAN -> "Нажмите для ввода ключей"
    }

    fun themeModeSystem(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Auto (System)"
        AppLanguage.RUSSIAN -> "Авто (Системная)"
    }

    fun themeModeDark(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Dark theme"
        AppLanguage.RUSSIAN -> "Тёмная тема"
    }

    fun themeModeLight(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Light theme"
        AppLanguage.RUSSIAN -> "Светлая тема"
    }

    fun tagBlacklistTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tag Blacklist"
        AppLanguage.RUSSIAN -> "Чёрный список тегов"
    }

    fun tagBlacklistDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Hide posts containing specific tags from results"
        AppLanguage.RUSSIAN -> "Скрывать посты с указанными тегами из поиска"
    }

    fun addTagBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add"
        AppLanguage.RUSSIAN -> "Добавить"
    }

    fun addTagPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Enter tag to block..."
        AppLanguage.RUSSIAN -> "Введите тег для скрытия..."
    }

    fun noBlacklistedTags(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No blocked tags added yet"
        AppLanguage.RUSSIAN -> "Список заблокированных тегов пуст"
    }

    fun clearAllBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear all"
        AppLanguage.RUSSIAN -> "Очистить всё"
    }

    fun setWallpaperTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Set as Wallpaper"
        AppLanguage.RUSSIAN -> "Установить как обои"
    }

    fun wallpaperHomeScreen(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Home Screen"
        AppLanguage.RUSSIAN -> "Главный экран"
    }

    fun wallpaperLockScreen(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Lock Screen"
        AppLanguage.RUSSIAN -> "Экран блокировки"
    }

    fun wallpaperBoth(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Both Screens"
        AppLanguage.RUSSIAN -> "Оба экрана"
    }

    fun wallpaperSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Wallpaper set successfully!"
        AppLanguage.RUSSIAN -> "Обои успешно установлены!"
    }

    fun wallpaperFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Failed to set wallpaper"
        AppLanguage.RUSSIAN -> "Не удалось установить обои"
    }

    fun settingWallpaper(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Setting wallpaper..."
        AppLanguage.RUSSIAN -> "Установка обоев..."
    }

    fun downloadSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Saved to Gallery (Booru)"
        AppLanguage.RUSSIAN -> "Сохранено в галерею (Booru)"
    }

    fun downloadFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Download failed"
        AppLanguage.RUSSIAN -> "Ошибка скачивания"
    }

    fun clearCacheTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear cache"
        AppLanguage.RUSSIAN -> "Очистить кэш"
    }

    fun clearCacheDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Temporary browsing cache (favorites protected)"
        AppLanguage.RUSSIAN -> "Временный кэш ленты (избранное защищено)"
    }

    fun clearCacheSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cache cleared (Favorites preserved)"
        AppLanguage.RUSSIAN -> "Кэш очищен (Избранное сохранено)"
    }
}
