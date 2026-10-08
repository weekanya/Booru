<p align="center">
  <img src="docs/assets/banner.svg" alt="Booru: private, fast booru client for Android" width="100%">
</p>

<p align="center">
  <a href="https://github.com/weekanya/Booru/releases/latest"><img src="https://img.shields.io/github/v/release/weekanya/Booru?style=flat-square&color=7B5BEA&label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-12%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 12+">
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.4">
  <img src="https://img.shields.io/badge/Material%203-Expressive-6750A4?style=flat-square&logo=materialdesign&logoColor=white" alt="Material 3 Expressive">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square" alt="GPL-3.0"></a>
</p>

<p align="center">
  <a href="https://github.com/weekanya/Booru/releases/latest"><b>Download</b></a>
  &nbsp;·&nbsp;
  <a href="#features">Features</a>
  &nbsp;·&nbsp;
  <a href="#sources">Sources</a>
  &nbsp;·&nbsp;
  <a href="#building">Building</a>
</p>

Booru is a native Android client for booru imageboards. It merges eight sources into one feed and learns your taste on the device. It plays video with parallel prefetching and keeps favorites available offline. It is built entirely with Jetpack Compose and Material 3 Expressive. There are no accounts, no analytics and no backend: the app talks directly to the boorus.

## Screenshots

<!-- Put PNG files into docs/screenshots/ using the names below (portrait, ~1080x2400 works best). -->

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/explore.png" width="200" alt="Explore"><br><sub>Explore</sub></td>
    <td align="center"><img src="docs/screenshots/details.png" width="200" alt="Post details"><br><sub>Post details</sub></td>
    <td align="center"><img src="docs/screenshots/viewer.png" width="200" alt="Viewer"><br><sub>Viewer</sub></td>
    <td align="center"><img src="docs/screenshots/favorites.png" width="200" alt="Favorites"><br><sub>Favorites</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/search.png" width="200" alt="Search"><br><sub>Search</sub></td>
    <td align="center"><img src="docs/screenshots/filters.png" width="200" alt="Filters"><br><sub>Filters</sub></td>
    <td align="center"><img src="docs/screenshots/sources.png" width="200" alt="Sources"><br><sub>Sources</sub></td>
    <td align="center"><img src="docs/screenshots/settings.png" width="200" alt="Settings"><br><sub>Settings</sub></td>
  </tr>
</table>

## Features

### Feed and recommendations
- **"For you" feed.** One aggregated feed that takes posts in turn from every enabled source, each source sorted by date or score.
- **On-device recommendations.** The engine learns from your favorites. Tags that appear on most of them are down-weighted so one dominant tag cannot take over the feed. Each tag is paged separately, so results keep changing.
- **Feed mix.** Choose Newest, Balanced or For you, or set an exact ratio with a slider.
- **Source switches.** Each source can be turned off. Rule34 and Gelbooru are left out of aggregated feeds until their API keys are set, so the feed never stalls on auth errors.
- **Incognito mode.** Pauses search history and recommendation learning.

### Search and filtering
- **Search.** Search for several tags at once, with server autocomplete. Suggestions show the tag category (artist, character, copyright, general, meta) and the post count.
- **History.** Search history with one-tap re-run, insert-into-query and per-entry removal.
- **Filters.** Filter by content type (photos, videos, GIFs), by rating (all, 18+ only, safe) and hide AI-generated posts.
- **Sorting.** Newest, top score or random.
- **Tag blacklist.** Exact token matching, applied to every feed and source.

### Viewer and playback
- **Detail sheet.** Swipe between posts, pinch or double-tap to zoom, see score and resolution. Long-press a tag to search, copy or blacklist it.
- **Fullscreen viewer.** Immersive mode with swipe-to-dismiss and zoom-aware gestures.
- **Video player** (Media3 ExoPlayer):
  - Downloads in parallel chunks, four connections with 1 MB ranges, into a 512 MB LRU cache.
  - Seeking into an already downloaded range starts immediately and snaps to the nearest keyframe.
  - Playback speed from 0.5x to 2x.
- **Image quality.** Original, Sample or Data saver.
- **Post actions.** Download originals, share, set as home or lock screen wallpaper, open the post in a browser.

### Favorites
- **Storage.** Saved to a Room database. Media files go to separate offline storage that survives cache clears.
- **Folders.** Custom folders with per-folder counters, and a swipeable folder pager.
- **Search.** Search inside favorites by tag, exclude tags with `-tag`, filter by media type and sort by date.

### Design
- **Material 3 Expressive.** Expressive motion springs, connected button groups, shape morphing, loading indicators and segmented lists.
- **Eight palettes.** Includes Monet dynamic color and the monochrome Graphite. System, light and dark modes, plus Midnight AMOLED.
- **Adaptive layout.** Uses a navigation rail on tablets and foldables (600 dp and wider) and adapts the number of grid columns.
- **Languages.** English, Russian, Japanese, Chinese, Korean and Arabic.

### Privacy and security
- **No tracking.** No analytics, telemetry or accounts. Every request goes straight to the booru.
- **Encrypted credentials.** API keys are stored in `EncryptedSharedPreferences`, backed by the Android Keystore.
- **App lock.** Biometric lock with a configurable timeout.
- **Screen protection.** `FLAG_SECURE` hides content from the recents screen and screen capture.
- **Cache cleanup.** The browsing cache is cleared on every start.
- **Updates.** The in-app updater pulls from GitHub Releases. It installs an update only if the APK is signed with the same certificate as the installed app.

## Sources

| Source | API | Notes |
| --- | --- | --- |
| Rule34 | Gelbooru DAPI | API key required (Settings) |
| Gelbooru | Gelbooru DAPI | API key required (Settings) |
| Realbooru | HTML parser | Video-heavy |
| Xbooru | Gelbooru DAPI | |
| TBIB | Gelbooru DAPI | |
| Yande.re | Moebooru | |
| Konachan | Moebooru | |
| Safebooru | Gelbooru DAPI | Safe content only |
| Custom | Gelbooru, Moebooru, Danbooru | Any HTTPS instance |

## Tech stack

| Area | Library |
| --- | --- |
| Language | Kotlin 2.4, coroutines |
| UI | Jetpack Compose (BOM 2026.09), Material 3 1.5 Expressive |
| Media | Media3 ExoPlayer with OkHttp data source and `SimpleCache` |
| Images | Coil 2 (GIF and video frame decoders) |
| Networking | OkHttp 4, Jsoup |
| Storage | Room, DataStore, Security Crypto |
| Build | AGP 9.4 with built-in Kotlin, KSP, Gradle 9.7, R8 with resource shrinking |

## Requirements

- **Device:** Android 12 (API 31) or newer.
- **Build:** JDK 21 and Android SDK Platform 37 (`compileSdk 37`, `targetSdk 37`).

## Building

```bash
git clone https://github.com/weekanya/Booru.git
cd Booru

./gradlew assembleDebug      # app/build/outputs/apk/debug
./gradlew assembleRelease    # app/build/outputs/apk/release
```

Release builds need a signing key. Set it with environment variables, Gradle properties or a `signing.properties` file in the project root:

```properties
BOORU_KEYSTORE_PATH=/path/to/release.jks
BOORU_KEYSTORE_PASSWORD=...
BOORU_KEY_ALIAS=...
BOORU_KEY_PASSWORD=...
```

### Continuous integration

| Workflow | Trigger | Output |
| --- | --- | --- |
| `Release APK (R8)` | Push to `test`, manual | Signed release APK and `mapping.txt` |
| `Debug APK` | Manual | Debug APK |

Both workflows install Android Platform 37 through the shared `.github/actions/android-setup` action.

The release workflow signs with the repository secrets `BOORU_KEYSTORE_BASE64`, `BOORU_KEYSTORE_PASSWORD`, `BOORU_KEY_ALIAS` and `BOORU_KEY_PASSWORD`. If they are missing, it falls back to a generated CI key that is cached between runs. Android will not install an update signed with one key over an app signed with the other.

## Project structure

```
app/src/main/java/com/booru/app
├── BooruApp.kt            Application, image loader, video cache and parallel prefetch
├── BooruRepository.kt     Source APIs, aggregation, interleaving
├── GalleryViewModel.kt    Feed, search, recommendations, favorites state
├── MainActivity.kt        Navigation, app lock, window setup
├── data/                  Preferences, Room, network, parsers, blacklist, AI filter, updater
└── ui/                    Compose screens, theme, motion, adaptive layout, shared components
```

## License

Released under the [GPL-3.0](LICENSE) license.
