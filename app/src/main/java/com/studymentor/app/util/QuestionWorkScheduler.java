package com.studymentor.app.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.studymentor.app.worker.PendingQuestionWorker;

public final class QuestionWorkScheduler {
    private QuestionWorkScheduler() {}

    public static void enqueue(Context context, long questionId, long userId) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data input = new Data.Builder()
                .putLong(PendingQuestionWorker.KEY_QUESTION_ID, questionId)
                .putLong(PendingQuestionWorker.KEY_USER_ID, userId)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PendingQuestionWorker.class)
                .setConstraints(constraints)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "pending-question-" + questionId,
                ExistingWorkPolicy.KEEP,
                request);
    }
}

