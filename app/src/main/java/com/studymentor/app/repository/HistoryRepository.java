package com.studymentor.app.repository;

import android.content.Context;

import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.AnswerDetail;
import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.data.LearningEvent;
import com.studymentor.app.data.Question;
import com.studymentor.app.data.XpEvent;

import java.util.List;

public class HistoryRepository {
    private static final int REVIEW_XP = 2;
    private final StudyMentorApp app;
    private final AppDatabase db;

    public HistoryRepository(Context context) {
        app = StudyMentorApp.get();
        db = app.db();
    }

    public void search(long userId, String search, String subject, boolean bookmarkedOnly,
                       RepositoryCallback<HistoryData> callback) {
        app.executor().execute(() -> {
            try {
                List<Question> items = db.questionDao().search(userId,
                        search == null ? "" : search.trim(),
                        subject == null ? "" : subject.trim(), bookmarkedOnly);
                HistoryData data = new HistoryData(items, db.questionDao().count(userId),
                        db.questionDao().bookmarkedCount(userId));
                postSuccess(callback, data);
            } catch (Exception e) {
                postError(callback, "Unable to load question history.", e);
            }
        });
    }

    public void detail(long userId, long questionId, RepositoryCallback<QuestionDetail> callback) {
        app.executor().execute(() -> {
            try {
                Question question = db.questionDao().byId(questionId, userId);
                if (question == null) {
                    postError(callback, "This question is not available for the current account.", null);
                    return;
                }
                AnswerDetail detail = db.answerDetailDao().byQuestion(questionId, userId);
                if (Question.STATUS_COMPLETED.equals(question.status)
                        && question.answer != null && !question.answer.trim().isEmpty()) {
                    awardReviewXp(userId, questionId);
                }
                postSuccess(callback, new QuestionDetail(question, detail));
            } catch (Exception e) {
                postError(callback, "Unable to load the saved answer.", e);
            }
        });
    }

    public void setBookmarked(long userId, long questionId, boolean bookmarked,
                              RepositoryCallback<Boolean> callback) {
        app.executor().execute(() -> {
            try {
                int updated = db.questionDao().setBookmarked(questionId, userId, bookmarked,
                        System.currentTimeMillis());
                if (updated == 0) {
                    postError(callback, "Bookmark was not updated.", null);
                } else {
                    postSuccess(callback, bookmarked);
                }
            } catch (Exception e) {
                postError(callback, "Bookmark was not updated.", e);
            }
        });
    }

    public void delete(long userId, long questionId, RepositoryCallback<Void> callback) {
        app.executor().execute(() -> {
            try {
                Question question = db.questionDao().byId(questionId, userId);
                if (question == null) {
                    postError(callback, "Question not found.", null);
                    return;
                }
                db.questionDao().delete(question);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, "Question could not be deleted.", e);
            }
        });
    }


    public void recordReviewDuration(long userId, long questionId, long durationSeconds) {
        if (userId <= 0 || questionId <= 0 || durationSeconds < 5) return;
        app.executor().execute(() -> {
            Question question = db.questionDao().byId(questionId, userId);
            if (question == null || !Question.STATUS_COMPLETED.equals(question.status)) return;
            LearningEvent event = new LearningEvent();
            event.userId = userId;
            event.eventType = LearningEvent.TYPE_ANSWER_REVIEW;
            event.sourceKey = String.valueOf(questionId);
            event.durationSeconds = Math.min(durationSeconds, 60L * 60L);
            db.learningEventDao().insert(event);
        });
    }

    private void awardReviewXp(long userId, long questionId) {
        XpEvent event = new XpEvent();
        event.userId = userId;
        event.eventType = XpEvent.TYPE_ANSWER_REVIEWED;
        event.sourceKey = String.valueOf(questionId);
        event.amount = REVIEW_XP;
        db.xpEventDao().insertOnce(event);
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        app.postToMain(() -> callback.onSuccess(value));
    }

    private <T> void postError(RepositoryCallback<T> callback, String message, Throwable error) {
        app.postToMain(() -> callback.onError(message, error));
    }

    public static final class HistoryData {
        public final List<Question> items;
        public final int total;
        public final int bookmarks;
        public HistoryData(List<Question> items, int total, int bookmarks) {
            this.items = items;
            this.total = total;
            this.bookmarks = bookmarks;
        }
    }

    public static final class QuestionDetail {
        public final Question question;
        public final AnswerDetail detail;
        public QuestionDetail(Question question, AnswerDetail detail) {
            this.question = question;
            this.detail = detail;
        }
    }
}

