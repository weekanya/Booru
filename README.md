# Booru

Modern, private Android client for booru imageboards. Built with Kotlin, Jetpack Compose, and Material 3 Expressive.

## Features

### Sources and Aggregation
- **Built-in Providers**: Rule34, Gelbooru, Realbooru, Xbooru, TBIB, Yande.re, Konachan, Safebooru.
- **Custom Booru**: Add custom instances via Gelbooru (`index.php`), Moebooru (`post.json`), or Danbooru (`posts.json`) engines (HTTPS only).
- **Smart Recommendations**: Multi-provider aggregated feed with local tag learning and history reset.

### Browsing and Viewing
- **Immersive Viewer**: Smooth fullscreen viewer with pinch-to-zoom, pan gestures, and swipe-to-dismiss.
- **Video Player**: Media3 ExoPlayer with seek controls, audio toggle, and background buffering.
- **Quality Selector**: Configurable media resolution: Original (uncompressed), Sample (balanced), or Data Saver (previews).
- **Post Details**: Metadata sheet with tags, score, rating, dimensions, and source links.
- **Smart Filter Accumulation**: Multi-page pagination when filtering by media types (Images, GIFs, Videos) with tag injection.

### Search and Blacklist
- **Tag Search**: Support for searching multiple tags simultaneously (space-separated), with server autocomplete and local search history.
- **Filters**: Rating (Safe, Questionable, Explicit), AI content filter, and sorting (Newest, Score, Random).
- **Tag Blacklist**: Exact token blacklist matching to hide unwanted content across all feeds.

### Favorites and Storage
- **Favorites**: Persistent SQLite/Room storage with dedicated offline media storage isolated from temporary caches.
- **Type Filtering**: Filter favorites by All, Images, GIFs, and Videos with animated segmented controls.
- **Automatic Cache Management**: Temporary browsing cache (images, videos, temporary updates) automatically clears on startup and exit.

### Localization
- **Multi-language Support**: English, Russian, Japanese, Chinese, Korean, and Arabic with dedicated selection interface.

### Privacy and Security
- **Encrypted Storage**: API keys and credentials secured via Android Keystore (`EncryptedSharedPreferences`).
- **No Analytics / Telemetry**: Completely private, direct client-to-booru connections.
- **In-App Updater**: GitHub Releases updater with signature verification and markdown release notes.

## Building

### Prerequisites
- Android Studio or Gradle CLI
- JDK 21
- Android SDK (API 26 – 36)

### Commands
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

## License

GPL-3.0 License. See [LICENSE](LICENSE) for details.
