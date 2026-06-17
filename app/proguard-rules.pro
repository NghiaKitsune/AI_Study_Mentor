# Standard rules
-keepattributes *Annotation*, Signature, Exception

# Retrofit interfaces and Gson DTOs
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

# Retrofit: keep generic Response type parameters
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.**

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
