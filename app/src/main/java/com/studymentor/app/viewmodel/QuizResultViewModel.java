package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.QuizRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class QuizResultViewModel extends AndroidViewModel {
    private final QuizRepository repository;
    private final MutableLiveData<QuizRepository.AttemptData> data=new MutableLiveData<>();
    private final MutableLiveData<String> error=new MutableLiveData<>();
    public QuizResultViewModel(@NonNull Application app){super(app);repository=new QuizRepository(app);}
    public LiveData<QuizRepository.AttemptData> data(){return data;}public LiveData<String> error(){return error;}
    public void load(long userId,long attemptId){repository.loadAttempt(userId,attemptId,new RepositoryCallback<QuizRepository.AttemptData>(){
        @Override public void onSuccess(QuizRepository.AttemptData value){data.setValue(value);}
        @Override public void onError(String message,Throwable throwable){error.setValue(message);}
    });}
}

