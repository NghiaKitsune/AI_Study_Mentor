package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.data.ReminderSchedule;
import com.studymentor.app.repository.NotificationRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class NotificationViewModel extends AndroidViewModel {
    private final NotificationRepository repository;
    private final MutableLiveData<NotificationRepository.NotificationData> data = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private long userId;
    private String type = "";

    public NotificationViewModel(@NonNull Application application) {
        super(application); repository = new NotificationRepository(application);
    }
    public LiveData<NotificationRepository.NotificationData> data() { return data; }
    public LiveData<String> error() { return error; }
    public void initialize(long userId) { this.userId = userId; refresh(); }
    public void filter(String type) { this.type = type == null ? "" : type; refresh(); }
    public void refresh() {
        if (userId <= 0) return;
        repository.load(userId, type, new RepositoryCallback<NotificationRepository.NotificationData>() {
            @Override public void onSuccess(NotificationRepository.NotificationData value) { data.setValue(value); }
            @Override public void onError(String message, Throwable throwable) { error.setValue(message); }
        });
    }
    public void markRead(long id) {
        repository.markRead(userId, id, new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void value) { refresh(); }
            @Override public void onError(String message, Throwable errorValue) { error.setValue(message); }
        });
    }
    public void setReminder(boolean enabled, int days, int hour) {
        repository.setReminder(userId, enabled, days, hour, new RepositoryCallback<ReminderSchedule>() {
            @Override public void onSuccess(ReminderSchedule value) { refresh(); }
            @Override public void onError(String message, Throwable errorValue) { error.setValue(message); }
        });
    }
}

