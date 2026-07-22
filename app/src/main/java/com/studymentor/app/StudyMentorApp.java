package com.studymentor.app;

import android.app.Activity;
import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.util.Session;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
                .fallbackToDestructiveMigration()
                .build();
        // Enable StrictMode AFTER one-time init (SharedPrefs first-access triggers disk check)
        if (com.studymentor.app.BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .penaltyLog()
                    .build());
        }
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

    /**
     * Runs a DB read on the background executor and delivers the result to the UI thread.
     * Silently drops the result if the host Activity is already finishing or destroyed.
     */
    public static <T> void query(Activity host, Callable<T> work, Consumer<T> onUi) {
        get().executor().execute(() -> {
            final T result;
            try {
                result = work.call();
            } catch (Exception e) {
                Log.e("Db", "query failed", e);
                return;
            }
            host.runOnUiThread(() -> {
                if (!host.isFinishing() && !host.isDestroyed()) onUi.accept(result);
            });
        });
    }
}
