# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# R8: slf4j / ktor pull in JVM-only classes that don't exist on Android.
# Suppress all the missing-class warnings they generate.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep line numbers so release crashes are debuggable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations consumed by reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod

# --- Hilt / Dagger: generated classes are discovered by reflection at runtime ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
# Hilt's aggregated module index, read from META-INF at app startup
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.internal.processedrootsentinel.codegen.** { *; }
-keep,allowobfuscation @javax.inject.Inject class * { *; }
-keep,allowobfuscation @javax.inject.Singleton class * { *; }
-keep,allowobfuscation @dagger.Module class * { *; }
-keep,allowobfuscation @dagger.Provides class * { *; }

# --- Room: DAO implementations are generated and referenced reflectively ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep,allowobfuscation @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.Dao { *; }
-keep class androidx.room.** { *; }

# --- Kotlin metadata / reflection ---
-keep class kotlin.Metadata { *; }
-keep,allowobfuscation @kotlin.Metadata class * { *; }
-keepclassmembers class * { @kotlin.Metadata *; }

# --- Compose: keep runtime internals that R8 is known to strip ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- App entry points discovered by name ---
-keep class com.vinote.NoTaApplication { *; }
-keep class com.vinote.MainActivity { *; }
-keep class com.nota.finance.** { *; }

# --- JNI / native callbacks (ML Kit OCR, camera) ---
-keepclasseswithmembernames class * { native <methods>; }

# --- Supabase / kotlinx.serialization ---
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,allowobfuscation @kotlinx.serialization.Serializable class * { *; }

# --- Enum values used by name ---
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }


