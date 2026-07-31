package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.ProgressRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class ProgressViewModel extends AndroidViewModel {
    private final ProgressRepository repository;
    private final MutableLiveData<ProgressRepository.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private long userId;

    public ProgressViewModel(@NonNull Application application) {
        super(application); repository = new ProgressRepository(application);
    }
    public LiveData<ProgressRepository.Snapshot> snapshot() { return snapshot; }
    public LiveData<String> error() { return error; }
    public void initialize(long userId) { this.userId = userId; refresh(); }
    public void refresh() {
        if (userId <= 0) return;
        repository.load(userId, new RepositoryCallback<ProgressRepository.Snapshot>() {
            @Override public void onSuccess(ProgressRepository.Snapshot value) { snapshot.setValue(value); }
            @Override public void onError(String message, Throwable throwable) { error.setValue(message); }
        });
    }
}

