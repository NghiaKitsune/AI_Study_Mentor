package com.studymentor.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.util.Session;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        executor = Executors.newSingleThreadExecutor();
        // Re-apply saved theme before any Activity is created
        AppCompatDelegate.setDefaultNightMode(Session.themeMode(this));
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

    /** Single-threaded executor for background DB write operations. */
    public ExecutorService executor() {
        return executor;
    }
}
