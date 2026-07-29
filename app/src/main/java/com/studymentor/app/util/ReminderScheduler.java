package com.studymentor.app.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.studymentor.app.StudyReminderWorker;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    public static final String INPUT_USER_ID = "user_id";
    private ReminderScheduler() {}

    public static void schedule(Context context, long userId, int intervalDays, int preferredHour) {
        if (userId <= 0) return;
        int days = Math.max(1, intervalDays);
        int hour = Math.max(0, Math.min(23, preferredHour));
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(days);
        long delayMinutes = Math.max(1, Duration.between(now, next).toMinutes());

        Data input = new Data.Builder().putLong(INPUT_USER_ID, userId).build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                StudyReminderWorker.class, days, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(input)
                .setConstraints(constraints)
                .addTag(workName(userId))
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                workName(userId), ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(Context context, long userId) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(workName(userId));
    }

    public static String workName(long userId) { return "study-reminder-user-" + userId; }
}

