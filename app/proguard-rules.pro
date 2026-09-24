# Retrofit / Gson / OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.salman.stoktakip.data.remote.dto.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class androidx.room.** { *; }

# Genel
-keepattributes SourceFile,LineNumberTable
