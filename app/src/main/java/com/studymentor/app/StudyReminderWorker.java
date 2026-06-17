package com.studymentor.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class StudyReminderWorker extends Worker {

    private static final String CHANNEL_ID  = "study_reminder";
    private static final int    NOTIF_ID    = 1001;

    public StudyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        ensureChannel(ctx);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sparkles)
                .setContentTitle(ctx.getString(R.string.notif_reminder_title))
                .setContentText(ctx.getString(R.string.notif_reminder_body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, builder.build());
        return Result.success();
    }

    private static void ensureChannel(Context ctx) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Daily reminder to keep your study streak going");
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }
}
