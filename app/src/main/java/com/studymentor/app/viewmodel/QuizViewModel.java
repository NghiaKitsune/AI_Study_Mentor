package com.studymentor.app.viewmodel;

import android.app.Application;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.data.QuizAnswer;
import com.studymentor.app.data.QuizAttempt;
import com.studymentor.app.data.QuizQuestionEntity;
import com.studymentor.app.repository.QuizRepository;
import com.studymentor.app.repository.RepositoryCallback;
import com.studymentor.app.util.QuizTimerMath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Owns the persisted quiz state machine and deadline-based timer. */
public class QuizViewModel extends AndroidViewModel {
    private final QuizRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.loading());
    private final MutableLiveData<Long> remainingMillis = new MutableLiveData<>(0L);
    private final List<QuizQuestionEntity> questions = new ArrayList<>();
    private long userId;
    private long attemptId;
    private QuizAttempt attempt;
    private int index;
    private int score;
    private boolean submitting;
    private CountDownTimer timer;

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = new QuizRepository(application);
    }

    public LiveData<State> state() { return state; }
    public LiveData<Long> remainingMillis() { return remainingMillis; }
    public List<String> options(QuizQuestionEntity question) { return repository.options(question); }
    public long attemptId() { return attemptId; }

    public void initialize(long userId, String subject) {
        if (this.userId > 0) return;
        this.userId = userId;
        state.setValue(State.loading());
        String resolved = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        repository.loadOrGenerate(userId, resolved,
                new RepositoryCallback<List<QuizQuestionEntity>>() {
                    @Override public void onSuccess(List<QuizQuestionEntity> value) {
                        repository.prepareAttempt(userId, resolved, value,
                                new RepositoryCallback<QuizRepository.AttemptSession>() {
                                    @Override public void onSuccess(QuizRepository.AttemptSession session) {
                                        restoreSession(session);
                                    }
                                    @Override public void onError(String message, Throwable error) {
                                        state.setValue(State.error(message));
                                    }
                                });
                    }
                    @Override public void onError(String message, Throwable error) {
                        state.setValue(State.error(message));
                    }
                });
    }

    private void restoreSession(QuizRepository.AttemptSession session) {
        questions.clear();
        questions.addAll(session.questions);
        attempt = session.attempt;
        attemptId = attempt.id;
        score = 0;
        submitting = false;
        Set<Long> answered = new HashSet<>();
        if (session.answers != null) {
            for (QuizAnswer answer : session.answers) {
                answered.add(answer.quizQuestionId);
                if (answer.correct) score++;
            }
        }
        index = Math.max(0, Math.min(attempt.currentQuestionIndex,
                Math.max(0, questions.size() - 1)));
        while (index < questions.size() && answered.contains(questions.get(index).id)) index++;
        if (index >= questions.size()) complete();
        else activateQuestion();
    }

    private void activateQuestion() {
        if (questions.isEmpty()) {
            state.setValue(State.error("No valid quiz questions are available."));
            return;
        }
        QuizQuestionEntity question = questions.get(index);
        long now = System.currentTimeMillis();
        if (attempt.currentQuestionIndex == index && attempt.currentQuestionDeadline > 0L) {
            publishQuestion(question, attempt.currentQuestionDeadline);
            startTimer(attempt.currentQuestionDeadline);
            return;
        }
        int seconds = Math.max(15, Math.min(120,
                question.timeLimitSeconds <= 0 ? 24 : question.timeLimitSeconds));
        long deadline = now + seconds * 1000L;
        repository.updateQuestionProgress(userId, attemptId, index, now, deadline,
                new RepositoryCallback<QuizAttempt>() {
                    @Override public void onSuccess(QuizAttempt value) {
                        attempt = value;
                        publishQuestion(question, deadline);
                        startTimer(deadline);
                    }
                    @Override public void onError(String message, Throwable error) {
                        state.setValue(State.error(message));
                    }
                });
    }

    public void submit(String answer) { submit(answer, false); }

    private void submit(String answer, boolean timedOut) {
        State current = state.getValue();
        if (current == null || current.question == null
                || current.answerChecked || submitting) return;
        submitting = true;
        cancelTimer();
        long responseTime = attempt == null || attempt.currentQuestionStartedAt <= 0
                ? 0L : System.currentTimeMillis() - attempt.currentQuestionStartedAt;
        repository.saveAnswer(userId, attemptId, current.question, answer, timedOut,
                responseTime, new RepositoryCallback<QuizRepository.AnswerResult>() {
                    @Override public void onSuccess(QuizRepository.AnswerResult value) {
                        submitting = false;
                        if (value.correct) score++;
                        state.setValue(State.checked(current.question, index, questions.size(),
                                score, value.correct, value.feedback, timedOut, answer,
                                current.deadlineEpochMs));
                    }
                    @Override public void onError(String message, Throwable error) {
                        submitting = false;
                        state.setValue(State.error(message));
                    }
                });
    }

    public void next() {
        State current = state.getValue();
        if (current == null || !current.answerChecked) return;
        if (index + 1 < questions.size()) {
            index++;
            attempt.currentQuestionIndex = index;
            attempt.currentQuestionStartedAt = 0L;
            attempt.currentQuestionDeadline = 0L;
            activateQuestion();
        } else complete();
    }

    private void complete() {
        cancelTimer();
        repository.completeAttempt(userId, attemptId,
                new RepositoryCallback<QuizAttempt>() {
                    @Override public void onSuccess(QuizAttempt value) {
                        state.setValue(State.completed(value));
                    }
                    @Override public void onError(String message, Throwable error) {
                        state.setValue(State.error(message));
                    }
                });
    }

    private void publishQuestion(QuizQuestionEntity question, long deadline) {
        state.setValue(State.question(question, index, questions.size(), score, deadline));
    }

    private void startTimer(long deadline) {
        cancelTimer();
        long remaining = QuizTimerMath.remainingMillis(deadline, System.currentTimeMillis());
        remainingMillis.setValue(remaining);
        if (remaining <= 0L) {
            submit("", true);
            return;
        }
        timer = new CountDownTimer(remaining, 250L) {
            @Override public void onTick(long value) {
                remainingMillis.setValue(QuizTimerMath.remainingMillis(
                        deadline, System.currentTimeMillis()));
            }
            @Override public void onFinish() {
                remainingMillis.setValue(0L);
                submit("", true);
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) { timer.cancel(); timer = null; }
    }

    @Override protected void onCleared() {
        cancelTimer();
        super.onCleared();
    }

    public static final class State {
        public final boolean loading;
        public final boolean answerChecked;
        public final boolean correct;
        public final boolean timedOut;
        public final boolean complete;
        public final QuizQuestionEntity question;
        public final int index;
        public final int total;
        public final int score;
        public final String feedback;
        public final String submittedAnswer;
        public final String error;
        public final long deadlineEpochMs;
        public final QuizAttempt attempt;

        private State(boolean loading, QuizQuestionEntity question, int index, int total,
                      int score, boolean checked, boolean correct, boolean timedOut,
                      String feedback, String submittedAnswer, String error,
                      long deadlineEpochMs, boolean complete, QuizAttempt attempt) {
            this.loading = loading;
            this.question = question;
            this.index = index;
            this.total = total;
            this.score = score;
            this.answerChecked = checked;
            this.correct = correct;
            this.timedOut = timedOut;
            this.feedback = feedback;
            this.submittedAnswer = submittedAnswer;
            this.error = error;
            this.deadlineEpochMs = deadlineEpochMs;
            this.complete = complete;
            this.attempt = attempt;
        }

        static State loading() {
            return new State(true, null, 0, 0, 0, false, false, false,
                    null, null, null, 0L, false, null);
        }
        static State question(QuizQuestionEntity question, int index, int total,
                              int score, long deadline) {
            return new State(false, question, index, total, score, false, false, false,
                    null, null, null, deadline, false, null);
        }
        static State checked(QuizQuestionEntity question, int index, int total, int score,
                             boolean correct, String feedback, boolean timedOut,
                             String answer, long deadline) {
            return new State(false, question, index, total, score, true, correct, timedOut,
                    feedback, answer, null, deadline, false, null);
        }
        static State error(String error) {
            return new State(false, null, 0, 0, 0, false, false, false,
                    null, null, error, 0L, false, null);
        }
        static State completed(QuizAttempt attempt) {
            return new State(false, null, 0, attempt.total, attempt.score,
                    false, false, false, null, null, null, 0L, true, attempt);
        }
    }
}
