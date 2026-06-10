package com.studymentor.app;

import android.app.Application;

import androidx.room.Room;

import com.studymentor.app.data.AppDatabase;

/**
 * Custom Application. Holds the singleton Room database. UI grabs it via
 * the static {@link #get()} accessor — kept no-arg on purpose so calls
 * stay short at the call sites.
 *
 * Declared in {@code AndroidManifest.xml} as {@code android:name=".StudyMentorApp"}.
 */
public class StudyMentorApp extends Application {

    private static StudyMentorApp instance;

    private AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
                // MVP: destructive migrations are fine. Replace with proper
                // Migrations once real users are on it.
                .fallbackToDestructiveMigration()
                // Note: queries below run on the main thread for terseness in
                // the MVP. Move to Executors before shipping.
                .allowMainThreadQueries()
                .build();
    }

    public static StudyMentorApp get() {
        return instance;
    }

    public AppDatabase db() {
        return db;
    }
}
