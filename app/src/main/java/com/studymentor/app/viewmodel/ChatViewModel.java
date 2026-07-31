package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.data.Message;
import com.studymentor.app.data.Question;
import com.studymentor.app.repository.ChatRepository;
import com.studymentor.app.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatState> state = new MutableLiveData<>(ChatState.idle());
    private long questionId = -1L;
    private long userId = -1L;
    private boolean sending;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    public LiveData<List<Message>> messages() { return messages; }
    public LiveData<ChatState> state() { return state; }
    public long questionId() { return questionId; }

    public void initialize(long questionId, long userId) {
        if (this.userId != userId) repository.resumePending(userId);
        this.userId = userId;
        if (questionId > 0 && this.questionId != questionId) {
            this.questionId = questionId;
            reload();
        }
    }

    public void send(String prompt, String subject) {
        if (sending) return;
        sending = true;
        state.setValue(ChatState.loading(questionId));
        repository.send(userId, prompt, subject, new RepositoryCallback<ChatRepository.SendResult>() {
            @Override public void onSuccess(ChatRepository.SendResult value) {
                questionId = value.questionId;
                state.setValue(ChatState.from(value));
                if (Question.STATUS_COMPLETED.equals(value.status)
                        || Question.STATUS_PENDING.equals(value.status)
                        || Question.STATUS_FAILED.equals(value.status)) {
                    sending = false;
                    reload();
                }
            }

            @Override public void onError(String message, Throwable error) {
                sending = false;
                state.setValue(ChatState.error(questionId, message));
            }
        });
    }

    public void retry() {
        if (sending || questionId <= 0) return;
        sending = true;
        state.setValue(ChatState.loading(questionId));
        repository.retry(questionId, userId, new RepositoryCallback<ChatRepository.SendResult>() {
            @Override public void onSuccess(ChatRepository.SendResult value) {
                state.setValue(ChatState.from(value));
                sending = false;
                reload();
            }

            @Override public void onError(String message, Throwable error) {
                sending = false;
                state.setValue(ChatState.error(questionId, message));
            }
        });
    }

    public void reload() {
        if (questionId <= 0 || userId <= 0) return;
        repository.load(questionId, userId, new RepositoryCallback<ChatRepository.ConversationData>() {
            @Override public void onSuccess(ChatRepository.ConversationData value) {
                messages.setValue(value.messages);
                state.setValue(new ChatState(value.question.status, questionId,
                        value.question.errorMessage, value.question.fromCache));
            }

            @Override public void onError(String message, Throwable error) {
                state.setValue(ChatState.error(questionId, message));
            }
        });
    }

    public static final class ChatState {
        public final String status;
        public final long questionId;
        public final String message;
        public final boolean fromCache;

        ChatState(String status, long questionId, String message, boolean fromCache) {
            this.status = status;
            this.questionId = questionId;
            this.message = message;
            this.fromCache = fromCache;
        }

        static ChatState idle() { return new ChatState("IDLE", -1, null, false); }
        static ChatState loading(long id) { return new ChatState(Question.STATUS_PROCESSING, id, null, false); }
        static ChatState error(long id, String message) { return new ChatState(Question.STATUS_FAILED, id, message, false); }
        static ChatState from(ChatRepository.SendResult value) {
            return new ChatState(value.status, value.questionId, value.message, value.fromCache);
        }
    }
}

