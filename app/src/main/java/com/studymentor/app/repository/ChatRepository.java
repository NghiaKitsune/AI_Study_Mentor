package com.studymentor.app.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.api.AiService;
import com.studymentor.app.api.AiServiceException;
import com.studymentor.app.api.ApiClient;
import com.studymentor.app.api.ChatRequest;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.data.AnswerDetail;
import com.studymentor.app.data.AppDatabase;
import com.studymentor.app.data.Message;
import com.studymentor.app.data.Question;
import com.studymentor.app.data.UserPreference;
import com.studymentor.app.data.XpEvent;
import com.studymentor.app.util.NetworkUtils;
import com.studymentor.app.util.QuestionWorkScheduler;
import com.studymentor.app.util.TextNormalizer;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {
    public static final int MAX_PROMPT_CHARS = 8_000;
    private static final int QUESTION_XP = 10;
    private static final long PROCESSING_STALE_MS = 2 * 60 * 1000L;

    private final Context appContext;
    private final StudyMentorApp app;
    private final AppDatabase db;
    private final AiService ai;
    private final Gson gson = new Gson();

    public ChatRepository(Context context) {
        this(context, ApiClient.get());
    }

    ChatRepository(Context context, AiService ai) {
        appContext = context.getApplicationContext();
        app = StudyMentorApp.get();
        db = app.db();
        this.ai = ai;
    }

    public void load(long questionId, long userId, RepositoryCallback<ConversationData> callback) {
        app.executor().execute(() -> {
            Question question = db.questionDao().byId(questionId, userId);
            if (question == null) {
                postError(callback, "This question is unavailable for the active account.", null);
                return;
            }
            List<Message> messages = db.messageDao().forQuestion(questionId, userId);
            AnswerDetail detail = db.answerDetailDao().byQuestion(questionId, userId);
            postSuccess(callback, new ConversationData(question, messages, detail));
        });
    }

    public void send(long userId, String prompt, String subject, RepositoryCallback<SendResult> callback) {
        final String cleanPrompt = prompt == null ? "" : prompt.trim();
        if (userId <= 0 || cleanPrompt.isEmpty()) {
            postError(callback, "Enter a question before sending.", null);
            return;
        }
        if (cleanPrompt.length() > MAX_PROMPT_CHARS) {
            postError(callback, "The question is too long. Keep it under " + MAX_PROMPT_CHARS + " characters.", null);
            return;
        }
        app.executor().execute(() -> {
            try {
                UserPreference preference = preferenceFor(userId);
                String normalized = TextNormalizer.normalizeQuestion(cleanPrompt);
                String resolvedSubject = subject == null || subject.trim().isEmpty()
                        ? detectSubject(cleanPrompt) : subject.trim().toLowerCase(Locale.ROOT);
                Question cached = db.questionDao().findCache(userId, normalized, resolvedSubject,
                        preference.educationLevel, preference.explanationStyle, preference.language);

                Question question = new Question();
                question.userId = userId;
                question.requestId = UUID.randomUUID().toString();
                question.prompt = cleanPrompt;
                question.normalizedPrompt = normalized;
                question.subject = resolvedSubject;
                question.educationLevel = preference.educationLevel;
                question.explanationStyle = preference.explanationStyle;
                question.language = preference.language;
                question.status = cached == null ? Question.STATUS_PENDING : Question.STATUS_COMPLETED;
                question.fromCache = cached != null;
                if (cached != null) question.answer = cached.answer;

                final long[] insertedId = new long[1];
                db.runInTransaction(() -> {
                    long questionId = db.questionDao().insert(question);
                    insertedId[0] = questionId;
                    db.messageDao().insert(Message.user(questionId, cleanPrompt));
                    if (cached != null) {
                        db.messageDao().insert(Message.assistant(questionId, cached.answer == null ? "" : cached.answer));
                        AnswerDetail cachedDetail = db.answerDetailDao().byQuestion(cached.id, userId);
                        if (cachedDetail != null) {
                            cachedDetail.questionId = questionId;
                            db.answerDetailDao().upsert(cachedDetail);
                        }
                    }
                });
                long questionId = insertedId[0];
                if (cached != null) {
                    postSuccess(callback, new SendResult(questionId, Question.STATUS_COMPLETED, true, null));
                    return;
                }
                if (!NetworkUtils.isOnline(appContext)) {
                    QuestionWorkScheduler.enqueue(appContext, questionId, userId);
                    postSuccess(callback, new SendResult(questionId, Question.STATUS_PENDING, false,
                            "Saved offline. It will be sent when the connection returns."));
                    return;
                }
                postSuccess(callback, new SendResult(questionId, Question.STATUS_PROCESSING, false, null));
                process(questionId, userId, callback);
            } catch (Exception e) {
                postError(callback, "Unable to save the question.", e);
            }
        });
    }

    public void resumePending(long userId) {
        if (userId <= 0) return;
        app.executor().execute(() -> {
            long now = System.currentTimeMillis();
            db.questionDao().recoverStaleProcessing(userId, now - PROCESSING_STALE_MS, now);
            for (Question pending : db.questionDao().pending(userId)) {
                QuestionWorkScheduler.enqueue(appContext, pending.id, userId);
            }
        });
    }

    public void retry(long questionId, long userId, RepositoryCallback<SendResult> callback) {
        app.executor().execute(() -> {
            Question question = db.questionDao().byId(questionId, userId);
            if (question == null) {
                postError(callback, "This question is unavailable.", null);
                return;
            }
            if (!NetworkUtils.isOnline(appContext)) {
                db.questionDao().updateStatus(questionId, userId, Question.STATUS_PENDING,
                        "OFFLINE", "Waiting for a network connection.", System.currentTimeMillis());
                QuestionWorkScheduler.enqueue(appContext, questionId, userId);
                postSuccess(callback, new SendResult(questionId, Question.STATUS_PENDING, false,
                        "Still offline. The question remains queued."));
                return;
            }
            process(questionId, userId, callback);
        });
    }

    /** Used by WorkManager; callback can be null. */
    public void process(long questionId, long userId, RepositoryCallback<SendResult> callback) {
        app.executor().execute(() -> {
            Question question = db.questionDao().byId(questionId, userId);
            if (question == null || Question.STATUS_COMPLETED.equals(question.status)
                    || Question.STATUS_CANCELLED.equals(question.status)) {
                if (callback != null) postSuccess(callback,
                        new SendResult(questionId, question == null ? Question.STATUS_FAILED : question.status, false, null));
                return;
            }
            if (!NetworkUtils.isOnline(appContext)) {
                db.questionDao().updateStatus(questionId, userId, Question.STATUS_PENDING,
                        "OFFLINE", "Waiting for a network connection.", System.currentTimeMillis());
                QuestionWorkScheduler.enqueue(appContext, questionId, userId);
                if (callback != null) postSuccess(callback,
                        new SendResult(questionId, Question.STATUS_PENDING, false, "Waiting for a connection."));
                return;
            }

            long now = System.currentTimeMillis();
            int claimed = db.questionDao().claimForProcessing(questionId, userId,
                    now - PROCESSING_STALE_MS, now);
            if (claimed == 0) {
                Question current = db.questionDao().byId(questionId, userId);
                if (callback != null) {
                    postSuccess(callback, new SendResult(questionId,
                            current == null ? Question.STATUS_FAILED : current.status,
                            current != null && current.fromCache,
                            current == null ? "This question is unavailable." : current.errorMessage));
                }
                return;
            }
            question.status = Question.STATUS_PROCESSING;
            question.updatedAt = now;
            ChatRequest request = buildRequest(question);
            Call<ChatResponse> call = ai.chat(request);
            call.enqueue(new Callback<ChatResponse>() {
                @Override
                public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                    ChatResponse body = response.body();
                    if (!response.isSuccessful() || body == null) {
                        handleFailure(question, new AiServiceException(AiServiceException.EMPTY_RESPONSE,
                                "Gemini returned an empty response."), callback);
                        return;
                    }
                    saveCompleted(question, body, callback);
                }

                @Override
                public void onFailure(Call<ChatResponse> call, Throwable throwable) {
                    handleFailure(question, throwable, callback);
                }
            });
        });
    }

    private ChatRequest buildRequest(Question question) {
        ChatRequest request = new ChatRequest(question.requestId, question.prompt);
        request.context.subject = question.subject;
        request.context.educationLevel = question.educationLevel;
        request.context.explanationStyle = question.explanationStyle;
        request.context.language = question.language;
        List<Message> history = db.messageDao().forQuestion(question.id, question.userId);
        int limit = Math.min(history.size(), 8);
        for (int i = Math.max(0, history.size() - limit); i < history.size(); i++) {
            Message message = history.get(i);
            if (message.role.equals(Message.ROLE_USER) && message.text.equals(question.prompt)) continue;
            request.history.add(new ChatRequest.ConversationMessage(message.role, message.text));
        }
        return request;
    }

    private void saveCompleted(Question question, ChatResponse response, RepositoryCallback<SendResult> callback) {
        app.executor().execute(() -> {
            try {
                String answer = chooseAnswer(response);
                if (answer.isEmpty()) {
                    handleFailure(question, new AiServiceException(AiServiceException.EMPTY_RESPONSE,
                            "Gemini returned no educational answer."), callback);
                    return;
                }
                AnswerDetail detail = new AnswerDetail();
                detail.questionId = question.id;
                detail.stepsJson = gson.toJson(nonNull(response.steps));
                detail.keyConceptsJson = gson.toJson(nonNull(response.keyConcepts));
                detail.commonMistakesJson = gson.toJson(nonNull(response.commonMistakes));
                detail.alternativeApproach = response.alternativeApproach == null ? "" : response.alternativeApproach;
                detail.examplesJson = gson.toJson(nonNull(response.examples));
                detail.followUpsJson = gson.toJson(nonNull(response.followUps));
                final int[] completed = {0};
                db.runInTransaction(() -> {
                    completed[0] = db.questionDao().complete(question.id, question.userId,
                            answer, false, System.currentTimeMillis());
                    if (completed[0] == 0) return;
                    db.messageDao().insert(Message.assistant(question.id, answer));
                    db.answerDetailDao().upsert(detail);
                    awardQuestionXp(question.userId, question.id);
                });
                if (callback != null) {
                    Question current = db.questionDao().byId(question.id, question.userId);
                    postSuccess(callback, new SendResult(question.id,
                            current == null ? Question.STATUS_FAILED : current.status,
                            current != null && current.fromCache,
                            current == null ? "This question is unavailable." : current.errorMessage));
                }
            } catch (Exception e) {
                handleFailure(question, e, callback);
            }
        });
    }

    private void handleFailure(Question question, Throwable throwable, RepositoryCallback<SendResult> callback) {
        app.executor().execute(() -> {
            boolean retryable = throwable instanceof UnknownHostException
                    || throwable instanceof SocketTimeoutException
                    || !(throwable instanceof AiServiceException);
            String code = throwable instanceof AiServiceException
                    ? ((AiServiceException) throwable).code : "NETWORK_ERROR";
            String message = userMessage(throwable);
            String status = retryable ? Question.STATUS_PENDING : Question.STATUS_FAILED;
            int changed = db.questionDao().failProcessing(question.id, question.userId,
                    status, code, message, System.currentTimeMillis());
            if (changed > 0 && retryable) {
                QuestionWorkScheduler.enqueue(appContext, question.id, question.userId);
            }
            if (callback != null) {
                Question current = db.questionDao().byId(question.id, question.userId);
                postSuccess(callback, new SendResult(question.id,
                        current == null ? Question.STATUS_FAILED : current.status,
                        current != null && current.fromCache,
                        current == null ? "This question is unavailable." : current.errorMessage));
            }
        });
    }

    private UserPreference preferenceFor(long userId) {
        UserPreference preference = db.userPreferenceDao().byUser(userId);
        if (preference == null) {
            preference = new UserPreference();
            preference.userId = userId;
            db.userPreferenceDao().upsert(preference);
        }
        return preference;
    }

    private void awardQuestionXp(long userId, long questionId) {
        XpEvent event = new XpEvent();
        event.userId = userId;
        event.eventType = XpEvent.TYPE_QUESTION_COMPLETED;
        event.sourceKey = String.valueOf(questionId);
        event.amount = QUESTION_XP;
        db.xpEventDao().insertOnce(event);
    }

    private static String chooseAnswer(ChatResponse response) {
        String finalAnswer = response.finalAnswer == null ? "" : response.finalAnswer.trim();
        String reply = response.reply == null ? "" : response.reply.trim();
        if (!reply.isEmpty() && !finalAnswer.isEmpty() && !reply.equals(finalAnswer)) {
            return reply + "\n\n" + finalAnswer;
        }
        return !finalAnswer.isEmpty() ? finalAnswer : reply;
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private static String userMessage(Throwable throwable) {
        if (throwable instanceof AiServiceException) {
            AiServiceException e = (AiServiceException) throwable;
            if (AiServiceException.MISSING_KEY.equals(e.code)) return e.getMessage();
            if (AiServiceException.RATE_LIMIT.equals(e.code)) return "Gemini rate limit reached. Try again later.";
            if (AiServiceException.SAFETY_BLOCK.equals(e.code)) return "Gemini could not answer this request because of safety controls.";
            if (AiServiceException.PARSE_ERROR.equals(e.code)) return "Gemini returned an unexpected response format. Try again.";
            return e.getMessage() == null ? "Gemini could not answer this question." : e.getMessage();
        }
        if (throwable instanceof SocketTimeoutException) return "The Gemini request timed out and was queued for retry.";
        return "The question was saved and will retry when the connection is available.";
    }

    private static String detectSubject(String prompt) {
        String value = prompt.toLowerCase(Locale.ROOT);
        String[] math = {"equation", "solve", "integral", "derivative", "algebra", "geometry", "phương trình", "toán", "tích phân"};
        String[] science = {"physics", "chemistry", "biology", "science", "vật lý", "hóa", "sinh học"};
        String[] code = {"code", "java", "python", "algorithm", "database", "sql", "programming", "lập trình"};
        String[] history = {"history", "war", "dynasty", "historical", "lịch sử", "chiến tranh", "triều đại"};
        for (String keyword : math) if (value.contains(keyword)) return "math";
        for (String keyword : science) if (value.contains(keyword)) return "science";
        for (String keyword : code) if (value.contains(keyword)) return "code";
        for (String keyword : history) if (value.contains(keyword)) return "history";
        return "general";
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        app.postToMain(() -> callback.onSuccess(value));
    }

    private <T> void postError(RepositoryCallback<T> callback, String message, Throwable error) {
        app.postToMain(() -> callback.onError(message, error));
    }

    public static final class ConversationData {
        public final Question question;
        public final List<Message> messages;
        public final AnswerDetail detail;

        public ConversationData(Question question, List<Message> messages, AnswerDetail detail) {
            this.question = question;
            this.messages = messages;
            this.detail = detail;
        }
    }

    public static final class SendResult {
        public final long questionId;
        public final String status;
        public final boolean fromCache;
        public final String message;

        public SendResult(long questionId, String status, boolean fromCache, String message) {
            this.questionId = questionId;
            this.status = status;
            this.fromCache = fromCache;
            this.message = message;
        }
    }
}
