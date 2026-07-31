package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.HistoryRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class HistoryViewModel extends AndroidViewModel {
    private final HistoryRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.loading());
    private long userId;
    private String search = "";
    private String subject = "";
    private boolean bookmarkedOnly;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
    }

    public LiveData<State> state() { return state; }

    public void initialize(long userId) {
        this.userId = userId;
        refresh();
    }

    public void filter(String search, String subject, boolean bookmarkedOnly) {
        this.search = search == null ? "" : search;
        this.subject = subject == null ? "" : subject;
        this.bookmarkedOnly = bookmarkedOnly;
        refresh();
    }

    public void refresh() {
        if (userId <= 0) return;
        state.setValue(State.loading());
        repository.search(userId, search, subject, bookmarkedOnly,
                new RepositoryCallback<HistoryRepository.HistoryData>() {
                    @Override public void onSuccess(HistoryRepository.HistoryData value) {
                        state.setValue(State.ready(value));
                    }
                    @Override public void onError(String message, Throwable error) {
                        state.setValue(State.error(message));
                    }
                });
    }

    public void delete(long questionId) {
        repository.delete(userId, questionId, new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void value) { refresh(); }
            @Override public void onError(String message, Throwable error) {
                state.setValue(State.error(message));
            }
        });
    }

    public static final class State {
        public final boolean loading;
        public final HistoryRepository.HistoryData data;
        public final String error;
        private State(boolean loading, HistoryRepository.HistoryData data, String error) {
            this.loading = loading; this.data = data; this.error = error;
        }
        static State loading() { return new State(true, null, null); }
        static State ready(HistoryRepository.HistoryData data) { return new State(false, data, null); }
        static State error(String message) { return new State(false, null, message); }
    }
}

