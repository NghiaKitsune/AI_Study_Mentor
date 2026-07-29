package com.studymentor.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studymentor.app.repository.HistoryRepository;
import com.studymentor.app.repository.RepositoryCallback;

public class AnswerViewModel extends AndroidViewModel {
    private final HistoryRepository repository;
    private final MutableLiveData<HistoryRepository.QuestionDetail> detail = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private long userId, questionId;

    public AnswerViewModel(@NonNull Application application) {
        super(application); repository = new HistoryRepository(application);
    }
    public LiveData<HistoryRepository.QuestionDetail> detail() { return detail; }
    public LiveData<String> error() { return error; }
    public void load(long userId, long questionId) {
        this.userId=userId; this.questionId=questionId;
        repository.detail(userId, questionId, new RepositoryCallback<HistoryRepository.QuestionDetail>() {
            @Override public void onSuccess(HistoryRepository.QuestionDetail value) { detail.setValue(value); }
            @Override public void onError(String message, Throwable throwable) { error.setValue(message); }
        });
    }
    public void recordReviewDuration(long durationSeconds) {
        repository.recordReviewDuration(userId, questionId, durationSeconds);
    }

    public void toggleBookmark() {
        HistoryRepository.QuestionDetail current=detail.getValue();
        if(current==null)return;
        boolean next=!current.question.bookmarked;
        repository.setBookmarked(userId,questionId,next,new RepositoryCallback<Boolean>(){
            @Override public void onSuccess(Boolean value){current.question.bookmarked=value;detail.setValue(current);}
            @Override public void onError(String message,Throwable throwable){error.setValue(message);}
        });
    }
}

