package com.studymentor.app.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.studymentor.app.util.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Security-focused database/session checks for UR1/UR2 and the local-first MVP.
 * Production source is intentionally not mocked or modified.
 */
@RunWith(AndroidJUnit4.class)
public class AuthenticationSecurityTest {
    private Context context;
    private AppDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Session.clear(context);
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        Session.clear(context);
        if (db != null) db.close();
    }

    @Test
    public void exactDuplicateEmailIsRejectedByUniqueDatabaseConstraint() {
        User first = user("student@local.invalid", "Student");
        db.userDao().insert(first);

        boolean rejected = false;
        try {
            db.userDao().insert(user("student@local.invalid", "Duplicate"));
        } catch (RuntimeException expected) {
            rejected = true;
        }

        assertTrue("The unique email constraint must reject an exact duplicate.", rejected);
        assertEquals("Student", db.userDao().byEmail("student@local.invalid").displayName);
    }

    @Test
    public void localSessionStartAndLogoutClearOnlyActiveAuthenticationState() {
        assertFalse(Session.isLoggedIn(context));

        Session.start(context, 42L, true);
        assertTrue(Session.isLoggedIn(context));
        assertEquals(42L, Session.userId(context));
        assertTrue(Session.isOnboarded(context));

        Session.clearAuth(context);
        assertFalse(Session.isLoggedIn(context));
        assertEquals(-1L, Session.userId(context));
    }

    @Test
    public void deletingUserCascadesOwnedQuestionNotificationAndReminderData() {
        long userId = db.userDao().insert(user("owner@local.invalid", "Owner"));

        Question question = completedQuestion(userId, "Explain Room", "code");
        long questionId = db.questionDao().insert(question);

        NotificationEntity notification = new NotificationEntity();
        notification.userId = userId;
        notification.title = "Study reminder";
        notification.body = "Time to review";
        long notificationId = db.notificationDao().insert(notification);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.userId = userId;
        schedule.enabled = true;
        schedule.intervalDays = 1;
        schedule.preferredHour = 19;
        db.reminderScheduleDao().upsert(schedule);

        db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM users WHERE id = ?", new Object[]{userId});

        assertNull(db.questionDao().byId(questionId, userId));
        assertTrue(db.notificationDao().list(userId, "").isEmpty());
        assertNull(db.reminderScheduleDao().byUser(userId));
        assertEquals(0, db.notificationDao().markRead(notificationId, userId));
    }

    private static User user(String email, String displayName) {
        User user = new User();
        user.email = email;
        user.displayName = displayName;
        user.passwordHash = "test-hash";
        user.passwordSalt = "test-salt";
        return user;
    }

    private static Question completedQuestion(long userId, String prompt, String subject) {
        Question question = new Question();
        question.userId = userId;
        question.prompt = prompt;
        question.normalizedPrompt = prompt.toLowerCase();
        question.subject = subject;
        question.answer = "Saved answer";
        question.status = Question.STATUS_COMPLETED;
        return question;
    }
}
