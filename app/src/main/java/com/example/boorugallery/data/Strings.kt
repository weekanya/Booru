package com.example.boorugallery.data

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

    fun postsCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "$count posts"
        AppLanguage.RUSSIAN -> "$count постов"
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

    fun safeModeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Safe Mode"
        AppLanguage.RUSSIAN -> "Безопасный режим (Safe)"
    }

    fun safeModeDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Hide explicit adult content (18+)"
        AppLanguage.RUSSIAN -> "Скрывать откровенный контент (18+)"
    }

    fun excludeSafeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Exclude Safe Content"
        AppLanguage.RUSSIAN -> "Исключить Safe-контент"
    }

    fun excludeSafeDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Show only NSFW (18+ / Explicit & Questionable)"
        AppLanguage.RUSSIAN -> "Показывать только NSFW (18+ / Explicit)"
    }

    fun only18Badge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "18+ Only"
        AppLanguage.RUSSIAN -> "Только 18+"
    }

    fun safeModeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Safe Mode"
        AppLanguage.RUSSIAN -> "Безопасный"
    }

    fun noAiTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Filter AI Content"
        AppLanguage.RUSSIAN -> "Фильтр ИИ-артов"
    }

    fun noAiDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Exclude AI-generated images"
        AppLanguage.RUSSIAN -> "Исключить арты, созданные нейросетями"
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

    fun shareBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.RUSSIAN -> "Поделиться"
    }

    fun openBrowserBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Open in Browser"
        AppLanguage.RUSSIAN -> "В браузере"
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

    fun colorPaletteDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Choose dynamic Monet or curated preset themes"
        AppLanguage.RUSSIAN -> "Динамический Monet или готовая цветовая тема"
    }

    fun monetTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Dynamic Color (Monet)"
        AppLanguage.RUSSIAN -> "Динамические цвета (Monet)"
    }

    fun monetDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Adaptive palette from your wallpaper"
        AppLanguage.RUSSIAN -> "Цветовая палитра из обоев Android"
    }

    fun dataSection(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "DATA & ABOUT"
        AppLanguage.RUSSIAN -> "ДАННЫЕ И О ПРИЛОЖЕНИИ"
    }

    fun searchHistoryTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search History"
        AppLanguage.RUSSIAN -> "История поиска"
    }

    fun searchHistoryEmpty(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "History is empty"
        AppLanguage.RUSSIAN -> "История пуста"
    }

    fun searchHistoryCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "$count saved queries"
        AppLanguage.RUSSIAN -> "$count сохранённых запросов"
    }

    fun aboutAppTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "About Booru"
        AppLanguage.RUSSIAN -> "О приложении"
    }

    fun aboutAppDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Booru • Version 2.0"
        AppLanguage.RUSSIAN -> "Booru • Версия 2.0"
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

    fun copyTags(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.RUSSIAN -> "Копировать"
    }

    fun tagsCopied(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tags copied to clipboard"
        AppLanguage.RUSSIAN -> "Теги скопированы в буфер обмена"
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

    fun closeBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.RUSSIAN -> "Закрыть"
    }

    fun loadingText(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Loading high quality images..."
        AppLanguage.RUSSIAN -> "Загрузка изображений высокого качества..."
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

    fun openSettingsBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Open Settings"
        AppLanguage.RUSSIAN -> "Открыть Настройки"
    }

    fun timeoutError(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Connection timed out. Please try again."
        AppLanguage.RUSSIAN -> "Превышено время ожидания. Попробуйте еще раз."
    }

    fun networkError(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No internet connection or server unreachable."
        AppLanguage.RUSSIAN -> "Нет подключения к интернету или сервер недоступен."
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

    fun historyClearedToast(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "History cleared"
        AppLanguage.RUSSIAN -> "История очищена"
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
}
