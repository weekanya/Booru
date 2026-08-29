-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.booru.app.data.db.** { *; }
-dontwarn androidx.room.paging.**

-keep class com.booru.app.RemoteMedia { *; }
-keep class com.booru.app.TagSuggestion { *; }
-keep class com.booru.app.SortOrder { *; }
-keep class com.booru.app.BooruCredentials { *; }
-keepclassmembers class com.booru.app.data.model.** { *; }
-keep class com.booru.app.data.model.** { *; }
-keepclassmembers class com.booru.app.data.AppUpdateInfo { *; }
-keep class com.booru.app.data.AppUpdateInfo { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-keep class coil.** { *; }
-dontwarn coil.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
