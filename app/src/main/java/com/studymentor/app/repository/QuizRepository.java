package com.studymentor.app.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.api.AiService;
import com.studymentor.app.api.ApiClient;
import com.studymentor.app.api.QuizGenerationRequest;
import com.studymentor.app.api.QuizGenerationResponse;
import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.data.Question;
import com.studymentor.app.data.QuizAnswer;
import com.studymentor.app.data.QuizAttempt;
import com.studymentor.app.data.QuizQuestionEntity;
import com.studymentor.app.data.UserPreference;
import com.studymentor.app.data.XpEvent;
import com.studymentor.app.util.QuizAnswerEvaluator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizRepository {
    private static final int QUIZ_SIZE = 5;
    private static final int QUIZ_XP = 20;
    private final StudyMentorApp app;
    private final AppDatabase db;
    private final AiService aiService;
    private final Gson gson = new Gson();

    public QuizRepository(Context context) { this(context, ApiClient.get()); }
    QuizRepository(Context context, AiService service) {
        app = StudyMentorApp.get();
        db = app.db();
        aiService = service;
    }

    public void loadOrGenerate(long userId, String subject, RepositoryCallback<List<QuizQuestionEntity>> callback) {
        String resolvedSubject = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        app.executor().execute(() -> {
            try {
                List<QuizQuestionEntity> saved = db.quizDao().questions(userId, resolvedSubject, QUIZ_SIZE);
                if (saved.size() >= QUIZ_SIZE) {
                    postSuccess(callback, saved);
                    return;
                }
                List<Question> sources = db.questionDao().recentCompleted(userId, 8);
                if (sources.isEmpty()) {
                    postError(callback, "Ask at least one question before generating a practice quiz.", null);
                    return;
                }
                UserPreference preference = db.userPreferenceDao().byUser(userId);
                QuizGenerationRequest request = new QuizGenerationRequest();
                request.subject = resolvedSubject.isEmpty() ? "general" : resolvedSubject;
                request.count = QUIZ_SIZE;
                if (preference != null) {
                    request.educationLevel = preference.educationLevel;
                    request.language = preference.language;
                }
                for (Question source : sources) {
                    QuizGenerationRequest.SourceItem item = new QuizGenerationRequest.SourceItem();
                    item.questionId = source.id;
                    item.question = limit(source.prompt, 2_000);
                    item.answer = limit(source.answer, 4_000);
                    request.sources.add(item);
                }
                app.postToMain(() -> generate(userId, request, callback));
            } catch (Exception e) {
                postError(callback, "Unable to load practice questions.", e);
            }
        });
    }

    private void generate(long userId, QuizGenerationRequest request,
                          RepositoryCallback<List<QuizQuestionEntity>> callback) {
        aiService.generateQuiz(request).enqueue(new Callback<QuizGenerationResponse>() {
            @Override public void onResponse(@NonNull Call<QuizGenerationResponse> call,
                                             @NonNull Response<QuizGenerationResponse> response) {
                QuizGenerationResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    postError(callback, "Gemini did not return a valid quiz.", null);
                    return;
                }
                app.executor().execute(() -> persistGenerated(userId, body, callback));
            }

            @Override public void onFailure(@NonNull Call<QuizGenerationResponse> call,
                                            @NonNull Throwable throwable) {
                postError(callback, throwable.getMessage() == null
                        ? "Unable to generate a quiz with Gemini." : throwable.getMessage(), throwable);
            }
        });
    }

    private void persistGenerated(long userId, QuizGenerationResponse response,
                                  RepositoryCallback<List<QuizQuestionEntity>> callback) {
        try {
            List<QuizQuestionEntity> valid = new ArrayList<>();
            if (response.questions != null) {
                for (QuizGenerationResponse.Item item : response.questions) {
                    String validation = validationError(item);
                    if (validation != null) continue;
                    QuizQuestionEntity entity = new QuizQuestionEntity();
                    entity.userId = userId;
                    entity.sourceQuestionId = item.sourceQuestionId;
                    entity.subject = clean(item.subject, "general").toLowerCase(Locale.ROOT);
                    entity.type = item.type;
                    entity.questionText = item.question.trim();
                    entity.optionsJson = gson.toJson(item.options == null ? new ArrayList<>() : item.options);
                    entity.correctAnswer = item.correctAnswer.trim();
                    entity.acceptableAnswersJson = gson.toJson(item.acceptableAnswers == null
                            ? new ArrayList<>() : item.acceptableAnswers);
                    entity.timeLimitSeconds = Math.max(15, Math.min(120,
                            item.timeLimitSeconds <= 0 ? 24 : item.timeLimitSeconds));
                    entity.caseSensitive = item.caseSensitive;
                    entity.numericTolerance = Math.max(0d, item.numericTolerance);
                    entity.explanation = clean(item.explanation, "Review the saved answer for this topic.");
                    valid.add(entity);
                    if (valid.size() == QUIZ_SIZE) break;
                }
            }
            if (valid.isEmpty()) {
                postError(callback, "Gemini returned quiz data that did not pass validation.", null);
                return;
            }
            List<Long> ids = db.quizDao().insertQuestions(valid);
            for (int i = 0; i < valid.size() && i < ids.size(); i++) {
                valid.get(i).id = ids.get(i);
            }
            postSuccess(callback, valid);
        } catch (Exception e) {
            postError(callback, "Generated quiz could not be saved.", e);
        }
    }

    public void prepareAttempt(long userId, String subject,
                               List<QuizQuestionEntity> fallbackQuestions,
                               RepositoryCallback<AttemptSession> callback) {
        String resolvedSubject = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        app.executor().execute(() -> {
            try {
                QuizAttempt active = db.quizDao().activeAttempt(userId, resolvedSubject);
                if (active != null) {
                    List<Long> ids = questionIds(active.questionIdsJson);
                    List<QuizQuestionEntity> restored = ids.isEmpty()
                            ? new ArrayList<>()
                            : orderedQuestions(ids, db.quizDao().questionsByIds(userId, ids));
                    if (!restored.isEmpty() && restored.size() == ids.size()) {
                        postSuccess(callback, new AttemptSession(active, restored,
                                db.quizDao().answers(active.id, userId), true));
                        return;
                    }
                    // An incomplete attempt whose source questions no longer exist cannot be resumed safely.
                    db.quizDao().deleteAttempt(active);
                }

                List<QuizQuestionEntity> selected = fallbackQuestions == null
                        ? new ArrayList<>() : new ArrayList<>(fallbackQuestions);
                if (selected.isEmpty()) {
                    postError(callback, "No valid practice questions are available.", null);
                    return;
                }
                QuizAttempt attempt = new QuizAttempt();
                attempt.userId = userId;
                attempt.subject = resolvedSubject;
                attempt.total = selected.size();
                attempt.status = QuizAttempt.STATUS_ACTIVE;
                attempt.currentQuestionIndex = 0;
                attempt.lastUpdatedAt = System.currentTimeMillis();
                List<Long> ids = new ArrayList<>();
                for (QuizQuestionEntity question : selected) ids.add(question.id);
                attempt.questionIdsJson = gson.toJson(ids);
                attempt.id = db.quizDao().insertAttempt(attempt);
                postSuccess(callback, new AttemptSession(attempt, selected,
                        new ArrayList<>(), false));
            } catch (Exception e) {
                postError(callback, "Could not prepare the quiz session.", e);
            }
        });
    }

    private List<Long> questionIds(String json) {
        try {
            Type type = new TypeToken<List<Long>>() {}.getType();
            List<Long> values = gson.fromJson(json, type);
            return values == null ? new ArrayList<>() : values;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<QuizQuestionEntity> orderedQuestions(
            List<Long> ids, List<QuizQuestionEntity> loaded) {
        Map<Long, QuizQuestionEntity> byId = new HashMap<>();
        if (loaded != null) {
            for (QuizQuestionEntity question : loaded) byId.put(question.id, question);
        }
        List<QuizQuestionEntity> ordered = new ArrayList<>();
        for (Long id : ids) {
            QuizQuestionEntity question = byId.get(id);
            if (question != null) ordered.add(question);
        }
        return ordered;
    }

    public void saveAnswer(long userId, long attemptId, QuizQuestionEntity question,
                           String answer, boolean timedOut, long responseTimeMs,
                           RepositoryCallback<AnswerResult> callback) {
        app.executor().execute(() -> {
            try {
                QuizAttempt attempt = db.quizDao().attempt(attemptId, userId);
                if (attempt == null || attempt.completed) {
                    postError(callback, "This quiz attempt is no longer active.", null);
                    return;
                }
                if (question == null || !questionIds(attempt.questionIdsJson).contains(question.id)) {
                    postError(callback, "This question does not belong to the active quiz attempt.", null);
                    return;
                }
                QuizQuestionEntity persistedQuestion = db.quizDao().question(question.id, userId);
                if (persistedQuestion == null) {
                    postError(callback, "This quiz question is unavailable for the active account.", null);
                    return;
                }
                QuizAnswerEvaluator.Evaluation evaluation =
                        QuizAnswerEvaluator.evaluate(persistedQuestion, answer);
                boolean correct = !timedOut && evaluation.correct;
                QuizAnswer item = new QuizAnswer();
                item.attemptId = attemptId;
                item.quizQuestionId = persistedQuestion.id;
                item.userAnswer = answer == null ? "" : answer.trim();
                item.correct = correct;
                item.timedOut = timedOut;
                item.answerType = persistedQuestion.type;
                item.responseTimeMs = Math.max(0L, responseTimeMs);
                item.feedback = persistedQuestion.explanation;
                long answerId = db.quizDao().insertAnswer(item);
                if (answerId == -1L) {
                    postError(callback, "This quiz question has already been answered in the current attempt.", null);
                    return;
                }
                postSuccess(callback, new AnswerResult(correct, persistedQuestion.explanation));
            } catch (Exception e) { postError(callback, "Answer could not be saved.", e); }
        });
    }

    public void updateQuestionProgress(long userId, long attemptId, int index,
                                       long startedAt, long deadline,
                                       RepositoryCallback<QuizAttempt> callback) {
        app.executor().execute(() -> {
            try {
                int updated = db.quizDao().updateQuestionProgress(attemptId, userId, index,
                        startedAt, deadline, System.currentTimeMillis());
                QuizAttempt attempt = db.quizDao().attempt(attemptId, userId);
                if (updated == 0 || attempt == null) {
                    postError(callback, "The quiz timer could not be persisted.", null);
                    return;
                }
                postSuccess(callback, attempt);
            } catch (Exception e) {
                postError(callback, "The quiz timer could not be persisted.", e);
            }
        });
    }

    public void completeAttempt(long userId, long attemptId,
                                RepositoryCallback<QuizAttempt> callback) {
        app.executor().execute(() -> {
            try {
                QuizAttempt attempt = db.quizDao().attempt(attemptId, userId);
                if (attempt == null) {
                    postError(callback, "Quiz attempt not found.", null);
                    return;
                }
                if (!attempt.completed) {
                    List<QuizAnswer> persistedAnswers = db.quizDao().answers(attemptId, userId);
                    int expectedTotal = questionIds(attempt.questionIdsJson).size();
                    if (expectedTotal <= 0) expectedTotal = attempt.total;
                    if (persistedAnswers.size() < expectedTotal) {
                        postError(callback, "Answer all quiz questions before completing the attempt.", null);
                        return;
                    }
                    int persistedScore = 0;
                    for (QuizAnswer answer : persistedAnswers) if (answer.correct) persistedScore++;
                    attempt.score = persistedScore;
                    attempt.total = expectedTotal;
                    attempt.completed = true;
                    attempt.status = QuizAttempt.STATUS_COMPLETED;
                    attempt.completedAt = System.currentTimeMillis();
                    attempt.currentQuestionDeadline = 0L;
                    attempt.lastUpdatedAt = attempt.completedAt;
                    db.quizDao().updateAttempt(attempt);
                    XpEvent event = new XpEvent();
                    event.userId = userId;
                    event.eventType = XpEvent.TYPE_QUIZ_COMPLETED;
                    event.sourceKey = String.valueOf(attemptId);
                    event.amount = QUIZ_XP;
                    db.xpEventDao().insertOnce(event);
                }
                postSuccess(callback, attempt);
            } catch (Exception e) { postError(callback, "Quiz result could not be saved.", e); }
        });
    }

    public void loadAttempt(long userId, long attemptId, RepositoryCallback<AttemptData> callback) {
        app.executor().execute(() -> {
            try {
                QuizAttempt attempt = db.quizDao().attempt(attemptId, userId);
                if (attempt == null) { postError(callback, "Quiz result not found.", null); return; }
                postSuccess(callback, new AttemptData(attempt, db.quizDao().answers(attemptId, userId)));
            } catch (Exception e) { postError(callback, "Unable to load quiz result.", e); }
        });
    }

    public List<String> options(QuizQuestionEntity question) {
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> values = gson.fromJson(question.optionsJson, type);
            return values == null ? new ArrayList<>() : values;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public static String validationError(QuizGenerationResponse.Item item) {
        if (item == null) return "missing item";
        if (blank(item.question)) return "missing question";
        if (blank(item.correctAnswer)) return "missing correct answer";
        if (!QuizQuestionEntity.TYPE_MULTIPLE_CHOICE.equals(item.type)
                && !QuizQuestionEntity.TYPE_SHORT_ANSWER.equals(item.type)
                && !QuizQuestionEntity.TYPE_FILL_BLANK.equals(item.type)) return "invalid type";
        if (QuizQuestionEntity.TYPE_MULTIPLE_CHOICE.equals(item.type)) {
            if (item.options == null || item.options.size() != 4) return "invalid options";
            Set<String> unique = new HashSet<>();
            boolean answerFound = false;
            for (String option : item.options) {
                if (blank(option) || !unique.add(normalize(option))) return "invalid option";
                if (normalize(option).equals(normalize(item.correctAnswer))) answerFound = true;
            }
            if (!answerFound) return "correct answer is not an option";
        }
        return null;
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String clean(String value, String fallback) { return blank(value) ? fallback : value.trim(); }
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        app.postToMain(() -> callback.onSuccess(value));
    }
    private <T> void postError(RepositoryCallback<T> callback, String message, Throwable error) {
        app.postToMain(() -> callback.onError(message, error));
    }

    public static final class AttemptSession {
        public final QuizAttempt attempt;
        public final List<QuizQuestionEntity> questions;
        public final List<QuizAnswer> answers;
        public final boolean resumed;

        AttemptSession(QuizAttempt attempt, List<QuizQuestionEntity> questions,
                       List<QuizAnswer> answers, boolean resumed) {
            this.attempt = attempt;
            this.questions = questions;
            this.answers = answers;
            this.resumed = resumed;
        }
    }

    public static final class AnswerResult {
        public final boolean correct; public final String feedback;
        AnswerResult(boolean correct, String feedback) { this.correct = correct; this.feedback = feedback; }
    }
    public static final class AttemptData {
        public final QuizAttempt attempt; public final List<QuizAnswer> answers;
        AttemptData(QuizAttempt attempt, List<QuizAnswer> answers) { this.attempt = attempt; this.answers = answers; }
    }
}
