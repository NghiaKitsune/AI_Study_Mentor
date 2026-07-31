package com.studymentor.app.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T value);
    void onError(String message, Throwable error);
}

