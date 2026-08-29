# Booru

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

A modern, fast, and elegant Android client for exploring and managing art, animations, and videos from popular booru imageboards. Built with **Jetpack Compose** and **Material 3 Expressive**.

---

## Supported Sources

- **Rule34** — The classic imageboard database.
- **Gelbooru** — Large anime and digital art community.
- **Realbooru** — Popular imageboard with extensive video & image content.
- **Xbooru** — Fast imageboard with full video and GIF support.
- **TBIB (The Big ImageBoard)** — Massive collection of over 28 million posts.
- **Yande.re** — High-resolution anime artwork and wallpapers.
- **Konachan** — High-quality wallpaper collection.
- **Safebooru** — Curated safe-for-work anime art.
- **All Sources Feed** — Unified multi-source search feed.

---

## Features

- **Video & GIF Player**: Timeline scrubber, time elapsed/total duration, ±5s skip buttons, and animated GIF playback.
- **Tag Blacklist**: Block unwanted tags in Settings to automatically exclude matching posts.
- **Smart Search & Autocomplete**: Real-time server tag suggestions with post counts and search history.
- **In-App Update Checker**: Automatic notifications for new GitHub releases with a "Don't remind again" option.
- **Filters & Modes**: "No AI" toggle, rating filters (Safe, All, 18+), and multiple sorting options (Newest, Score, Random).
- **Pinch-to-Zoom Viewer**: Smooth double-tap zoom, gesture panning, and clickable tag chips.
- **Local Favorites**: Save posts locally with instant filtering by tag.
- **Material You / Dynamic Colors**: Theme that adapts to your wallpaper with light and dark mode support.
- **Set as Wallpaper**: Instant one-click wallpaper setup for home screen, lock screen, or both screens.
- **Smart Cache**: Automatic cache cleanup on app exit to preserve storage.

---

## Built With

- **Jetpack Compose** — Modern declarative UI toolkit.
- **Material 3 Expressive** — Adaptive colors and spring physics animations.
- **Media3 ExoPlayer** — Fast and smooth video streaming.
- **Coil** — Image and GIF loading with hardware/software decoding.
- **OkHttp** — Resilient networking with provider-specific headers.
- **Jetpack DataStore** — Reactive local storage for settings, blacklist, and favorites.

---

## Getting Started

### Prerequisites
- Android 8.0 (API level 26) or higher.
- JDK 17+ or Android Studio Ladybug / Meerkat.

### Build from source
```bash
git clone https://github.com/weekanya/Booru.git
cd Booru
./gradlew assembleRelease
```
The compiled APK will be at `app/build/outputs/apk/release/app-release.apk`.
