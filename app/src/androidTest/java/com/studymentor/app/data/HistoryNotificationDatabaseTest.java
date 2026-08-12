package com.studymentor.app.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional/integration evidence for UR5, UR6 and the local notification store in UR8.
 */
@RunWith(AndroidJUnit4.class)
public class HistoryNotificationDatabaseTest {
    private AppDatabase db;
    private long ownerId;
    private long otherId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        ownerId = db.userDao().insert(user("owner@local.invalid", "Owner"));
        otherId = db.userDao().insert(user("other@local.invalid", "Other"));
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
    }

    @Test
    public void historySearchSupportsKeywordSubjectAndBookmarkFilters() {
        Question science = completedQuestion(ownerId, "Explain Newton laws", "science", true, 1000L);
        Question math = completedQuestion(ownerId, "Solve quadratic equation", "math", false, 2000L);
        Question otherUser = completedQuestion(otherId, "Explain Newton laws", "science", true, 3000L);
        db.questionDao().insert(science);
        db.questionDao().insert(math);
        db.questionDao().insert(otherUser);

        List<Question> keyword = db.questionDao().search(ownerId, "Newton", "", false);
        List<Question> scienceOnly = db.questionDao().search(ownerId, "", "science", false);
        List<Question> bookmarked = db.questionDao().search(ownerId, "", "", true);

        assertEquals(1, keyword.size());
        assertEquals("Explain Newton laws", keyword.get(0).prompt);
        assertEquals(1, scienceOnly.size());
        assertEquals(1, bookmarked.size());
        assertTrue(bookmarked.get(0).bookmarked);
        assertEquals(2, db.questionDao().count(ownerId));
        assertEquals(1, db.questionDao().bookmarkedCount(ownerId));
    }

    @Test
    public void deletingQuestionCascadesMessagesAndAnswerDetails() {
        long questionId = db.questionDao().insert(
                completedQuestion(ownerId, "What is Room?", "code", false, 1000L));
        db.messageDao().insert(Message.user(questionId, "What is Room?"));
        db.messageDao().insert(Message.assistant(questionId, "A local persistence library."));

        AnswerDetail detail = new AnswerDetail();
        detail.questionId = questionId;
        detail.stepsJson = "[{\"title\":\"Persist\",\"body\":\"Use Room\"}]";
        db.answerDetailDao().upsert(detail);

        assertEquals(2, db.messageDao().forQuestion(questionId, ownerId).size());
        assertNotNull(db.answerDetailDao().byQuestion(questionId, ownerId));

        db.questionDao().delete(db.questionDao().byId(questionId, ownerId));

        assertTrue(db.messageDao().forQuestion(questionId, ownerId).isEmpty());
        assertNull(db.answerDetailDao().byQuestion(questionId, ownerId));
    }

    @Test
    public void notificationReadStateCannotBeChangedByAnotherUser() {
        NotificationEntity notification = notification(ownerId, NotificationEntity.TYPE_REMINDER,
                "Study reminder", "Review your saved questions", 1000L);
        long notificationId = db.notificationDao().insert(notification);

        assertEquals(1, db.notificationDao().unreadCount(ownerId));
        assertEquals(0, db.notificationDao().markRead(notificationId, otherId));
        assertEquals(1, db.notificationDao().unreadCount(ownerId));

        assertEquals(1, db.notificationDao().markRead(notificationId, ownerId));
        assertEquals(0, db.notificationDao().unreadCount(ownerId));
    }

    @Test
    public void notificationTypeFilterAndReminderSchedulePersistPerUser() {
        db.notificationDao().insert(notification(ownerId, NotificationEntity.TYPE_REMINDER,
                "Reminder", "Study now", 2000L));
        db.notificationDao().insert(notification(ownerId, NotificationEntity.TYPE_PROGRESS,
                "Progress", "You gained XP", 3000L));
        db.notificationDao().insert(notification(otherId, NotificationEntity.TYPE_REMINDER,
                "Other", "Other user's reminder", 4000L));

        assertEquals(2, db.notificationDao().list(ownerId, "").size());
        assertEquals(1, db.notificationDao().list(ownerId, NotificationEntity.TYPE_REMINDER).size());
        assertEquals(1, db.notificationDao().list(ownerId, NotificationEntity.TYPE_PROGRESS).size());

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.userId = ownerId;
        schedule.enabled = true;
        schedule.intervalDays = 3;
        schedule.preferredHour = 20;
        db.reminderScheduleDao().upsert(schedule);

        ReminderSchedule persisted = db.reminderScheduleDao().byUser(ownerId);
        assertNotNull(persisted);
        assertTrue(persisted.enabled);
        assertEquals(3, persisted.intervalDays);
        assertEquals(20, persisted.preferredHour);
        assertNull(db.reminderScheduleDao().byUser(otherId));
    }

    private static User user(String email, String displayName) {
        User user = new User();
        user.email = email;
        user.displayName = displayName;
        user.passwordHash = "test-hash";
        user.passwordSalt = "test-salt";
        return user;
    }

    private static Question completedQuestion(long userId, String prompt, String subject,
                                               boolean bookmarked, long createdAt) {
        Question question = new Question();
        question.userId = userId;
        question.prompt = prompt;
        question.normalizedPrompt = prompt.toLowerCase();
        question.subject = subject;
        question.answer = "Answer for " + prompt;
        question.status = Question.STATUS_COMPLETED;
        question.bookmarked = bookmarked;
        question.createdAt = createdAt;
        question.updatedAt = createdAt;
        return question;
    }

    private static NotificationEntity notification(long userId, String type, String title,
                                                     String body, long createdAt) {
        NotificationEntity value = new NotificationEntity();
        value.userId = userId;
        value.type = type;
        value.title = title;
        value.body = body;
        value.createdAt = createdAt;
        return value;
    }
}
