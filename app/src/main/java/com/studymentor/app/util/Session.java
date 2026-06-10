package com.studymentor.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Thin wrapper around {@link SharedPreferences} for the bits of user
 * state the UI needs without standing up a full auth subsystem.
 *
 * For an MVP this is enough. Replace with a real token + secure storage
 * (e.g. EncryptedSharedPreferences) before shipping.
 */
public final class Session {

    private static final String KEY_AUTH_TOKEN  = "auth_token";
    private static final String KEY_USER_EMAIL  = "user_email";
    private static final String KEY_USER_NAME   = "user_name";
    private static final String KEY_ONBOARDED   = "onboarded";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String KEY_USER_LEVEL  = "user_level";
    private static final String KEY_SUBJECTS    = "subjects";          // CSV
    private static final String KEY_THEME_MODE  = "theme_mode";        // AppCompatDelegate.MODE_NIGHT_*
    private static final String KEY_LANGUAGE    = "language";          // "en" | "vi"
    private static final String KEY_NOTIFS      = "notifications_on";
    private static final String KEY_STREAK      = "streak_days";

    private Session() {}

    private static SharedPreferences p(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
    }

    // ---- Auth ------------------------------------------------------

    public static boolean isLoggedIn(Context c) {
        return p(c).getString(KEY_AUTH_TOKEN, null) != null;
    }

    public static void saveAuth(Context c, String token, String email) {
        String displayName = email == null ? "Friend" : email.split("@")[0];
        p(c).edit()
                .putString(KEY_AUTH_TOKEN, token)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, displayName)
                .apply();
    }

    public static String email(Context c) { return p(c).getString(KEY_USER_EMAIL, ""); }
    public static String name(Context c)  { return p(c).getString(KEY_USER_NAME, "Friend"); }

    /** Wipes everything (auth + onboarding + prefs). Used by Sign Out. */
    public static void clear(Context c) {
        p(c).edit().clear().apply();
    }

    // ---- Personalization (UC1) -------------------------------------

    public static boolean isOnboarded(Context c) {
        return p(c).getBoolean(KEY_ONBOARDED, false);
    }

    public static void savePersonalization(Context c, String level, String subjectsCsv) {
        p(c).edit()
                .putBoolean(KEY_ONBOARDED, true)
                .putString(KEY_USER_LEVEL, level)
                .putString(KEY_SUBJECTS, subjectsCsv)
                .apply();
    }

    public static String level(Context c)    { return p(c).getString(KEY_USER_LEVEL, "high-school"); }
    public static String subjects(Context c) { return p(c).getString(KEY_SUBJECTS, "math,science"); }

    // ---- Preferences (UC10) ----------------------------------------

    /**
     * Returns one of {@code AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM},
     * {@code MODE_NIGHT_NO}, or {@code MODE_NIGHT_YES}.
     */
    public static int themeMode(Context c) {
        return p(c).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context c, int mode) {
        p(c).edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public static String language(Context c) { return p(c).getString(KEY_LANGUAGE, "en"); }
    public static void setLanguage(Context c, String v) { p(c).edit().putString(KEY_LANGUAGE, v).apply(); }

    public static boolean notificationsOn(Context c) { return p(c).getBoolean(KEY_NOTIFS, true); }
    public static void setNotificationsOn(Context c, boolean v) { p(c).edit().putBoolean(KEY_NOTIFS, v).apply(); }

    // ---- Streak ----------------------------------------------------

    public static int streak(Context c) { return p(c).getInt(KEY_STREAK, 0); }
    public static void setStreak(Context c, int days) { p(c).edit().putInt(KEY_STREAK, days).apply(); }

    // ---- Onboarding seen flag --------------------------------------

    public static boolean hasSeenOnboarding(Context c) {
        return p(c).getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    public static void markOnboardingSeen(Context c) {
        p(c).edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
    }
}
