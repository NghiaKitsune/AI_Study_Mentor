package com.studymentor.app.repository;

import android.content.Context;

import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.data.SubjectCount;
import com.studymentor.app.data.User;
import com.studymentor.app.data.UserAchievement;
import com.studymentor.app.data.XpEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgressRepository {
    private final StudyMentorApp app;
    private final AppDatabase db;

    public ProgressRepository(Context context) {
        app = StudyMentorApp.get();
        db = app.db();
    }

    public void load(long userId, RepositoryCallback<Snapshot> callback) {
        app.executor().execute(() -> {
            try {
                User user = db.userDao().byId(userId);
                if (user == null) {
                    postError(callback, "Current account was not found.", null);
                    return;
                }
                int totalQuestions = db.questionDao().count(userId);
                int bookmarks = db.questionDao().bookmarkedCount(userId);
                int attempts = db.quizDao().completedAttemptCount(userId);
                int correct = db.quizDao().totalCorrect(userId);
                int answered = db.quizDao().totalAnswered(userId);
                int accuracy = answered == 0 ? 0 : Math.round(correct * 100f / answered);
                int xp = db.xpEventDao().totalXp(userId);
                long reviewSeconds = db.learningEventDao().totalReviewSeconds(userId);
                int level = levelForXp(xp);
                int streak = calculateStreak(db.questionDao().completedTimestamps(userId),
                        db.quizDao().completedTimestamps(userId));
                unlockAchievements(userId, totalQuestions, bookmarks, attempts, accuracy, streak);
                int achievements = db.userAchievementDao().count(userId);
                List<UserAchievement> achievementItems = db.userAchievementDao().all(userId);
                Snapshot snapshot = new Snapshot(user, totalQuestions, bookmarks, attempts,
                        accuracy, xp, level, streak, achievements, reviewSeconds,
                        db.questionDao().subjectCounts(userId), db.xpEventDao().recent(userId, 8), achievementItems);
                postSuccess(callback, snapshot);
            } catch (Exception e) {
                postError(callback, "Unable to calculate learning progress.", e);
            }
        });
    }

    private void unlockAchievements(long userId, int questions, int bookmarks, int attempts,
                                    int accuracy, int streak) {
        if (questions >= 1) unlock(userId, "FIRST_QUESTION");
        if (questions >= 10) unlock(userId, "QUESTION_10");
        if (bookmarks >= 5) unlock(userId, "BOOKMARK_5");
        if (attempts >= 1) unlock(userId, "FIRST_QUIZ");
        if (attempts >= 3 && accuracy >= 80) unlock(userId, "QUIZ_ACCURACY_80");
        if (streak >= 3) unlock(userId, "STREAK_3");
    }

    private void unlock(long userId, String key) {
        UserAchievement achievement = new UserAchievement();
        achievement.userId = userId;
        achievement.achievementKey = key;
        db.userAchievementDao().unlock(achievement);
    }

    static int levelForXp(int xp) { return Math.max(1, xp / 250 + 1); }
    public static int levelStartXp(int level) { return Math.max(0, level - 1) * 250; }
    public static int nextLevelXp(int level) { return Math.max(1, level) * 250; }

    static int calculateStreak(List<Long> questions, List<Long> attempts) {
        Set<LocalDate> dates = new HashSet<>();
        ZoneId zone = ZoneId.systemDefault();
        for (Long value : questions) if (value != null && value > 0)
            dates.add(Instant.ofEpochMilli(value).atZone(zone).toLocalDate());
        for (Long value : attempts) if (value != null && value > 0)
            dates.add(Instant.ofEpochMilli(value).atZone(zone).toLocalDate());
        if (dates.isEmpty()) return 0;
        LocalDate cursor = LocalDate.now(zone);
        if (!dates.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!dates.contains(cursor)) return 0;
        }
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        app.postToMain(() -> callback.onSuccess(value));
    }
    private <T> void postError(RepositoryCallback<T> callback, String message, Throwable e) {
        app.postToMain(() -> callback.onError(message, e));
    }

    public static final class Snapshot {
        public final User user;
        public final int totalQuestions;
        public final int bookmarks;
        public final int quizAttempts;
        public final int accuracy;
        public final int xp;
        public final int level;
        public final int streak;
        public final int achievements;
        public final long reviewSeconds;
        public final List<SubjectCount> subjects;
        public final List<XpEvent> recentEvents;
        public final List<UserAchievement> achievementItems;

        Snapshot(User user, int totalQuestions, int bookmarks, int quizAttempts, int accuracy,
                 int xp, int level, int streak, int achievements, long reviewSeconds,
                 List<SubjectCount> subjects, List<XpEvent> recentEvents, List<UserAchievement> achievementItems) {
            this.user = user;
            this.totalQuestions = totalQuestions;
            this.bookmarks = bookmarks;
            this.quizAttempts = quizAttempts;
            this.accuracy = accuracy;
            this.xp = xp;
            this.level = level;
            this.streak = streak;
            this.achievements = achievements;
            this.reviewSeconds = reviewSeconds;
            this.subjects = subjects == null ? new ArrayList<>() : subjects;
            this.recentEvents = recentEvents == null ? new ArrayList<>() : recentEvents;
            this.achievementItems = achievementItems == null ? new ArrayList<>() : achievementItems;
        }
    }
}

