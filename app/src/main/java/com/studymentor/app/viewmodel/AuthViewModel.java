package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.AuthRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<AuthState> state = new MutableLiveData<>(AuthState.idle());

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<AuthState> state() {
        return state;
    }

    public void signUp(String email, char[] password) {
        state.setValue(AuthState.loading());
        repository.signUp(email, password, callback());
    }

    public void login(String email, char[] password) {
        state.setValue(AuthState.loading());
        repository.login(email, password, callback());
    }

    public void savePersonalization(long userId, String level, String subjects,
                                    String style, String language) {
        state.setValue(AuthState.loading());
        repository.savePersonalization(userId, level, subjects, style, language,
                new RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void value) {
                        state.setValue(AuthState.personalizationSaved());
                    }

                    @Override public void onError(String message, Throwable error) {
                        state.setValue(AuthState.error(message));
                    }
                });
    }

    private RepositoryCallback<AuthRepository.AuthResult> callback() {
        return new RepositoryCallback<AuthRepository.AuthResult>() {
            @Override public void onSuccess(AuthRepository.AuthResult value) {
                state.setValue(AuthState.authenticated(value));
            }

            @Override public void onError(String message, Throwable error) {
                state.setValue(AuthState.error(message));
            }
        };
    }

    public void clearTransientState() {
        state.setValue(AuthState.idle());
    }

    public static final class AuthState {
        public enum Status { IDLE, LOADING, AUTHENTICATED, PERSONALIZATION_SAVED, ERROR }

        public final Status status;
        public final AuthRepository.AuthResult result;
        public final String message;

        private AuthState(Status status, AuthRepository.AuthResult result, String message) {
            this.status = status;
            this.result = result;
            this.message = message;
        }

        public static AuthState idle() { return new AuthState(Status.IDLE, null, null); }
        public static AuthState loading() { return new AuthState(Status.LOADING, null, null); }
        public static AuthState authenticated(AuthRepository.AuthResult result) {
            return new AuthState(Status.AUTHENTICATED, result, null);
        }
        public static AuthState personalizationSaved() {
            return new AuthState(Status.PERSONALIZATION_SAVED, null, null);
        }
        public static AuthState error(String message) {
            return new AuthState(Status.ERROR, null, message);
        }
    }
}

