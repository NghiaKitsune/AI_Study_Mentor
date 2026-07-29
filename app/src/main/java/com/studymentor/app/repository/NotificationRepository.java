package com.studymentor.app.repository;

import android.content.Context;

import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.NotificationEntity;
import com.studymentor.app.data.ReminderSchedule;
import com.studymentor.app.util.ReminderScheduler;

import java.util.List;

public class NotificationRepository {
    private final StudyMentorApp app;
    private final Context context;

    public NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        app = StudyMentorApp.get();
    }

    public void load(long userId, String type, RepositoryCallback<NotificationData> callback) {
        app.executor().execute(() -> {
            try {
                List<NotificationEntity> items = app.db().notificationDao().list(userId,
                        type == null ? "" : type);
                int unread = app.db().notificationDao().unreadCount(userId);
                ReminderSchedule schedule = app.db().reminderScheduleDao().byUser(userId);
                if (schedule == null) {
                    schedule = defaultSchedule(userId);
                    app.db().reminderScheduleDao().upsert(schedule);
                }
                ReminderSchedule value = schedule;
                app.postToMain(() -> callback.onSuccess(new NotificationData(items, unread, value)));
            } catch (Exception e) {
                app.postToMain(() -> callback.onError("Unable to load notifications.", e));
            }
        });
    }

    public void markRead(long userId, long notificationId, RepositoryCallback<Void> callback) {
        app.executor().execute(() -> {
            try {
                app.db().notificationDao().markRead(notificationId, userId);
                app.postToMain(() -> callback.onSuccess(null));
            } catch (Exception e) {
                app.postToMain(() -> callback.onError("Unable to update notification.", e));
            }
        });
    }

    public void setReminder(long userId, boolean enabled, int intervalDays, int preferredHour,
                            RepositoryCallback<ReminderSchedule> callback) {
        app.executor().execute(() -> {
            try {
                ReminderSchedule schedule = new ReminderSchedule();
                schedule.userId = userId;
                schedule.enabled = enabled;
                schedule.intervalDays = Math.max(1, intervalDays);
                schedule.preferredHour = Math.max(0, Math.min(23, preferredHour));
                app.db().reminderScheduleDao().upsert(schedule);
                if (enabled) ReminderScheduler.schedule(context, userId, schedule.intervalDays, schedule.preferredHour);
                else ReminderScheduler.cancel(context, userId);
                app.postToMain(() -> callback.onSuccess(schedule));
            } catch (Exception e) {
                app.postToMain(() -> callback.onError("Reminder setting could not be saved.", e));
            }
        });
    }

    private static ReminderSchedule defaultSchedule(long userId) {
        ReminderSchedule schedule = new ReminderSchedule();
        schedule.userId = userId;
        return schedule;
    }

    public static final class NotificationData {
        public final List<NotificationEntity> items;
        public final int unread;
        public final ReminderSchedule schedule;
        NotificationData(List<NotificationEntity> items, int unread, ReminderSchedule schedule) {
            this.items = items; this.unread = unread; this.schedule = schedule;
        }
    }
}

