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
    private static final String KEY_STREAK         = "streak_days";
    private static final String KEY_LAST_OPEN_DATE  = "last_open_date"; // "yyyy-MM-dd"
    private static final String KEY_BEST_QUIZ_PCT   = "best_quiz_pct"; // 0–100

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

    /**
     * Call once per app launch (e.g. SplashActivity).
     * Compares today vs last-open-date:
     *   same day   → no change
     *   yesterday  → streak + 1
     *   older      → reset to 1
     */
    public static void updateStreak(Context c) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        String last = p(c).getString(KEY_LAST_OPEN_DATE, "");

        if (last.equals(today)) return;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String yesterday = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(cal.getTime());

        int newStreak = last.equals(yesterday) ? streak(c) + 1 : 1;
        p(c).edit()
                .putInt(KEY_STREAK, newStreak)
                .putString(KEY_LAST_OPEN_DATE, today)
                .apply();
    }

    // ---- Quiz best score -------------------------------------------

    public static int bestQuizPct(Context c) { return p(c).getInt(KEY_BEST_QUIZ_PCT, 0); }

    /** Saves quiz result; only updates KEY_BEST_QUIZ_PCT when it improves. */
    public static void saveQuizResult(Context c, int score, int total) {
        if (total <= 0) return;
        int pct = score * 100 / total;
        if (pct > bestQuizPct(c)) {
            p(c).edit().putInt(KEY_BEST_QUIZ_PCT, pct).apply();
        }
    }

    // ---- Onboarding seen flag --------------------------------------

    public static boolean hasSeenOnboarding(Context c) {
        return p(c).getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    public static void markOnboardingSeen(Context c) {
        p(c).edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
    }
}
