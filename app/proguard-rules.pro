# Proguard Rules for Booru App

# General
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room database and entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.booru.app.data.db.** { *; }
-dontwarn androidx.room.paging.**

# Keep data models used for JSON parsing
-keep class com.booru.app.RemoteMedia { *; }
-keep class com.booru.app.TagSuggestion { *; }
-keep class com.booru.app.SortOrder { *; }
-keep class com.booru.app.BooruCredentials { *; }
-keepclassmembers class com.booru.app.data.model.** { *; }
-keep class com.booru.app.data.model.** { *; }
-keepclassmembers class com.booru.app.data.AppUpdateInfo { *; }
-keep class com.booru.app.data.AppUpdateInfo { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Android Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
