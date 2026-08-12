package com.studymentor.app.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Reliability and offline-state evidence for UR3 and the PendingQuestionWorker flow.
 */
@RunWith(AndroidJUnit4.class)
public class QuestionStateMachineTest {
    private AppDatabase db;
    private long ownerId;
    private long otherId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        ownerId = db.userDao().insert(user("owner@local.invalid"));
        otherId = db.userDao().insert(user("other@local.invalid"));
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
    }

    @Test
    public void failedQuestionCanBeClaimedAgainAndCompleted() {
        Question question = pendingQuestion(ownerId, "Explain photosynthesis");
        long questionId = db.questionDao().insert(question);
        long now = 100_000L;

        assertEquals(1, db.questionDao().claimForProcessing(
                questionId, ownerId, now - 60_000L, now));
        assertEquals(1, db.questionDao().failProcessing(
                questionId, ownerId, Question.STATUS_FAILED, "NETWORK", "Temporary failure", now + 1L));
        assertEquals(Question.STATUS_FAILED, db.questionDao().byId(questionId, ownerId).status);

        assertEquals(1, db.questionDao().claimForProcessing(
                questionId, ownerId, now - 60_000L, now + 2L));
        assertEquals(1, db.questionDao().complete(
                questionId, ownerId, "Plants convert light into chemical energy.", false, now + 3L));

        Question completed = db.questionDao().byId(questionId, ownerId);
        assertEquals(Question.STATUS_COMPLETED, completed.status);
        assertEquals("Plants convert light into chemical energy.", completed.answer);
        assertNull(completed.errorCode);
    }

    @Test
    public void staleProcessingQuestionIsRecoveredToPending() {
        Question question = pendingQuestion(ownerId, "Explain gravity");
        question.status = Question.STATUS_PROCESSING;
        question.updatedAt = 1_000L;
        long questionId = db.questionDao().insert(question);

        int recovered = db.questionDao().recoverStaleProcessing(ownerId, 5_000L, 10_000L);

        assertEquals(1, recovered);
        Question value = db.questionDao().byId(questionId, ownerId);
        assertEquals(Question.STATUS_PENDING, value.status);
        assertEquals("RECOVERED", value.errorCode);
        assertEquals(10_000L, value.updatedAt);
    }

    @Test
    public void completedCacheIsScopedByUserAndLearningContext() {
        Question cached = pendingQuestion(ownerId, "Explain Room");
        cached.normalizedPrompt = "explain room";
        cached.subject = "code";
        cached.educationLevel = "university";
        cached.explanationStyle = "detailed";
        cached.language = "en";
        cached.status = Question.STATUS_COMPLETED;
        cached.answer = "Room is an Android persistence library.";
        cached.updatedAt = 5000L;
        db.questionDao().insert(cached);

        Question exact = db.questionDao().findCache(ownerId, "explain room", "code",
                "university", "detailed", "en");
        Question wrongUser = db.questionDao().findCache(otherId, "explain room", "code",
                "university", "detailed", "en");
        Question wrongLanguage = db.questionDao().findCache(ownerId, "explain room", "code",
                "university", "detailed", "vi");

        assertNotNull(exact);
        assertEquals("Room is an Android persistence library.", exact.answer);
        assertNull(wrongUser);
        assertNull(wrongLanguage);
    }

    @Test
    public void anotherUserCannotBookmarkOrChangeQuestionStatus() {
        long questionId = db.questionDao().insert(pendingQuestion(ownerId, "Explain inheritance"));

        assertEquals(0, db.questionDao().setBookmarked(questionId, otherId, true, 2000L));
        assertEquals(0, db.questionDao().updateStatus(
                questionId, otherId, Question.STATUS_FAILED, "X", "foreign update", 2001L));

        Question ownerQuestion = db.questionDao().byId(questionId, ownerId);
        assertFalse(ownerQuestion.bookmarked);
        assertEquals(Question.STATUS_PENDING, ownerQuestion.status);
        assertNull(db.questionDao().byId(questionId, otherId));
    }

    private static User user(String email) {
        User user = new User();
        user.email = email;
        user.displayName = "Test User";
        user.passwordHash = "test-hash";
        user.passwordSalt = "test-salt";
        return user;
    }

    private static Question pendingQuestion(long userId, String prompt) {
        Question question = new Question();
        question.userId = userId;
        question.prompt = prompt;
        question.normalizedPrompt = prompt.toLowerCase();
        question.subject = "general";
        question.status = Question.STATUS_PENDING;
        return question;
    }
}
