package com.booru.app.data

enum class AppLanguage(val code: String, val displayName: String, val englishName: String) {
    ENGLISH("en", "English", "English"),
    RUSSIAN("ru", "Русский", "Russian"),
    JAPANESE("ja", "日本語", "Japanese"),
    CHINESE("zh", "中文", "Chinese"),
    KOREAN("ko", "한국어", "Korean"),
    ARABIC("ar", "العربية", "Arabic")
}

object Strings {
    fun navExplore(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Обзор"
        AppLanguage.JAPANESE -> "探索"
        AppLanguage.CHINESE -> "浏览"
        AppLanguage.KOREAN -> "탐색"
        AppLanguage.ARABIC -> "استكشاف"
        else -> "Explore"
    }

    fun navFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Избранное"
        AppLanguage.JAPANESE -> "お気に入り"
        AppLanguage.CHINESE -> "收藏"
        AppLanguage.KOREAN -> "즐겨찾기"
        AppLanguage.ARABIC -> "المفضلة"
        else -> "Favorites"
    }

    fun navSettings(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Настройки"
        AppLanguage.JAPANESE -> "設定"
        AppLanguage.CHINESE -> "设置"
        AppLanguage.KOREAN -> "설정"
        AppLanguage.ARABIC -> "الإعدادات"
        else -> "Settings"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск по тегам..."
        AppLanguage.JAPANESE -> "タグを検索..."
        AppLanguage.CHINESE -> "搜索标签..."
        AppLanguage.KOREAN -> "태그 검색..."
        AppLanguage.ARABIC -> "البحث عن الوسوم..."
        else -> "Search tags..."
    }

    fun tagSuggestions(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Подсказки тегов"
        AppLanguage.JAPANESE -> "タグの提案"
        AppLanguage.CHINESE -> "标签建议"
        AppLanguage.KOREAN -> "태그 제안"
        AppLanguage.ARABIC -> "اقتراحات الوسوم"
        else -> "Tag Suggestions"
    }

    fun recentSearches(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Недавние запросы"
        AppLanguage.JAPANESE -> "最近の検索"
        AppLanguage.CHINESE -> "最近搜索"
        AppLanguage.KOREAN -> "최근 검색"
        AppLanguage.ARABIC -> "عمليات البحث الأخيرة"
        else -> "Recent Searches"
    }

    fun clearAll(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить всё"
        AppLanguage.JAPANESE -> "すべてクリア"
        AppLanguage.CHINESE -> "清除全部"
        AppLanguage.KOREAN -> "모두 지우기"
        AppLanguage.ARABIC -> "مسح الكل"
        else -> "Clear all"
    }

    fun allPosts(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все посты"
        AppLanguage.JAPANESE -> "すべての投稿"
        AppLanguage.CHINESE -> "全部帖子"
        AppLanguage.KOREAN -> "모든 게시물"
        AppLanguage.ARABIC -> "جميع المنشورات"
        else -> "All posts"
    }

    fun fullscreen(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "На весь экран"
        AppLanguage.JAPANESE -> "全画面"
        AppLanguage.CHINESE -> "全屏"
        AppLanguage.KOREAN -> "전체 화면"
        AppLanguage.ARABIC -> "ملء الشاشة"
        else -> "Fullscreen"
    }

    fun sortNewest(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Новые"
        AppLanguage.JAPANESE -> "新しい順"
        AppLanguage.CHINESE -> "最新"
        AppLanguage.KOREAN -> "최신순"
        AppLanguage.ARABIC -> "الأحدث"
        else -> "Newest"
    }

    fun sortScore(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "По рейтингу"
        AppLanguage.JAPANESE -> "スコア順"
        AppLanguage.CHINESE -> "按评分"
        AppLanguage.KOREAN -> "점수순"
        AppLanguage.ARABIC -> "حسب التقييم"
        else -> "By Score"
    }

    fun sortRandom(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Случайно"
        AppLanguage.JAPANESE -> "ランダム"
        AppLanguage.CHINESE -> "随机"
        AppLanguage.KOREAN -> "무작위"
        AppLanguage.ARABIC -> "عشوائي"
        else -> "Random"
    }

    fun only18Badge(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Только 18+"
        AppLanguage.JAPANESE -> "18禁のみ"
        AppLanguage.CHINESE -> "仅限18+"
        AppLanguage.KOREAN -> "18+ 전용"
        AppLanguage.ARABIC -> "18+ فقط"
        else -> "18+ Only"
    }

    fun safeModeBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Безопасный"
        AppLanguage.JAPANESE -> "セーフモード"
        AppLanguage.CHINESE -> "安全模式"
        AppLanguage.KOREAN -> "안전 모드"
        AppLanguage.ARABIC -> "الوضع الآمن"
        else -> "Safe Mode"
    }

    fun allRatings(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все рейтинги"
        AppLanguage.JAPANESE -> "すべてのレーティング"
        AppLanguage.CHINESE -> "所有分级"
        AppLanguage.KOREAN -> "모든 등급"
        AppLanguage.ARABIC -> "جميع التصنيفات"
        else -> "All ratings"
    }

    fun noAiBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Без ИИ"
        AppLanguage.JAPANESE -> "AI除外"
        AppLanguage.CHINESE -> "无AI"
        AppLanguage.KOREAN -> "AI 제외"
        AppLanguage.ARABIC -> "بدون ذكاء اصطناعي"
        else -> "No AI"
    }

    fun selectSourceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите источник"
        AppLanguage.JAPANESE -> "ソースを選択"
        AppLanguage.CHINESE -> "选择图站源"
        AppLanguage.KOREAN -> "소스 선택"
        AppLanguage.ARABIC -> "اختر المصدر"
        else -> "Choose Booru Source"
    }

    fun downloadBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Скачать"
        AppLanguage.JAPANESE -> "ダウンロード"
        AppLanguage.CHINESE -> "下载"
        AppLanguage.KOREAN -> "다운로드"
        AppLanguage.ARABIC -> "تحميل"
        else -> "Download"
    }

    fun ratingSafe(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Safe (0+)"
        AppLanguage.JAPANESE -> "全年齢 (Safe)"
        AppLanguage.CHINESE -> "全年龄 (Safe)"
        AppLanguage.KOREAN -> "전연령 (Safe)"
        AppLanguage.ARABIC -> "آمن (Safe)"
        else -> "Safe"
    }

    fun ratingQuestionable(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Questionable (16+)"
        AppLanguage.JAPANESE -> "微エロ (Questionable)"
        AppLanguage.CHINESE -> "轻度敏感 (Questionable)"
        AppLanguage.KOREAN -> "주의 (Questionable)"
        AppLanguage.ARABIC -> "مشبوه (Questionable)"
        else -> "Questionable"
    }

    fun ratingExplicit(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Explicit (18+)"
        AppLanguage.JAPANESE -> "成人向け (Explicit)"
        AppLanguage.CHINESE -> "成人内容 (Explicit)"
        AppLanguage.KOREAN -> "성인용 (Explicit)"
        AppLanguage.ARABIC -> "صريح (Explicit)"
        else -> "Explicit"
    }

    fun authSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "АВТОРИЗАЦИЯ И API"
        AppLanguage.JAPANESE -> "認証とAPI"
        AppLanguage.CHINESE -> "认证与API"
        AppLanguage.KOREAN -> "인증 및 API"
        AppLanguage.ARABIC -> "المصادقة و API"
        else -> "AUTHENTICATION & API"
    }

    fun contentSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "КОНТЕНТ И ФИЛЬТРАЦИЯ"
        AppLanguage.JAPANESE -> "コンテンツとフィルター"
        AppLanguage.CHINESE -> "内容与过滤"
        AppLanguage.KOREAN -> "콘텐츠 및 필터링"
        AppLanguage.ARABIC -> "المحتوى والتصفية"
        else -> "CONTENT & FILTERING"
    }

    fun appearanceSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "ВНЕШНИЙ ВИД И ТЕМЫ"
        AppLanguage.JAPANESE -> "外観とテーマ"
        AppLanguage.CHINESE -> "外观与主题"
        AppLanguage.KOREAN -> "디자인 및 테마"
        AppLanguage.ARABIC -> "المظهر والسمات"
        else -> "APPEARANCE & THEMES"
    }

    fun languageSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "ЯЗЫК ИНТЕРФЕЙСА"
        AppLanguage.JAPANESE -> "言語"
        AppLanguage.CHINESE -> "语言"
        AppLanguage.KOREAN -> "언어"
        AppLanguage.ARABIC -> "اللغة"
        else -> "LANGUAGE"
    }

    fun languageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Язык приложения"
        AppLanguage.JAPANESE -> "アプリの言語"
        AppLanguage.CHINESE -> "应用语言"
        AppLanguage.KOREAN -> "앱 언어"
        AppLanguage.ARABIC -> "لغة التطبيق"
        else -> "App Language"
    }

    fun darkThemeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Режим темы"
        AppLanguage.JAPANESE -> "テーマモード"
        AppLanguage.CHINESE -> "主题模式"
        AppLanguage.KOREAN -> "테마 모드"
        AppLanguage.ARABIC -> "وضع المظهر"
        else -> "Theme Mode"
    }

    fun colorPaletteTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Цветовая схема"
        AppLanguage.JAPANESE -> "カラーパレット"
        AppLanguage.CHINESE -> "配色方案"
        AppLanguage.KOREAN -> "색상 팔레트"
        AppLanguage.ARABIC -> "لوحة الألوان"
        else -> "Color Palette"
    }

    fun imageQualityTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Качество"
        AppLanguage.JAPANESE -> "画質"
        AppLanguage.CHINESE -> "图片画质"
        AppLanguage.KOREAN -> "화질"
        AppLanguage.ARABIC -> "جودة الصورة"
        else -> "Quality"
    }

    fun imageQualityDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Разрешение контента при просмотре"
        AppLanguage.JAPANESE -> "メディアのデフォルト解像度"
        AppLanguage.CHINESE -> "查看媒体时的默认分辨率"
        AppLanguage.KOREAN -> "미디어 기본 해상도"
        AppLanguage.ARABIC -> "الدقة الافتراضية للوسائط"
        else -> "Default resolution for media"
    }

    fun qualityOriginal(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Оригинал (Максимальное)"
        AppLanguage.JAPANESE -> "オリジナル (最高品質)"
        AppLanguage.CHINESE -> "原图 (最高画质)"
        AppLanguage.KOREAN -> "원본 (최고 화질)"
        AppLanguage.ARABIC -> "الأصلي (أعلى جودة)"
        else -> "Original (Full Quality)"
    }

    fun qualitySample(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Оптимальное (Сэмпл)"
        AppLanguage.JAPANESE -> "標準 (サンプル)"
        AppLanguage.CHINESE -> "标准 (Sample)"
        AppLanguage.KOREAN -> "표준 (샘플)"
        AppLanguage.ARABIC -> "قياسي (عينة)"
        else -> "Optimal (Sample)"
    }

    fun qualitySaver(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Экономия трафика (Превью)"
        AppLanguage.JAPANESE -> "データ節約 (プレビュー)"
        AppLanguage.CHINESE -> "省流 (预览图)"
        AppLanguage.KOREAN -> "데이터 절약 (미리보기)"
        AppLanguage.ARABIC -> "توفير البيانات (معاينة)"
        else -> "Data Saver (Preview)"
    }

    fun customSourcesTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пользовательские Booru"
        AppLanguage.JAPANESE -> "カスタムBooru"
        AppLanguage.CHINESE -> "自定义Booru图站"
        AppLanguage.KOREAN -> "사용자 지정 Booru"
        AppLanguage.ARABIC -> "مصادر Booru مخصصة"
        else -> "Custom Booru Sources"
    }

    fun customSourcesDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Подключение сторонних Booru-сайтов"
        AppLanguage.JAPANESE -> "外部Booruサイトを追加"
        AppLanguage.CHINESE -> "添加第三方Booru站点"
        AppLanguage.KOREAN -> "외부 Booru 사이트 추가"
        AppLanguage.ARABIC -> "إضافة مواقع Booru خارجية"
        else -> "Add third-party Danbooru or Gelbooru websites"
    }

    fun addSourceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Добавить Booru источник"
        AppLanguage.JAPANESE -> "Booruを追加"
        AppLanguage.CHINESE -> "添加Booru源"
        AppLanguage.KOREAN -> "Booru 추가"
        AppLanguage.ARABIC -> "إضافة مصدر Booru"
        else -> "Add Booru Source"
    }

    fun sourceNameHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Имя источника (напр. Paheal)"
        AppLanguage.JAPANESE -> "ソース名 (例: Paheal)"
        AppLanguage.CHINESE -> "源名称 (例如 Paheal)"
        AppLanguage.KOREAN -> "소스 이름 (예: Paheal)"
        AppLanguage.ARABIC -> "اسم المصدر (مثال: Paheal)"
        else -> "Source Name (e.g. Paheal)"
    }

    fun sourceUrlHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "URL адрес (напр. https://e621.net)"
        AppLanguage.JAPANESE -> "URL (例: https://e621.net)"
        AppLanguage.CHINESE -> "网址 (例如 https://e621.net)"
        AppLanguage.KOREAN -> "웹사이트 URL (예: https://e621.net)"
        AppLanguage.ARABIC -> "رابط الموقع (مثال: https://e621.net)"
        else -> "Website URL (e.g. https://e621.net)"
    }

    fun sourceEngineLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тип API движка"
        AppLanguage.JAPANESE -> "APIエンジン"
        AppLanguage.CHINESE -> "API引擎类型"
        AppLanguage.KOREAN -> "API 엔진 유형"
        AppLanguage.ARABIC -> "نوع محرك API"
        else -> "API Engine"
    }

    fun sourceAddedSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Источник добавлен"
        AppLanguage.JAPANESE -> "ソースを追加しました"
        AppLanguage.CHINESE -> "已添加图站源"
        AppLanguage.KOREAN -> "소스가 추가되었습니다"
        AppLanguage.ARABIC -> "تمت إضافة المصدر"
        else -> "Source added"
    }

    fun editSourceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Редактировать Booru источник"
        AppLanguage.JAPANESE -> "Booruを編集"
        AppLanguage.CHINESE -> "编辑Booru源"
        AppLanguage.KOREAN -> "Booru 편집"
        AppLanguage.ARABIC -> "تعديل مصدر Booru"
        else -> "Edit Booru Source"
    }

    fun sourceUpdatedSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Источник обновлен"
        AppLanguage.JAPANESE -> "ソースを更新しました"
        AppLanguage.CHINESE -> "图站源已更新"
        AppLanguage.KOREAN -> "소스가 업데이트되었습니다"
        AppLanguage.ARABIC -> "تم تحديث المصدر"
        else -> "Source updated"
    }

    fun sourceRemovedSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Источник удален"
        AppLanguage.JAPANESE -> "ソースを削除しました"
        AppLanguage.CHINESE -> "已移除图站源"
        AppLanguage.KOREAN -> "소스가 삭제되었습니다"
        AppLanguage.ARABIC -> "تم حذف المصدر"
        else -> "Source removed"
    }

    fun deleteSourceConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить этот источник?"
        AppLanguage.JAPANESE -> "このソースを削除しますか？"
        AppLanguage.CHINESE -> "确定删除此图站源？"
        AppLanguage.KOREAN -> "이 소스를 삭제하시겠습니까?"
        AppLanguage.ARABIC -> "هل تريد حذف هذا المصدر؟"
        else -> "Remove this source?"
    }

    fun invalidHttpsUrlError(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "URL должен начинаться с https://"
        AppLanguage.JAPANESE -> "URLは https:// で始まる必要があります"
        AppLanguage.CHINESE -> "网址必须以 https:// 开头"
        AppLanguage.KOREAN -> "URL은 https:// 로 시작해야 합니다"
        AppLanguage.ARABIC -> "يجب أن يبدأ الرابط بـ https://"
        else -> "URL must start with https://"
    }

    fun emptySourceNameError(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Название не может быть пустым"
        AppLanguage.JAPANESE -> "名前を入力してください"
        AppLanguage.CHINESE -> "名称不能为空"
        AppLanguage.KOREAN -> "이름을 입력해주세요"
        AppLanguage.ARABIC -> "لا يمكن أن يكون الاسم فارغاً"
        else -> "Name cannot be empty"
    }

    fun noCustomSources(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кастомные источники пока не добавлены"
        AppLanguage.JAPANESE -> "カスタムソースはまだありません"
        AppLanguage.CHINESE -> "尚未添加自定义图站"
        AppLanguage.KOREAN -> "추가된 사용자 지정 소스가 없습니다"
        AppLanguage.ARABIC -> "لم تتم إضافة مصادر مخصصة بعد"
        else -> "No custom sources added yet"
    }

    fun dataSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "ДАННЫЕ И О ПРИЛОЖЕНИИ"
        AppLanguage.JAPANESE -> "データとアプリ情報"
        AppLanguage.CHINESE -> "数据与关于"
        AppLanguage.KOREAN -> "데이터 및 정보"
        AppLanguage.ARABIC -> "البيانات وحول التطبيق"
        else -> "DATA & ABOUT"
    }

    fun aboutAppTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "О приложении"
        AppLanguage.JAPANESE -> "Booruについて"
        AppLanguage.CHINESE -> "关于Booru"
        AppLanguage.KOREAN -> "Booru 정보"
        AppLanguage.ARABIC -> "حول Booru"
        else -> "About Booru"
    }

    fun aboutAppDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Booru • Версия 5.2"
        AppLanguage.JAPANESE -> "Booru • バージョン 5.2"
        AppLanguage.CHINESE -> "Booru • 版本 5.2"
        AppLanguage.KOREAN -> "Booru • 버전 5.2"
        AppLanguage.ARABIC -> "Booru • الإصدار 5.2"
        else -> "Booru • Version 5.2"
    }

    fun checkUpdatesTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Проверить обновления"
        AppLanguage.JAPANESE -> "アップデートを確認"
        AppLanguage.CHINESE -> "检查更新"
        AppLanguage.KOREAN -> "업데이트 확인"
        AppLanguage.ARABIC -> "التحقق من التحديثات"
        else -> "Check for Updates"
    }

    fun checkUpdatesDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажмите для проверки свежих релизов"
        AppLanguage.JAPANESE -> "GitHubで新しいリリースを確認"
        AppLanguage.CHINESE -> "点击检查GitHub最新发布"
        AppLanguage.KOREAN -> "GitHub에서 새 릴리스 확인"
        AppLanguage.ARABIC -> "انقر للتحقق من الإصدارات الجديدة على GitHub"
        else -> "Tap to check GitHub for new releases"
    }

    fun updateAvailableTitle(lang: AppLanguage, version: String) = when (lang) {
        AppLanguage.RUSSIAN -> "Доступно обновление ($version)"
        AppLanguage.JAPANESE -> "アップデートがあります ($version)"
        AppLanguage.CHINESE -> "有新版本可用 ($version)"
        AppLanguage.KOREAN -> "새 업데이트 가능 ($version)"
        AppLanguage.ARABIC -> "يتوفر تحديث ($version)"
        else -> "Update Available ($version)"
    }

    fun updateAvailableDesc(lang: AppLanguage, version: String) = when (lang) {
        AppLanguage.RUSSIAN -> "На GitHub вышла новая версия ($version). Хотите скачать обновление?"
        AppLanguage.JAPANESE -> "新しいバージョン ($version) が公開されています。ダウンロードしますか？"
        AppLanguage.CHINESE -> "GitHub上有新版本 ($version)，是否下载更新？"
        AppLanguage.KOREAN -> "새 버전 ($version)이 출시되었습니다. 다운로드하시겠습니까?"
        AppLanguage.ARABIC -> "يتوفر إصدار جديد ($version) على GitHub. هل ترغب في تنزيله؟"
        else -> "A new version ($version) is available on GitHub. Would you like to download it?"
    }

    fun updateButton(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Скачать и обновить"
        AppLanguage.JAPANESE -> "ダウンロードして更新"
        AppLanguage.CHINESE -> "下载并安装"
        AppLanguage.KOREAN -> "다운로드 및 설치"
        AppLanguage.ARABIC -> "تنزيل وتثبيت"
        else -> "Download & Install"
    }

    fun downloadingUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Загрузка обновления..."
        AppLanguage.JAPANESE -> "ダウンロード中..."
        AppLanguage.CHINESE -> "正在下载更新..."
        AppLanguage.KOREAN -> "업데이트 다운로드 중..."
        AppLanguage.ARABIC -> "جاري تنزيل التحديث..."
        else -> "Downloading update..."
    }

    fun installUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Установить"
        AppLanguage.JAPANESE -> "インストール"
        AppLanguage.CHINESE -> "安装"
        AppLanguage.KOREAN -> "설치"
        AppLanguage.ARABIC -> "تثبيت"
        else -> "Install"
    }

    fun updateDownloadFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось скачать APK обновления"
        AppLanguage.JAPANESE -> "APKのダウンロードに失敗しました"
        AppLanguage.CHINESE -> "更新包下载失败"
        AppLanguage.KOREAN -> "APK 다운로드 실패"
        AppLanguage.ARABIC -> "فشل تنزيل ملف التحديث"
        else -> "Failed to download update APK"
    }

    fun openInBrowser(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Открыть в браузере"
        AppLanguage.JAPANESE -> "ブラウザで開く"
        AppLanguage.CHINESE -> "在浏览器中打开"
        AppLanguage.KOREAN -> "브라우저에서 열기"
        AppLanguage.ARABIC -> "فتح في المتصفح"
        else -> "Open in Browser"
    }

    fun dontRemindAgain(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Больше не напоминать"
        AppLanguage.JAPANESE -> "通知しない"
        AppLanguage.CHINESE -> "不再提醒"
        AppLanguage.KOREAN -> "다시 알리지 않음"
        AppLanguage.ARABIC -> "عدم التذكير مرة أخرى"
        else -> "Don't remind again"
    }

    fun upToDateTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Установлена последняя версия"
        AppLanguage.JAPANESE -> "最新バージョンです"
        AppLanguage.CHINESE -> "已是最新版本"
        AppLanguage.KOREAN -> "최신 버전 사용 중"
        AppLanguage.ARABIC -> "أحدث إصدار مثبت"
        else -> "Latest Version Installed"
    }

    fun upToDateDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "У вас уже установлена самая актуальная версия Booru."
        AppLanguage.JAPANESE -> "現在のバージョンは最新です。"
        AppLanguage.CHINESE -> "您已安装最新版Booru。"
        AppLanguage.KOREAN -> "이미 최신 버전의 Booru를 사용하고 있습니다."
        AppLanguage.ARABIC -> "أنت تستخدم بالفعل أحدث إصدار من Booru."
        else -> "You are already using the newest version of Booru."
    }

    fun updateCheckFailedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ошибка проверки"
        AppLanguage.JAPANESE -> "確認エラー"
        AppLanguage.CHINESE -> "检查失败"
        AppLanguage.KOREAN -> "확인 실패"
        AppLanguage.ARABIC -> "فشل التحقق"
        else -> "Check Failed"
    }

    fun updateCheckFailedDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось связаться с серверами GitHub. Проверьте интернет-соединение."
        AppLanguage.JAPANESE -> "サーバーに接続できませんでした。接続を確認してください。"
        AppLanguage.CHINESE -> "无法连接至GitHub服务器，请检查网络。"
        AppLanguage.KOREAN -> "GitHub 서버에 연결할 수 없습니다. 인터넷 연결을 확인하세요."
        AppLanguage.ARABIC -> "تعذر الاتصال بخوادم GitHub. يرجى التحقق من اتصال الإنترنت."
        else -> "Could not reach GitHub servers. Please check your internet connection."
    }

    fun savedPostsCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "$count сохранённых постов"
        AppLanguage.JAPANESE -> "$count 件の保存済み投稿"
        AppLanguage.CHINESE -> "$count 个已收藏帖子"
        AppLanguage.KOREAN -> "${count}개의 저장된 게시물"
        AppLanguage.ARABIC -> "$count منشور محفوظ"
        else -> "$count saved posts"
    }

    fun favoritesEmptyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "В избранном пока пусто"
        AppLanguage.JAPANESE -> "お気に入りは空です"
        AppLanguage.CHINESE -> "收藏夹为空"
        AppLanguage.KOREAN -> "즐겨찾기가 비어 있습니다"
        AppLanguage.ARABIC -> "المفضلة فارغة"
        else -> "Favorites is empty"
    }

    fun favoritesEmptyDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажмите на иконку сердечка на карточке любого поста, чтобы сохранить его сюда насовсем."
        AppLanguage.JAPANESE -> "ハートアイコンをタップして保存できます。"
        AppLanguage.CHINESE -> "点击帖子上的心形图标将其永久保存在这里。"
        AppLanguage.KOREAN -> "게시물의 하트 아이콘을 탭하여 여기에 저장하세요."
        AppLanguage.ARABIC -> "اضغط على أيقونة القلب في أي منشور لحفظه هنا بشكل دائم."
        else -> "Tap the heart icon on any post to save it here permanently."
    }

    fun goToExplore(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Перейти к обзору"
        AppLanguage.JAPANESE -> "探索へ移動"
        AppLanguage.CHINESE -> "前往浏览"
        AppLanguage.KOREAN -> "탐색으로 이동"
        AppLanguage.ARABIC -> "الانتقال إلى الاستكشاف"
        else -> "Go to Explore"
    }

    fun filterFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фильтр по тегам в избранном..."
        AppLanguage.JAPANESE -> "お気に入りをタグで絞り込み..."
        AppLanguage.CHINESE -> "按标签筛选收藏..."
        AppLanguage.KOREAN -> "태그로 즐겨찾기 필터링..."
        AppLanguage.ARABIC -> "تصفية المفضلة حسب الوسم..."
        else -> "Filter favorites by tag..."
    }

    fun favFilterAll(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все"
        AppLanguage.JAPANESE -> "すべて"
        AppLanguage.CHINESE -> "全部"
        AppLanguage.KOREAN -> "전체"
        AppLanguage.ARABIC -> "الكل"
        else -> "All"
    }

    fun favFilterImages(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фото"
        AppLanguage.JAPANESE -> "画像"
        AppLanguage.CHINESE -> "图片"
        AppLanguage.KOREAN -> "이미지"
        AppLanguage.ARABIC -> "صور"
        else -> "Images"
    }

    fun favFilterGifs(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "GIF"
        AppLanguage.JAPANESE -> "GIF"
        AppLanguage.CHINESE -> "GIF"
        AppLanguage.KOREAN -> "GIF"
        AppLanguage.ARABIC -> "GIF"
        else -> "GIFs"
    }

    fun favFilterVideos(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Видео"
        AppLanguage.JAPANESE -> "動画"
        AppLanguage.CHINESE -> "视频"
        AppLanguage.KOREAN -> "동영상"
        AppLanguage.ARABIC -> "فيديو"
        else -> "Videos"
    }

    fun favSortTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сортировка"
        AppLanguage.JAPANESE -> "並び替え"
        AppLanguage.CHINESE -> "排序"
        AppLanguage.KOREAN -> "정렬"
        AppLanguage.ARABIC -> "ترتيب"
        else -> "Sort"
    }

    fun favSortNewest(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сначала новые"
        AppLanguage.JAPANESE -> "新しい順"
        AppLanguage.CHINESE -> "从新到旧"
        AppLanguage.KOREAN -> "최신순"
        AppLanguage.ARABIC -> "الأحدث أولاً"
        else -> "Newest first"
    }

    fun favSortOldest(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сначала старые"
        AppLanguage.JAPANESE -> "古い順"
        AppLanguage.CHINESE -> "从旧到新"
        AppLanguage.KOREAN -> "오래된순"
        AppLanguage.ARABIC -> "الأقدم أولاً"
        else -> "Oldest first"
    }

    fun favSortSource(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "По источнику"
        AppLanguage.JAPANESE -> "ソース順"
        AppLanguage.CHINESE -> "按来源"
        AppLanguage.KOREAN -> "소스별"
        AppLanguage.ARABIC -> "حسب المصدر"
        else -> "By source"
    }

    fun favSearchHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск по тегам, источнику..."
        AppLanguage.JAPANESE -> "タグやソースで検索..."
        AppLanguage.CHINESE -> "搜索标签、来源..."
        AppLanguage.KOREAN -> "태그, 소스로 검색..."
        AppLanguage.ARABIC -> "البحث حسب الوسوم والمصدر..."
        else -> "Search by tags, source..."
    }

    fun favFoundCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Найдено: $count"
        AppLanguage.JAPANESE -> "該当: $count"
        AppLanguage.CHINESE -> "找到: $count"
        AppLanguage.KOREAN -> "발견: $count"
        AppLanguage.ARABIC -> "تم العثور على: $count"
        else -> "Found: $count"
    }

    fun resetFilters(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сбросить"
        AppLanguage.JAPANESE -> "リセット"
        AppLanguage.CHINESE -> "重置"
        AppLanguage.KOREAN -> "초기화"
        AppLanguage.ARABIC -> "إعادة ضبط"
        else -> "Reset"
    }

    fun clearFavoritesConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить избранное?"
        AppLanguage.JAPANESE -> "お気に入りをすべて削除？"
        AppLanguage.CHINESE -> "清空全部收藏？"
        AppLanguage.KOREAN -> "즐겨찾기를 모두 지우시겠습니까?"
        AppLanguage.ARABIC -> "هل تريد مسح كل المفضلة؟"
        else -> "Clear all favorites?"
    }

    fun clearFavoritesDesc(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все сохранённые посты ($count) будут удалены из избранного."
        AppLanguage.JAPANESE -> "保存された $count 件の投稿が削除されます。"
        AppLanguage.CHINESE -> "将从收藏中删除所有 $count 个帖子。"
        AppLanguage.KOREAN -> "저장된 ${count}개의 게시물이 즐겨찾기에서 제거됩니다."
        AppLanguage.ARABIC -> "سيتم حذف جميع المنشورات المحفوظة البالغ عددها $count من المفضلة."
        else -> "All $count saved posts will be removed from favorites."
    }

    fun loadingOriginal(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Скачивание в полном качестве начато..."
        AppLanguage.JAPANESE -> "オリジナル画質をダウンロード中..."
        AppLanguage.CHINESE -> "正在下载原图..."
        AppLanguage.KOREAN -> "원본 화질 다운로드 중..."
        AppLanguage.ARABIC -> "جاري تنزيل الصورة الأصلية..."
        else -> "Downloading original image..."
    }

    fun copyTag(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Скопировать тег"
        AppLanguage.JAPANESE -> "タグをコピー"
        AppLanguage.CHINESE -> "复制标签"
        AppLanguage.KOREAN -> "태그 복사"
        AppLanguage.ARABIC -> "نسخ الوسم"
        else -> "Copy Tag"
    }

    fun tagCopied(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тег скопирован в буфер"
        AppLanguage.JAPANESE -> "クリップボードにコピーしました"
        AppLanguage.CHINESE -> "标签已复制到剪贴板"
        AppLanguage.KOREAN -> "태그가 클립보드에 복사되었습니다"
        AppLanguage.ARABIC -> "تم نسخ الوسم إلى الحافظة"
        else -> "Tag copied to clipboard"
    }

    fun addToBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "В чёрный список"
        AppLanguage.JAPANESE -> "ブロックリストに追加"
        AppLanguage.CHINESE -> "加入黑名单"
        AppLanguage.KOREAN -> "블랙리스트에 추가"
        AppLanguage.ARABIC -> "إضافة إلى القائمة السوداء"
        else -> "Add to Blacklist"
    }

    fun removeFromBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить из чёрного списка"
        AppLanguage.JAPANESE -> "ブロックリストから削除"
        AppLanguage.CHINESE -> "从黑名单移除"
        AppLanguage.KOREAN -> "블랙리스트에서 제거"
        AppLanguage.ARABIC -> "إزالة من القائمة السوداء"
        else -> "Remove from Blacklist"
    }

    fun tagAddedToBlacklist(tag: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "«$tag» добавлен в чёрный список"
        AppLanguage.JAPANESE -> "「$tag」をブロックリストに追加しました"
        AppLanguage.CHINESE -> "已将 \"$tag\" 加入黑名单"
        AppLanguage.KOREAN -> "\"$tag\" 태그가 블랙리스트에 추가되었습니다"
        AppLanguage.ARABIC -> "تمت إضافة \"$tag\" إلى القائمة السوداء"
        else -> "\"$tag\" added to blacklist"
    }

    fun tagRemovedFromBlacklist(tag: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "«$tag» удален из чёрного списка"
        AppLanguage.JAPANESE -> "「$tag」をブロックリストから削除しました"
        AppLanguage.CHINESE -> "已将 \"$tag\" 从黑名单移除"
        AppLanguage.KOREAN -> "\"$tag\" 태그가 블랙리스트에서 제거되었습니다"
        AppLanguage.ARABIC -> "تمت إزالة \"$tag\" من القائمة السوداء"
        else -> "\"$tag\" removed from blacklist"
    }

    fun tagBlacklistedStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "В чёрном списке"
        AppLanguage.JAPANESE -> "ブロック中"
        AppLanguage.CHINESE -> "已在黑名单"
        AppLanguage.KOREAN -> "블랙리스트 등록됨"
        AppLanguage.ARABIC -> "في القائمة السوداء"
        else -> "In blacklist"
    }

    fun saveBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранить"
        AppLanguage.JAPANESE -> "保存"
        AppLanguage.CHINESE -> "保存"
        AppLanguage.KOREAN -> "저장"
        AppLanguage.ARABIC -> "حفظ"
        else -> "Save"
    }

    fun cancelBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отмена"
        AppLanguage.JAPANESE -> "キャンセル"
        AppLanguage.CHINESE -> "取消"
        AppLanguage.KOREAN -> "취소"
        AppLanguage.ARABIC -> "إلغاء"
        else -> "Cancel"
    }

    fun clearBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить"
        AppLanguage.JAPANESE -> "クリア"
        AppLanguage.CHINESE -> "清除"
        AppLanguage.KOREAN -> "지우기"
        AppLanguage.ARABIC -> "مسح"
        else -> "Clear"
    }

    fun retryBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Повторить"
        AppLanguage.JAPANESE -> "再試行"
        AppLanguage.CHINESE -> "重试"
        AppLanguage.KOREAN -> "다시 시도"
        AppLanguage.ARABIC -> "إعادة المحاولة"
        else -> "Retry"
    }

    fun refreshBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Обновить"
        AppLanguage.JAPANESE -> "更新"
        AppLanguage.CHINESE -> "刷新"
        AppLanguage.KOREAN -> "새로고침"
        AppLanguage.ARABIC -> "تحديث"
        else -> "Refresh"
    }

    fun closeBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Закрыть"
        AppLanguage.JAPANESE -> "閉じる"
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.KOREAN -> "닫기"
        AppLanguage.ARABIC -> "إغلاق"
        else -> "Close"
    }

    fun loadingText(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Загрузка..."
        AppLanguage.JAPANESE -> "読み込み中..."
        AppLanguage.CHINESE -> "加载中..."
        AppLanguage.KOREAN -> "로딩 중..."
        AppLanguage.ARABIC -> "جاري التحميل..."
        else -> "Loading..."
    }

    fun nothingFound(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ничего не найдено"
        AppLanguage.JAPANESE -> "見つかりませんでした"
        AppLanguage.CHINESE -> "未找到内容"
        AppLanguage.KOREAN -> "결과 없음"
        AppLanguage.ARABIC -> "لم يتم العثور على شيء"
        else -> "Nothing found"
    }

    fun nothingFoundDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Попробуйте изменить поисковые теги, выбрать другой режим или выбрать другой источник."
        AppLanguage.JAPANESE -> "検索タグやソースを変更してみてください。"
        AppLanguage.CHINESE -> "请尝试更改搜索标签、模式或更换图站源。"
        AppLanguage.KOREAN -> "검색 태그, 모드를 변경하거나 다른 소스를 선택해 보세요."
        AppLanguage.ARABIC -> "جرّب تغيير وسوم البحث أو الوضع أو اختيار مصدر آخر."
        else -> "Try changing search tags, change mode, or choosing another source."
    }

    fun authErrorTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Требуется авторизация"
        AppLanguage.JAPANESE -> "認証が必要です"
        AppLanguage.CHINESE -> "需要认证"
        AppLanguage.KOREAN -> "인증 필요"
        AppLanguage.ARABIC -> "المصادقة مطلوبة"
        else -> "Authentication Required"
    }

    fun authErrorDesc(source: String, code: Int? = null, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> if (code != null) {
            "Доступ ограничен ($code). Требуется указать API Key в Настройках для $source."
        } else {
            "Для $source требуется User ID и API Key. Перейдите в Настройки для ввода ключей."
        }
        AppLanguage.JAPANESE -> if (code != null) {
            "アクセスが制限されています ($code)。設定で $source の APIキーを入力してください。"
        } else {
            "$source にはユーザーIDとAPIキーが必要です。設定を開いてキーを入力してください。"
        }
        AppLanguage.CHINESE -> if (code != null) {
            "访问受限 ($code)。请在设置中配置 $source 的 API Key。"
        } else {
            "访问 $source 需要 User ID 和 API Key。请前往设置进行配置。"
        }
        AppLanguage.KOREAN -> if (code != null) {
            "접근이 제한되었습니다 ($code). 설정에서 ${source}의 API 키를 입력하세요."
        } else {
            "${source}에는 User ID와 API Key가 필요합니다. 설정에서 키를 입력하세요."
        }
        AppLanguage.ARABIC -> if (code != null) {
            "تم تقييد الوصول ($code). يلزم إدخال مفتاح API في الإعدادات لـ $source."
        } else {
            "يتطلب $source معرف المستخدم ومفتاح API. افتح الإعدادات لإدخال المفاتيح."
        }
        else -> if (code != null) {
            "Access restricted ($code). An API Key is required in Settings for $source."
        } else {
            "Source $source requires User ID and API Key. Open Settings to configure your credentials."
        }
    }

    fun httpErrorDesc(source: String, code: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сервер $source вернул ошибку HTTP $code."
        AppLanguage.JAPANESE -> "$source から HTTP $code エラーが返されました。"
        AppLanguage.CHINESE -> "$source 服务器返回了 HTTP $code 错误。"
        AppLanguage.KOREAN -> "$source 서버에서 HTTP $code 오류가 반환되었습니다."
        AppLanguage.ARABIC -> "أرجع خادم $source الخطأ HTTP $code."
        else -> "Server returned HTTP $code from $source."
    }

    fun rateLimitErrorDesc(source: String, retryAfter: Int? = null, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> if (retryAfter != null) "Превышен лимит запросов к $source. Повторите через $retryAfter сек." else "Превышен лимит запросов к $source. Пожалуйста, подождите."
        AppLanguage.JAPANESE -> if (retryAfter != null) "$source のリクエスト制限に達しました。$retryAfter 秒後に再試行してください。" else "$source のリクエスト制限に達しました。しばらくお待ちください。"
        AppLanguage.CHINESE -> if (retryAfter != null) "$source 请求过多已被限流，请在 $retryAfter 秒后重试。" else "$source 请求过多已被限流，请稍后重试。"
        AppLanguage.KOREAN -> if (retryAfter != null) "$source 요청 한도를 초과했습니다. $retryAfter 초 후에 다시 시도하세요." else "$source 요청 한도를 초과했습니다. 잠시 후 다시 시도하세요."
        AppLanguage.ARABIC -> if (retryAfter != null) "تم تجاوز حد الطلبات لـ $source. أعد المحاولة بعد $retryAfter ثانية." else "تم تجاوز حد الطلبات لـ $source. يرجى الانتظار قليلاً."
        else -> if (retryAfter != null) "Rate limited by $source. Please retry in $retryAfter seconds." else "Rate limited by $source. Please wait a moment."
    }

    fun insecureHttpWarning(source: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Небезопасные HTTP-соединения запрещены для '$source'. Используйте HTTPS в Настройках."
        AppLanguage.JAPANESE -> "'$source' の安全でないHTTP接続は許可されていません。設定でHTTPSを使用してください。"
        AppLanguage.CHINESE -> "'$source' 不允许不安全的 HTTP 连接。请在设置中更新为 HTTPS。"
        AppLanguage.KOREAN -> "'$source'에 안전하지 않은 HTTP 연결이 허용되지 않습니다. 설정에서 HTTPS를 사용하세요."
        AppLanguage.ARABIC -> "غير مسموح باتصالات HTTP غير الآمنة لـ '$source'. يرجى استخدام HTTPS في الإعدادات."
        else -> "Insecure HTTP connections are not allowed for '$source'. Please use HTTPS in Settings."
    }

    fun timeoutErrorDesc(source: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Превышено время ожидания ответа от $source. Проверьте соединение."
        AppLanguage.JAPANESE -> "$source からの応答がタイムアウトしました。接続を確認してください。"
        AppLanguage.CHINESE -> "连接 $source 超时，请检查网络。"
        AppLanguage.KOREAN -> "$source 응답 시간이 초과되었습니다. 연결을 확인하세요."
        AppLanguage.ARABIC -> "انتهت مهلة استجابة $source. يرجى التحقق من اتصال الشبكة."
        else -> "Connection timed out for $source. Please check your network."
    }

    fun invalidCredentialsDesc(source: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Неверные учетные данные для $source. Проверьте ключи в Настройках."
        AppLanguage.JAPANESE -> "$source の認証情報が無効です。設定でキーを確認してください。"
        AppLanguage.CHINESE -> "$source 凭据无效。请在设置中检查 API Key。"
        AppLanguage.KOREAN -> "$source 인증 정보가 잘못되었습니다. 설정에서 확인하세요."
        AppLanguage.ARABIC -> "بيانات اعتماد $source غير صالحة. يرجى التحقق من المفاتيح في الإعدادات."
        else -> "Invalid credentials for $source. Please check your keys in Settings."
    }

    fun genericErrorTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ошибка"
        AppLanguage.JAPANESE -> "エラー"
        AppLanguage.CHINESE -> "错误"
        AppLanguage.KOREAN -> "오류"
        AppLanguage.ARABIC -> "خطأ"
        else -> "Error"
    }

    fun enterApiKeyBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ввести API Key"
        AppLanguage.JAPANESE -> "APIキーを入力"
        AppLanguage.CHINESE -> "输入 API Key"
        AppLanguage.KOREAN -> "API 키 입력"
        AppLanguage.ARABIC -> "إدخال مفتاح API"
        else -> "Enter API Key"
    }

    fun failedToLoad(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось загрузить данные. Проверьте интернет-соединение."
        AppLanguage.JAPANESE -> "データの読み込みに失敗しました。接続を確認してください。"
        AppLanguage.CHINESE -> "加载数据失败，请检查网络连接。"
        AppLanguage.KOREAN -> "데이터를 불러오지 못했습니다. 네트워크를 확인하세요."
        AppLanguage.ARABIC -> "فشل تحميل البيانات. يرجى التحقق من اتصال الإنترنت."
        else -> "Failed to load data. Please check your internet connection."
    }

    fun scoreLabel(score: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "$score очков"
        AppLanguage.JAPANESE -> "$score スコア"
        AppLanguage.CHINESE -> "$score 分"
        AppLanguage.KOREAN -> "$score 점"
        AppLanguage.ARABIC -> "$score نقطة"
        else -> "$score score"
    }

    fun tagsLabel(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Теги ($count)"
        AppLanguage.JAPANESE -> "タグ ($count)"
        AppLanguage.CHINESE -> "标签 ($count)"
        AppLanguage.KOREAN -> "태그 ($count)"
        AppLanguage.ARABIC -> "الوسوم ($count)"
        else -> "Tags ($count)"
    }

    fun noTags(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Теги для этого поста не найдены"
        AppLanguage.JAPANESE -> "タグがありません"
        AppLanguage.CHINESE -> "此帖子无标签"
        AppLanguage.KOREAN -> "태그 없음"
        AppLanguage.ARABIC -> "لا توجد وسوم لهذا المنشور"
        else -> "No tags found for this post"
    }

    fun infoAndTags(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Инфо и теги"
        AppLanguage.JAPANESE -> "詳細とタグ"
        AppLanguage.CHINESE -> "详情与标签"
        AppLanguage.KOREAN -> "정보 및 태그"
        AppLanguage.ARABIC -> "المعلومات والوسوم"
        else -> "Info & Tags"
    }

    fun resolution(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Разрешение"
        AppLanguage.JAPANESE -> "解像度"
        AppLanguage.CHINESE -> "分辨率"
        AppLanguage.KOREAN -> "해상도"
        AppLanguage.ARABIC -> "الدقة"
        else -> "Resolution"
    }

    fun source(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Источник"
        AppLanguage.JAPANESE -> "ソース"
        AppLanguage.CHINESE -> "来源"
        AppLanguage.KOREAN -> "소스"
        AppLanguage.ARABIC -> "المصدر"
        else -> "Source"
    }

    fun keysSavedToast(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ключи сохранены!"
        AppLanguage.JAPANESE -> "キーを保存しました！"
        AppLanguage.CHINESE -> "密钥已保存！"
        AppLanguage.KOREAN -> "키가 저장되었습니다!"
        AppLanguage.ARABIC -> "تم حفظ المفاتيح!"
        else -> "Keys saved!"
    }

    fun rule34DialogDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Укажите User ID и API Key для поиска на Rule34."
        AppLanguage.JAPANESE -> "Rule34の検索にはUser IDとAPI Keyが必要です。"
        AppLanguage.CHINESE -> "搜索 Rule34 需要提供 User ID 和 API Key。"
        AppLanguage.KOREAN -> "Rule34 검색을 위해 User ID와 API Key를 입력하세요。"
        AppLanguage.ARABIC -> "أدخل User ID و API Key للبحث في Rule34."
        else -> "Provide User ID and API Key to search Rule34."
    }

    fun gelbooruDialogDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Укажите User ID и API Key для поиска на Gelbooru."
        AppLanguage.JAPANESE -> "Gelbooruの検索にはUser IDとAPI Keyが必要です。"
        AppLanguage.CHINESE -> "搜索 Gelbooru 需要提供 User ID 和 API Key。"
        AppLanguage.KOREAN -> "Gelbooru 검색을 위해 User ID와 API Key를 입력하세요。"
        AppLanguage.ARABIC -> "أدخل User ID و API Key للبحث في Gelbooru."
        else -> "Provide User ID and API Key to search Gelbooru."
    }

    fun getKeyFromSite(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Получить ключ на сайте"
        AppLanguage.JAPANESE -> "サイトからキーを取得"
        AppLanguage.CHINESE -> "前往官网获取密钥"
        AppLanguage.KOREAN -> "사이트에서 키 가져오기"
        AppLanguage.ARABIC -> "الحصول على المفتاح من الموقع"
        else -> "Get key from website"
    }

    fun tapToEnterKeys(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажмите для ввода ключей"
        AppLanguage.JAPANESE -> "タップしてキーを入力"
        AppLanguage.CHINESE -> "点击输入密钥"
        AppLanguage.KOREAN -> "키를 입력하려면 탭하세요"
        AppLanguage.ARABIC -> "انقر لإدخال المفاتيح"
        else -> "Tap to enter User ID & API Key"
    }

    fun themeModeSystem(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Авто (Системная)"
        AppLanguage.JAPANESE -> "自動 (システム設定)"
        AppLanguage.CHINESE -> "跟随系统"
        AppLanguage.KOREAN -> "시스템 설정"
        AppLanguage.ARABIC -> "تلقائي (النظام)"
        else -> "Auto (System)"
    }

    fun themeModeDark(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тёмная тема"
        AppLanguage.JAPANESE -> "ダークテーマ"
        AppLanguage.CHINESE -> "深色主题"
        AppLanguage.KOREAN -> "다크 테マ"
        AppLanguage.ARABIC -> "الوضع الداكن"
        else -> "Dark theme"
    }

    fun themeModeLight(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Светлая тема"
        AppLanguage.JAPANESE -> "ライトテーマ"
        AppLanguage.CHINESE -> "浅色主题"
        AppLanguage.KOREAN -> "라이트 테마"
        AppLanguage.ARABIC -> "الوضع الفاتح"
        else -> "Light theme"
    }

    fun tagBlacklistTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Чёрный список тегов"
        AppLanguage.JAPANESE -> "タグブロックリスト"
        AppLanguage.CHINESE -> "标签黑名单"
        AppLanguage.KOREAN -> "태그 블랙리스트"
        AppLanguage.ARABIC -> "قائمة حظر الوسوم"
        else -> "Tag Blacklist"
    }

    fun tagBlacklistDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Скрывать посты с указанными тегами из поиска"
        AppLanguage.JAPANESE -> "指定したタグを含む投稿を非表示にします"
        AppLanguage.CHINESE -> "在搜索结果中隐藏包含特定标签的内容"
        AppLanguage.KOREAN -> "특정 태그가 포함된 게시물을 결과에서 숨깁니다"
        AppLanguage.ARABIC -> "إخفاء المنشورات التي تحتوي على وسوم محددة"
        else -> "Hide posts containing specific tags from results"
    }

    fun addTagBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Добавить"
        AppLanguage.JAPANESE -> "追加"
        AppLanguage.CHINESE -> "添加"
        AppLanguage.KOREAN -> "추가"
        AppLanguage.ARABIC -> "إضافة"
        else -> "Add"
    }

    fun addTagPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Введите тег для скрытия..."
        AppLanguage.JAPANESE -> "ブロックするタグを入力..."
        AppLanguage.CHINESE -> "输入要屏蔽的标签..."
        AppLanguage.KOREAN -> "차단할 태그 입력..."
        AppLanguage.ARABIC -> "أدخل وسماً للحظر..."
        else -> "Enter tag to block..."
    }

    fun noBlacklistedTags(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Список заблокированных тегов пуст"
        AppLanguage.JAPANESE -> "ブロック中のタグはありません"
        AppLanguage.CHINESE -> "黑名单为空"
        AppLanguage.KOREAN -> "차단된 태그가 없습니다"
        AppLanguage.ARABIC -> "لا توجد وسوم محظورة بعد"
        else -> "No blocked tags added yet"
    }

    fun clearAllBlacklist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить всё"
        AppLanguage.JAPANESE -> "すべて削除"
        AppLanguage.CHINESE -> "清空全部"
        AppLanguage.KOREAN -> "모두 삭제"
        AppLanguage.ARABIC -> "مسح الكل"
        else -> "Clear all"
    }

    fun quickPresets(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Быстрые фильтры:"
        AppLanguage.JAPANESE -> "人気のプリセット:"
        AppLanguage.CHINESE -> "常用预设:"
        AppLanguage.KOREAN -> "인기 프리셋:"
        AppLanguage.ARABIC -> "الإعدادات المسبقة الشائعة:"
        else -> "Popular presets:"
    }

    fun clearBlacklistConfirmTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить чёрный список?"
        AppLanguage.JAPANESE -> "ブロックリストを空にしますか？"
        AppLanguage.CHINESE -> "清空标签黑名单？"
        AppLanguage.KOREAN -> "블랙리스트를 초기화하시겠습니까?"
        AppLanguage.ARABIC -> "هل تريد مسح قائمة الوسوم المحظورة؟"
        else -> "Clear Tag Blacklist?"
    }

    fun clearBlacklistConfirmDesc(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все $count заблокированных тегов будут удалены."
        AppLanguage.JAPANESE -> "$count 件のブロックタグがすべて削除されます。"
        AppLanguage.CHINESE -> "将删除全部 $count 个屏蔽标签。"
        AppLanguage.KOREAN -> "차단된 ${count}개의 태그가 모두 삭제됩니다."
        AppLanguage.ARABIC -> "سيتم حذف جميع الوسوم المحظورة البالغ عددها $count."
        else -> "All $count blocked tags will be removed."
    }

    fun searchPostsWithTag(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Найти посты с этим тегом"
        AppLanguage.JAPANESE -> "このタグの投稿を検索"
        AppLanguage.CHINESE -> "搜索包含此标签的帖子"
        AppLanguage.KOREAN -> "이 태그로 게시물 검색"
        AppLanguage.ARABIC -> "البحث عن منشورات بهذا الوسم"
        else -> "Search posts with this tag"
    }

    fun searchBlacklistPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фильтр тегов..."
        AppLanguage.JAPANESE -> "タグを絞り込み..."
        AppLanguage.CHINESE -> "筛选标签..."
        AppLanguage.KOREAN -> "태그 필터링..."
        AppLanguage.ARABIC -> "تصفية الوسوم..."
        else -> "Filter tags..."
    }

    fun emptyBlacklistHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Введите теги выше, чтобы скрыть нежелательные посты"
        AppLanguage.JAPANESE -> "タグを入力して不要な投稿を非表示にできます"
        AppLanguage.CHINESE -> "在上方输入标签以隐藏不喜欢的帖子"
        AppLanguage.KOREAN -> "원하지 않는 게시물을 숨기려면 위에 태그를 입력하세요"
        AppLanguage.ARABIC -> "اكتب وسوماً أعلاه لإخفاء المنشورات غير المرغوب فيها"
        else -> "Type tags above to hide unwanted posts"
    }

    fun setWallpaperTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Установить как обои"
        AppLanguage.JAPANESE -> "壁紙に設定"
        AppLanguage.CHINESE -> "设为壁纸"
        AppLanguage.KOREAN -> "배경화면으로 설정"
        AppLanguage.ARABIC -> "تعيين كخلفية"
        else -> "Set as Wallpaper"
    }

    fun wallpaperHomeScreen(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Главный экран"
        AppLanguage.JAPANESE -> "ホーム画面"
        AppLanguage.CHINESE -> "主屏幕"
        AppLanguage.KOREAN -> "홈 화면"
        AppLanguage.ARABIC -> "الشاشة الرئيسية"
        else -> "Home Screen"
    }

    fun wallpaperLockScreen(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Экран блокировки"
        AppLanguage.JAPANESE -> "ロック画面"
        AppLanguage.CHINESE -> "锁定屏幕"
        AppLanguage.KOREAN -> "잠금 화면"
        AppLanguage.ARABIC -> "شاشة القفل"
        else -> "Lock Screen"
    }

    fun wallpaperBoth(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Оба экрана"
        AppLanguage.JAPANESE -> "両方の画面"
        AppLanguage.CHINESE -> "主屏与锁屏"
        AppLanguage.KOREAN -> "둘 다 설정"
        AppLanguage.ARABIC -> "كلا الشاشتين"
        else -> "Both Screens"
    }

    fun wallpaperSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Обои успешно установлены!"
        AppLanguage.JAPANESE -> "壁紙を設定しました！"
        AppLanguage.CHINESE -> "壁纸设置成功！"
        AppLanguage.KOREAN -> "배경화면이 설정되었습니다!"
        AppLanguage.ARABIC -> "تم تعيين الخلفية بنجاح!"
        else -> "Wallpaper set successfully!"
    }

    fun wallpaperFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось установить обои"
        AppLanguage.JAPANESE -> "壁紙の設定に失敗しました"
        AppLanguage.CHINESE -> "壁纸设置失败"
        AppLanguage.KOREAN -> "배경화면 설정 실패"
        AppLanguage.ARABIC -> "فشل تعيين الخلفية"
        else -> "Failed to set wallpaper"
    }

    fun settingWallpaper(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Установка обоев..."
        AppLanguage.JAPANESE -> "壁紙を設定中..."
        AppLanguage.CHINESE -> "正在设置壁纸..."
        AppLanguage.KOREAN -> "배경화면 설정 중..."
        AppLanguage.ARABIC -> "جاري تعيين الخلفية..."
        else -> "Setting wallpaper..."
    }

    fun downloadSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранено в галерею (Booru)"
        AppLanguage.JAPANESE -> "ギャラリーに保存しました (Booru)"
        AppLanguage.CHINESE -> "已保存至相册 (Booru)"
        AppLanguage.KOREAN -> "갤러리에 저장되었습니다 (Booru)"
        AppLanguage.ARABIC -> "تم الحفظ في المعرض (Booru)"
        else -> "Saved to Gallery (Booru)"
    }

    fun downloadFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ошибка скачивания"
        AppLanguage.JAPANESE -> "ダウンロード失敗"
        AppLanguage.CHINESE -> "下载失败"
        AppLanguage.KOREAN -> "다운로드 실패"
        AppLanguage.ARABIC -> "فشل التنزيل"
        else -> "Download failed"
    }

    fun clearCacheTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить кэш"
        AppLanguage.JAPANESE -> "キャッシュを削除"
        AppLanguage.CHINESE -> "清除缓存"
        AppLanguage.KOREAN -> "캐시 삭제"
        AppLanguage.ARABIC -> "مسح التخزين المؤقت"
        else -> "Clear cache"
    }

    fun clearCacheDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Временный кэш ленты (избранное защищено)"
        AppLanguage.JAPANESE -> "一時的なブラウジングキャッシュ（お気に入りは保護されます）"
        AppLanguage.CHINESE -> "清除浏览临时缓存（收藏内容不受影响）"
        AppLanguage.KOREAN -> "임시 탐색 캐시 (즐겨찾기는 안전하게 유지됨)"
        AppLanguage.ARABIC -> "ذاكرة التخزين المؤقت للتصفح (المفضلة محمية)"
        else -> "Temporary browsing cache (favorites protected)"
    }

    fun favoritesStorageDesc(lang: AppLanguage, size: String) = when (lang) {
        AppLanguage.RUSSIAN -> "Хранилище избранного: $size"
        AppLanguage.JAPANESE -> "お気に入りの容量: $size"
        AppLanguage.CHINESE -> "收藏夹占用空间: $size"
        AppLanguage.KOREAN -> "즐겨찾기 저장공간: $size"
        AppLanguage.ARABIC -> "مساحة المفضلة: $size"
        else -> "Favorites storage: $size"
    }

    fun clearCacheSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кэш очищен (Избранное сохранено)"
        AppLanguage.JAPANESE -> "キャッシュを削除しました"
        AppLanguage.CHINESE -> "缓存已清除（收藏已保留）"
        AppLanguage.KOREAN -> "캐시가 삭제되었습니다 (즐겨찾기 보존됨)"
        AppLanguage.ARABIC -> "تم مسح الذاكرة المؤقتة (تم الحفاظ على المفضلة)"
        else -> "Cache cleared (Favorites preserved)"
    }

    fun share(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поделиться"
        AppLanguage.JAPANESE -> "共有"
        AppLanguage.CHINESE -> "分享"
        AppLanguage.KOREAN -> "공유"
        AppLanguage.ARABIC -> "مشاركة"
        else -> "Share"
    }

    fun tags(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Теги"
        AppLanguage.JAPANESE -> "タグ"
        AppLanguage.CHINESE -> "标签"
        AppLanguage.KOREAN -> "태그"
        AppLanguage.ARABIC -> "وسوم"
        else -> "Tags"
    }

    fun downloadComplete(filename: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранено: $filename"
        AppLanguage.JAPANESE -> "保存完了: $filename"
        AppLanguage.CHINESE -> "已保存: $filename"
        AppLanguage.KOREAN -> "저장 완료: $filename"
        AppLanguage.ARABIC -> "تم الحفظ: $filename"
        else -> "Saved: $filename"
    }

    fun errorLoading(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось загрузить"
        AppLanguage.JAPANESE -> "読み込みに失敗しました"
        AppLanguage.CHINESE -> "加载失败"
        AppLanguage.KOREAN -> "불러오기 실패"
        AppLanguage.ARABIC -> "فشل التحميل"
        else -> "Failed to load image"
    }

    fun pressBackAgainToExit(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажмите ещё раз для выхода"
        AppLanguage.JAPANESE -> "もう一度戻るを押して終了"
        AppLanguage.CHINESE -> "再按一次退出应用"
        AppLanguage.KOREAN -> "뒤로 가기를 한 번 더 누르면 종료됩니다"
        AppLanguage.ARABIC -> "اضغط رجوع مرة أخرى للخروج"
        else -> "Press back again to exit"
    }

    fun recommendationsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Для вас"
        AppLanguage.JAPANESE -> "あなたへのおすすめ"
        AppLanguage.CHINESE -> "为你推荐"
        AppLanguage.KOREAN -> "추천"
        AppLanguage.ARABIC -> "لك"
        else -> "For you"
    }

    fun sourceRecommendations(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Рекомендации"
        AppLanguage.JAPANESE -> "おすすめ"
        AppLanguage.CHINESE -> "智能推荐"
        AppLanguage.KOREAN -> "추천"
        AppLanguage.ARABIC -> "التوصيات"
        else -> "Recommendations"
    }

    fun sourceRecommendationsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Умная лента под ваши интересы"
        AppLanguage.JAPANESE -> "好みに合わせた動的フィード"
        AppLanguage.CHINESE -> "根据兴趣个性化推荐的动态流"
        AppLanguage.KOREAN -> "관심사에 맞춘 맞춤형 피드"
        AppLanguage.ARABIC -> "خلاصة ديناميكية مخصصة لاهتماماتك"
        else -> "Dynamic feed tailored to your interests"
    }

    fun clearRecommendationsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Стереть память рекомендаций"
        AppLanguage.JAPANESE -> "おすすめ履歴を消去"
        AppLanguage.CHINESE -> "重置推荐记忆"
        AppLanguage.KOREAN -> "추천 기록 초기화"
        AppLanguage.ARABIC -> "مسح ذاكرة التوصيات"
        else -> "Clear recommendation memory"
    }

    fun clearRecommendationsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сбросить историю предпочтений и память тегов"
        AppLanguage.JAPANESE -> "スマートタグの関心と検索の好みをリセット"
        AppLanguage.CHINESE -> "重置标签偏好与搜索历史记忆"
        AppLanguage.KOREAN -> "스마트 태그 관심사 및 검색 설정 초기화"
        AppLanguage.ARABIC -> "إعادة ضبط اهتمامات الوسوم الذكية وتفضيلات البحث"
        else -> "Reset smart tag interests and search preferences"
    }

    fun clearRecommendationsSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Память рекомендаций очищена"
        AppLanguage.JAPANESE -> "おすすめ履歴を消去しました"
        AppLanguage.CHINESE -> "推荐偏好已重置"
        AppLanguage.KOREAN -> "추천 기록이 초기화되었습니다"
        AppLanguage.ARABIC -> "تم مسح ذاكرة التوصيات"
        else -> "Recommendation memory cleared"
    }

    fun contentTypeTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тип контента"
        AppLanguage.JAPANESE -> "コンテンツタイプ"
        AppLanguage.CHINESE -> "内容类型"
        AppLanguage.KOREAN -> "콘텐츠 유형"
        AppLanguage.ARABIC -> "نوع المحتوى"
        else -> "Content type"
    }

    fun contentTypeAll(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все типы"
        AppLanguage.JAPANESE -> "すべて"
        AppLanguage.CHINESE -> "全部类型"
        AppLanguage.KOREAN -> "모든 유형"
        AppLanguage.ARABIC -> "جميع الأنواع"
        else -> "All types"
    }

    fun contentTypePhotos(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фото"
        AppLanguage.JAPANESE -> "画像"
        AppLanguage.CHINESE -> "图片"
        AppLanguage.KOREAN -> "사진"
        AppLanguage.ARABIC -> "صور"
        else -> "Photos"
    }

    fun contentTypeVideos(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Видео"
        AppLanguage.JAPANESE -> "動画"
        AppLanguage.CHINESE -> "视频"
        AppLanguage.KOREAN -> "동영상"
        AppLanguage.ARABIC -> "فيديو"
        else -> "Videos"
    }

    fun contentTypeGifs(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "GIF"
        AppLanguage.JAPANESE -> "GIF"
        AppLanguage.CHINESE -> "GIF"
        AppLanguage.KOREAN -> "GIF"
        AppLanguage.ARABIC -> "GIF"
        else -> "GIFs"
    }

    fun filtersAndSorting(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фильтры и сортировка"
        AppLanguage.JAPANESE -> "フィルターと並び替え"
        AppLanguage.CHINESE -> "筛选与排序"
        AppLanguage.KOREAN -> "필터 및 정렬"
        AppLanguage.ARABIC -> "التصفية والترتيب"
        else -> "Filters & Sorting"
    }

    fun filtersButton(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Фильтры"
        AppLanguage.JAPANESE -> "フィルター"
        AppLanguage.CHINESE -> "筛选"
        AppLanguage.KOREAN -> "필터"
        AppLanguage.ARABIC -> "تصفية"
        else -> "Filters"
    }

    fun applyFilters(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Применить"
        AppLanguage.JAPANESE -> "適用"
        AppLanguage.CHINESE -> "应用"
        AppLanguage.KOREAN -> "적용"
        AppLanguage.ARABIC -> "تطبيق"
        else -> "Apply"
    }

    fun comicMode(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Режим комикса"
        AppLanguage.JAPANESE -> "コミックスクロール"
        AppLanguage.CHINESE -> "条漫滚动模式"
        AppLanguage.KOREAN -> "웹툰 스크롤"
        AppLanguage.ARABIC -> "تمرير القصص المصورة"
        else -> "Comic scroll"
    }

    fun fitMode(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Вписать"
        AppLanguage.JAPANESE -> "全体表示"
        AppLanguage.CHINESE -> "全图适应"
        AppLanguage.KOREAN -> "화면 맞춤"
        AppLanguage.ARABIC -> "ملاءمة"
        else -> "Fit"
    }

    fun chooseLanguageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите язык"
        AppLanguage.JAPANESE -> "言語を選択"
        AppLanguage.CHINESE -> "选择语言"
        AppLanguage.KOREAN -> "언어 선택"
        AppLanguage.ARABIC -> "اختر اللغة"
        else -> "Choose Language"
    }

    fun confirmDeleteAction(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить?"
        AppLanguage.JAPANESE -> "削除しますか？"
        AppLanguage.CHINESE -> "确认删除？"
        AppLanguage.KOREAN -> "삭제하시겠습니까?"
        AppLanguage.ARABIC -> "تأكيد الحذف؟"
        else -> "Delete?"
    }

    fun sourceNoVideosNotice(source: String, lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "В источнике $source нет видео (только изображения и GIF)"
        AppLanguage.JAPANESE -> "$source には動画がありません（画像とGIFのみ）"
        AppLanguage.CHINESE -> "$source 没有视频（仅图片和GIF）"
        AppLanguage.KOREAN -> "$source 에는 동영상이 없습니다 (이미지 및 GIF만 지원)"
        AppLanguage.ARABIC -> "المصدر $source لا يحتوي على مقاطع فيديو (صور وGIF فقط)"
        else -> "Source $source has no videos (images & GIFs only)"
    }
}
