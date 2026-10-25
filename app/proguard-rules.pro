# ProGuard & R8 Configuration for LinkUp Messenger

# Keep Line numbers and source files for readable stacktraces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data Models & JSON Serialization (Moshi, Kotlinx, JSONObject)
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.insforge.** { *; }
-keepclassmembers class com.example.data.insforge.** { *; }

# BuildConfig
-keep class com.example.BuildConfig { *; }

# Jetpack Compose
-dontwarn androidx.compose.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Coil Image Loader
-dontwarn coil.**
-keep class coil.** { *; }

# AndroidX Security & Crypto (EncryptedSharedPreferences)
-dontwarn androidx.security.crypto.**
-keep class androidx.security.crypto.** { *; }

# AndroidX Credentials & Google Identity
-dontwarn androidx.credentials.**
-keep class androidx.credentials.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Firebase components
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
