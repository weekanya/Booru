# Booru

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

A modern, fast, and elegant Android client for exploring and managing art, animations, and videos from popular booru imageboards. Built with **Jetpack Compose** and **Material Design 3 (Material You / Expressive)**.

---

##  Supported Sources

- **Rule34** — The classic imageboard database.
- **Gelbooru** — Large anime and digital art community.
- **Xbooru** — Fast imageboard with full support for video and GIF animations.
- **TBIB (The Big ImageBoard)** — Massive collection of over 28 million posts.
- **Yande.re** — High-resolution anime artwork and wallpapers.
- **Konachan** — High-quality wallpaper collection.
- **Safebooru** — Curated safe-for-work anime art.
- **All Sources Feed** — Unified search across multiple providers at the same time.

---

##  Features

- **Video & GIF Playback**: Integrated video player with instant poster thumbnails, volume toggle, progress scrubber, and full-resolution animated GIF playback.
- **Smart Search & Autocomplete**: Real-time tag suggestions with post counters as you type, recent search history, and trending popular tags.
- **"No AI" Filter**: Single-tap filter on the main screen to hide AI-generated content across all sources.
- **Content Modes**: Convenient toggle between Safe mode, All posts, and 18+ only content.
- **Pinch-to-Zoom Viewer**: Smooth double-tap zoom, gesture panning, and interactive tag chips that let you search new tags with one tap.
- **Favorites**: Save your favorite artworks locally with instant access and filtering.
- **Material You / Dynamic Colors**: Adapts its palette to your Android wallpaper with support for light and dark themes.
- **Smart Cache**: Automatically clears temporary image cache when closing the app to save storage space.

---

##  Built With

- **Jetpack Compose** — Modern declarative UI toolkit.
- **Material 3 Expressive** — Adaptive dynamic colors, smooth spring animations, and bottom sheets.
- **Media3 ExoPlayer** — Fast and smooth video streaming.
- **Coil** — Image and GIF loading with hardware/software decoding.
- **OkHttp** — Resilient HTTP networking with source-specific referer and user-agent handling.
- **Jetpack DataStore** — Modern reactive local storage for settings and favorites.

---

##  Getting Started

### Prerequisites
- Android 8.0 (API level 26) or higher.
- Android Studio Ladybug / Meerkat or JDK 11+.

### Build from source
```bash
git clone https://github.com/weekanya/booru-gallery.git
cd booru-gallery
./gradlew assembleRelease
```
The output APK will be located at `app/build/outputs/apk/release/app-release.apk`.
