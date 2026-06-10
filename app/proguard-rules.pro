# Standard rules
-keepattributes *Annotation*, Signature, Exception

# Retrofit / Gson DTOs
-keep class com.studymentor.app.api.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
