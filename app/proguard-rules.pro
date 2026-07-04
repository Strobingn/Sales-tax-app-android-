# TaxCalc Pro - ProGuard/R8 rules for release builds
# Keeps everything needed so minify + shrinkResources doesn't nuke your app at runtime

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Jetpack Compose (non-negotiable or your UI dies)
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material.** { *; }

# Room + KSP generated code
-keep @androidx.room.* class *
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
    public static *** getInstance(...);
    public static void *** (android.content.Context);
}

# Lifecycle / ViewModel / Compose ViewModel
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Navigation Compose
-keep class androidx.navigation.** { *; }
-keepclassmembers class ** {
    public static *** navigate(...);
}

# DataStore
-keep class androidx.datastore.** { *; }
-keepclassmembers class * implements androidx.datastore.preferences.core.Preferences {
    <fields>;
}

# Material Components (your themes.xml dependency)
-keep class com.google.android.material.** { *; }

# Your entire app package — safe for a small calculator, guarantees zero runtime breakage
-keep class com.strobingn.taxcalc.** { *; }

# Optional but recommended for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
