# Booru

An Android client for searching, browsing, and managing media from booru imageboards. Built with Kotlin, Jetpack Compose, and Material 3 Expressive.

## Supported Sources

### Built-in Providers
- Rule34 (DAPI)
- Gelbooru (DAPI)
- Realbooru (DAPI)
- Xbooru (DAPI)
- The Big ImageBoard / TBIB (DAPI)
- Yande.re (Moebooru)
- Konachan (Moebooru)
- Safebooru (DAPI)
- All Sources (Aggregated multi-provider feed)

### Custom Booru Providers
Users can add and manage custom booru instances using one of the following engines:
- Gelbooru / DAPI (`index.php`)
- Moebooru (`post.json`)
- Danbooru / e621 (`posts.json`)

All custom provider endpoints require HTTPS. Insecure HTTP connections are rejected.

## Supported Media Types

- Images (JPEG, PNG, WebP)
- Animated GIFs
- Videos (MP4, WebM)

## Core Functionality

### Media Viewer and Playback
- Fullscreen media viewer with pinch-to-zoom, pan, and double-tap zoom gestures.
- Video playback powered by AndroidX Media3 ExoPlayer with progress scrubbing, play/pause controls, time display, and 5-second seek buttons.
- Detail sheet displaying dimensions, score, rating, creation date, source, and clickable tag chips.

### Search and Filtering
- Multi-token tag search with server-driven autocomplete suggestions.
- Search history management.
- Content filters for rating (Safe, All, Questionable/Explicit) and AI-generated content exclusion.
- Feed sorting modes: Newest (timestamp-sorted with score tie-breakers), Highest Score, and Random.

### Tag Blacklist
- Token-based blacklist matching that operates on exact tag tokens to prevent substring false positives.
- Supports namespace tags (such as `character:xxx`, `artist:xxx`).
- Case-insensitive matching applied across all search results.

### Favorites
- Persistent favorite post storage backed by an SQLite database via Android Jetpack Room.
- Dedicated offline media cache stored in app-private storage (`filesDir/favorites_media`), isolated from browsing cache.
- Filter favorites by media type (Images, GIFs, Videos) and search favorites by tag or source.

### Cache Management
- Image caching managed by Coil with 25% RAM memory cache and 200 MB LRU disk cache limits.
- Video caching handled by Media3 `SimpleCache` with a 100 MB LRU eviction limit.
- Manual cache clearing from Settings clears browsing caches without deleting saved favorites.

### Media Downloads
- Direct downloads to public device storage (`Pictures/Booru` and `Movies/Booru`).
- MediaStore integration using `IS_PENDING` on Android 10+ with automatic cleanup of partial files upon failure.
- Registered with `MediaScannerConnection` for immediate gallery indexation.

### Security and Credentials
- API keys and user credentials for Rule34, Gelbooru, and custom sources are stored in `EncryptedSharedPreferences` backed by the Android Keystore.
- No plaintext credential fallback is permitted.
- Safe, idempotent migration transfers legacy DataStore credentials to encrypted storage without data loss.
- Sensitive authentication parameters (`api_key`, `user_id`, `password`, `token`, `secret`, `login`) are redacted from application logs.

### Application Updater
- Release checks against GitHub repository API over HTTPS.
- Download URLs are restricted to `github.com` and `objects.githubusercontent.com`.
- Unsafe redirects to third-party domains or unencrypted HTTP are rejected.
- Downloaded APK packages are validated for package name and signing certificate match against the running application prior to installation.
- Corrupted or partial APK files are deleted immediately on failure.

## Technical Architecture

- Language: Kotlin
- UI Framework: Jetpack Compose, Material 3 Expressive
- Local Database: Room (SQLite)
- Local Preferences: Jetpack DataStore Preferences
- Credential Security: AndroidX Security Crypto (EncryptedSharedPreferences)
- Image Pipeline: Coil
- Media Player: AndroidX Media3 ExoPlayer
- Networking: OkHttp 4
- Serialization: org.json

## Building

### Requirements
- Android 8.0 (API level 26) or higher
- JDK 17
- Android SDK with platform tools (compileSdk 35)

### Commands
Debug build:
```bash
./gradlew :app:assembleDebug
```

Release build:
```bash
./gradlew :app:assembleRelease
```

## License

Booru is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.
