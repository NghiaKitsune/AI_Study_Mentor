package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.RepositoryCallback;
import com.studymentor.app.repository.UserRepository;

public class UserViewModel extends AndroidViewModel {
    private final UserRepository repository;
    private final MutableLiveData<UserRepository.UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application); repository = new UserRepository(application);
    }
    public LiveData<UserRepository.UserProfile> profile() { return profile; }
    public LiveData<String> error() { return error; }
    public void load(long userId) {
        repository.profile(userId, new RepositoryCallback<UserRepository.UserProfile>() {
            @Override public void onSuccess(UserRepository.UserProfile value) { profile.setValue(value); }
            @Override public void onError(String message, Throwable throwable) { error.setValue(message); }
        });
    }

    public void setLanguage(long userId, String language) {
        repository.setLanguage(userId, language, new RepositoryCallback<UserRepository.UserProfile>() {
            @Override public void onSuccess(UserRepository.UserProfile value) { profile.setValue(value); }
            @Override public void onError(String message, Throwable throwable) { error.setValue(message); }
        });
    }
}

