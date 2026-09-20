# Booru 5.2 Release Notes

## Overview
Booru version 5.2 (Build 11) introduces key quality-of-life improvements, removes the irrelevant score indicator for Realbooru, overhauls the tag blacklist with smooth physics-based animations, fixes text clipping in tag filtering, and enforces system-wide stability and security.

## What's New & Changed

### Realbooru Integration Polish
- Removed the score and star rating card in the detailed media view when browsing Realbooru, as Realbooru does not have a post scoring or upvoting system.
- When score is absent, the resolution metadata card expands seamlessly to full width without awkward gaps.
- Feed thumbnails for Realbooru omit score badges.

### Tag Blacklist Animations & Polish
- Added entrance animation for new tags: adding a tag smoothly scales and fades it into the flow layout with a spring bounce.
- Softened all transition curves: tag removals, list resizing, and empty state crossfades now use low-stiffness springs for natural fluid motion.
- Tag count indicator now slides vertically up or down with fade transitions when items are added or removed.
- Add tag button features animated color transitions and tactile spring bounce.
- Filter search bar expands and shrinks vertically with smooth spring easing.

### Blacklist Tag Filter Fixes
- Fixed vertical text clipping: replaced the squeezed outlined text field with a centered basic text field layout, ensuring all typed characters remain fully visible without sinking into borders.
- Removed the border outline from the tag filter field for a clean, frameless appearance.

### Security, Stability & System
- Updated target SDK to 36 (Android 16 compatibility).
- Permanent FLAG_SECURE window protection against screen recording and system recents previews.
- All floating dialogs replaced with bottom sheets featuring pure white drag handles.
- Asynchronous safeguards and timeouts to prevent UI hangs during downloads and wallpaper operations.
- Automatic temporary cache purge on application startup and exit.

## File Information
- Version: 5.2
- Version Code: 11
- Target SDK: 36 (Android 16)
- Min SDK: 26 (Android 8.0)
- Package Name: com.booru.app
- APK Output Path: app/build/outputs/apk/release/app-release.apk
