# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# =============================================
# PERFORMANCE OPTIMIZATIONS
# =============================================

# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove Timber debug logs in release
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# =============================================
# KEEP RULES FOR APP FUNCTIONALITY
# =============================================

# Keep accessibility service
-keep class com.selfcontrol.deviceowner.AccessibilityMonitor { *; }

# Keep device admin receiver
-keep class com.selfcontrol.deviceowner.DeviceOwnerReceiver { *; }

# Keep VPN service
-keep class com.selfcontrol.deviceowner.UrlFilterVpnService { *; }

# Keep Hilt generated classes
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep ALL device owner classes
-keep class com.selfcontrol.deviceowner.** { *; }

# Keep ALL presentation classes (Activities, ViewModels, etc.)
-keep class com.selfcontrol.presentation.** { *; }

# Keep Enforcement Activity specifically
-keep class com.selfcontrol.presentation.enforcement.EnforcementActivity { *; }
-keep class com.selfcontrol.presentation.enforcement.EnforcementViewModel { *; }
-keep class com.selfcontrol.presentation.enforcement.EnforcementState { *; }

# =============================================
# DATA MODELS - CRITICAL FOR API CALLS
# =============================================

# Keep ALL domain models (used by Retrofit/Room)
-keep class com.selfcontrol.domain.model.** { *; }

# Keep ALL data entities (Room)
-keep class com.selfcontrol.data.local.entity.** { *; }

# Keep ALL DTOs (Retrofit)
-keep class com.selfcontrol.data.remote.dto.** { *; }

# Keep ALL mappers (prevent stripping)
-keep class com.selfcontrol.data.mapper.** { *; }

# Keep ALL repositories (implementations)
-keep class com.selfcontrol.data.repository.** { *; }

# Keep ALL domain repositories (interfaces)
-keep class com.selfcontrol.domain.repository.** { *; }

# Keep ALL use cases
-keep class com.selfcontrol.domain.usecase.** { *; }

# =============================================
# RETROFIT / NETWORK
# =============================================

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep API interface and all methods
-keep interface com.selfcontrol.data.remote.api.SelfControlApi { *; }
-keep class com.selfcontrol.data.remote.api.** { *; }

# Retrofit does reflection on generic parameters
-keepattributes Signature
-keep class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent stripping of model class members
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# =============================================
# ROOM DATABASE
# =============================================

# Room entities and DAOs
-keep class com.selfcontrol.data.local.entity.** { *; }
-keep class com.selfcontrol.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# =============================================
# FIREBASE / CRASHLYTICS
# =============================================

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =============================================
# WORKMANAGER
# =============================================

# Workers
-keep class com.selfcontrol.data.worker.** { *; }

# =============================================
# COROUTINES
# =============================================

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# =============================================
# SEALED CLASSES (Result, etc.)
# =============================================

-keep class com.selfcontrol.domain.util.Result { *; }
-keep class com.selfcontrol.domain.util.Result$* { *; }