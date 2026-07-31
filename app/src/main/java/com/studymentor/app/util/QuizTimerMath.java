package com.studymentor.app.util;

import java.util.Locale;

/** Pure deadline arithmetic shared by the Practice timer and local unit tests. */
public final class QuizTimerMath {
    private QuizTimerMath() {}

    public static long remainingMillis(long deadlineEpochMs, long nowEpochMs) {
        if (deadlineEpochMs <= 0L) return 0L;
        return Math.max(0L, deadlineEpochMs - nowEpochMs);
    }

    public static String formatRemaining(long remainingMillis) {
        long totalSeconds = (Math.max(0L, remainingMillis) + 999L) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
