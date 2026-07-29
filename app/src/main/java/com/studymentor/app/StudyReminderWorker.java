package com.studymentor.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.studymentor.app.data.NotificationEntity;
import com.studymentor.app.data.ReminderSchedule;
import com.studymentor.app.ui.HomeActivity;
import com.studymentor.app.util.ReminderScheduler;

public class StudyReminderWorker extends Worker {
    private static final String CHANNEL_ID = "study_reminder";

    public StudyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        long userId = getInputData().getLong(ReminderScheduler.INPUT_USER_ID, -1L);
        if (userId <= 0) return Result.failure();
        StudyMentorApp app = StudyMentorApp.get();
        ReminderSchedule schedule = app.db().reminderScheduleDao().byUser(userId);
        if (schedule == null || !schedule.enabled) return Result.success();

        Context context = getApplicationContext();
        String title = context.getString(R.string.notif_reminder_title);
        String body = context.getString(R.string.notif_reminder_body);
        NotificationEntity inbox = new NotificationEntity();
        inbox.userId = userId;
        inbox.title = title;
        inbox.body = body;
        inbox.type = NotificationEntity.TYPE_REMINDER;
        long inboxId = app.db().notificationDao().insert(inbox);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return Result.success();
        ensureChannel(context);
        Intent intent = new Intent(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context,
                (int) (userId % Integer.MAX_VALUE), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sparkles)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending);
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) (1000 + inboxId % 100000), builder.build());
        return Result.success();
    }

    private static void ensureChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notif_channel_description));
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }
}
