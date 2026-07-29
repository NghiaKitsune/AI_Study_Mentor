package com.studymentor.app.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                User.class,
                UserPreference.class,
                Question.class,
                Message.class,
                AnswerDetail.class,
                QuizQuestionEntity.class,
                QuizAttempt.class,
                QuizAnswer.class,
                LearningEvent.class,
                XpEvent.class,
                UserAchievement.class,
                NotificationEntity.class,
                ReminderSchedule.class
        },
        version = 2,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract UserPreferenceDao userPreferenceDao();
    public abstract QuestionDao questionDao();
    public abstract MessageDao messageDao();
    public abstract AnswerDetailDao answerDetailDao();
    public abstract QuizDao quizDao();
    public abstract LearningEventDao learningEventDao();
    public abstract XpEventDao xpEventDao();
    public abstract UserAchievementDao userAchievementDao();
    public abstract NotificationDao notificationDao();
    public abstract ReminderScheduleDao reminderScheduleDao();
}
