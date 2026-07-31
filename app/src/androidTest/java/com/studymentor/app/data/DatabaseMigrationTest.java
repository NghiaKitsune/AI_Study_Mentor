package com.studymentor.app.data;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
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

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {
    private static final String DB_NAME = "migration-test.db";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void migrationOneToTwoPreservesQuestionMessageAndEnforcesOwnership() {
        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(DB_NAME)
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE `questions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `prompt` TEXT NOT NULL, `answer` TEXT, `subject` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `bookmarked` INTEGER NOT NULL)");
                                db.execSQL("CREATE TABLE `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `question_id` INTEGER NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `sent_at` INTEGER NOT NULL)");
                            }

                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                                // The Room migration is executed when AppDatabase opens this version-one file.
                            }
                        })
                        .build();
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(configuration);
        SupportSQLiteDatabase oldDb = helper.getWritableDatabase();
        oldDb.execSQL("INSERT INTO questions(id,prompt,answer,subject,created_at,bookmarked) VALUES(7,'What is Room?','A persistence library.','code',1000,1)");
        oldDb.execSQL("INSERT INTO messages(id,question_id,role,text,sent_at) VALUES(9,7,'user','What is Room?',1000)");
        helper.close();

        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                .build();
        try {
            Question migrated = db.questionDao().byId(7, User.MIGRATED_USER_ID);
            assertNotNull(migrated);
            assertEquals(Question.STATUS_COMPLETED, migrated.status);
            assertTrue(migrated.bookmarked);
            assertEquals(1, db.messageDao().forQuestion(7, User.MIGRATED_USER_ID).size());

            User user = new User();
            user.email = "second@local.invalid";
            user.displayName = "Second";
            user.passwordHash = "hash";
            user.passwordSalt = "salt";
            long secondUserId = db.userDao().insert(user);
            db.questionDao().reassignOwner(User.MIGRATED_USER_ID, secondUserId);
            assertNull(db.questionDao().byId(7, User.MIGRATED_USER_ID));
            assertNotNull(db.questionDao().byId(7, secondUserId));

            db.questionDao().delete(db.questionDao().byId(7, secondUserId));
            List<Message> remaining = db.messageDao().forQuestion(7, secondUserId);
            assertTrue(remaining.isEmpty());
        } finally {
            db.close();
        }
    }

    @Test
    public void persistedPracticeAndAiStateEnforceIdempotencyAndUserIsolation() {
        AppDatabase db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        try {
            User owner = new User();
            owner.email = "owner@local.invalid";
            owner.displayName = "Owner";
            owner.passwordHash = "hash";
            owner.passwordSalt = "salt";
            long ownerId = db.userDao().insert(owner);

            User other = new User();
            other.email = "other@local.invalid";
            other.displayName = "Other";
            other.passwordHash = "hash";
            other.passwordSalt = "salt";
            long otherId = db.userDao().insert(other);

            Question pending = new Question();
            pending.userId = ownerId;
            pending.prompt = "Explain Room";
            pending.normalizedPrompt = "explain room";
            pending.status = Question.STATUS_PENDING;
            long questionId = db.questionDao().insert(pending);
            assertNull(db.questionDao().byId(questionId, otherId));

            long now = System.currentTimeMillis();
            assertEquals(1, db.questionDao().claimForProcessing(
                    questionId, ownerId, now - 60_000L, now));
            assertEquals(0, db.questionDao().claimForProcessing(
                    questionId, ownerId, now - 60_000L, now + 1L));
            assertEquals(1, db.questionDao().complete(
                    questionId, ownerId, "A persistence library", false, now + 2L));

            AnswerDetail detail = new AnswerDetail();
            detail.questionId = questionId;
            detail.stepsJson = "[{\"title\":\"Persist\",\"body\":\"Use Room\"}]";
            db.answerDetailDao().upsert(detail);
            assertNotNull(db.answerDetailDao().byQuestion(questionId, ownerId));
            assertNull(db.answerDetailDao().byQuestion(questionId, otherId));

            QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
            quizQuestion.userId = ownerId;
            quizQuestion.subject = "code";
            quizQuestion.questionText = "Which library persists local data?";
            quizQuestion.correctAnswer = "Room";
            long quizQuestionId = db.quizDao().insertQuestion(quizQuestion);

            QuizAttempt attempt = new QuizAttempt();
            attempt.userId = ownerId;
            attempt.subject = "code";
            attempt.questionIdsJson = "[" + quizQuestionId + "]";
            attempt.currentQuestionIndex = 0;
            attempt.currentQuestionStartedAt = now;
            attempt.currentQuestionDeadline = now + 24_000L;
            attempt.total = 1;
            long attemptId = db.quizDao().insertAttempt(attempt);
            QuizAttempt resumed = db.quizDao().activeAttempt(ownerId, "code");
            assertNotNull(resumed);
            assertEquals(now + 24_000L, resumed.currentQuestionDeadline);

            QuizAnswer answer = new QuizAnswer();
            answer.attemptId = attemptId;
            answer.quizQuestionId = quizQuestionId;
            answer.userAnswer = "Room";
            answer.correct = true;
            assertTrue(db.quizDao().insertAnswer(answer) > 0L);
            assertEquals(-1L, db.quizDao().insertAnswer(answer));
            assertEquals(1, db.quizDao().answers(attemptId, ownerId).size());

            XpEvent xp = new XpEvent();
            xp.userId = ownerId;
            xp.eventType = XpEvent.TYPE_QUIZ_COMPLETED;
            xp.sourceKey = String.valueOf(attemptId);
            xp.amount = 25;
            assertTrue(db.xpEventDao().insertOnce(xp) > 0L);
            assertEquals(-1L, db.xpEventDao().insertOnce(xp));
            assertEquals(25, db.xpEventDao().totalXp(ownerId));
        } finally {
            db.close();
        }
    }
}
