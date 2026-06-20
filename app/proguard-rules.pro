# Standard attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit: keep library classes so generic signatures in method Signature attrs are readable
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Retrofit: conditional rule — if an interface has @retrofit2.http.* methods, keep it with sigs
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep app API interfaces and DTOs (already covers AiService, ChatRequest, ChatResponse)
-keep class com.studymentor.app.api.** { *; }
-keep interface com.studymentor.app.api.** { *; }

# Room: database, entities, DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.studymentor.app.data.** { *; }
-dontwarn androidx.room.paging.**

# Gson: keep POJO field names so serialization/deserialization works
-keepclassmembers class com.studymentor.app.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.studymentor.app.data.QuizQuestion { *; }

# Gson TypeToken generic resolution
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-dontwarn javax.annotation.**
-dontwarn kotlin.**
