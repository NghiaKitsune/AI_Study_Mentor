package com.studymentor.app.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.studymentor.app.repository.ChatRepository;
import com.studymentor.app.repository.RepositoryCallback;
import com.studymentor.app.repository.ChatRepository.SendResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PendingQuestionWorker extends Worker {
    public static final String KEY_QUESTION_ID = "question_id";
    public static final String KEY_USER_ID = "user_id";

    public PendingQuestionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long questionId = getInputData().getLong(KEY_QUESTION_ID, -1L);
        long userId = getInputData().getLong(KEY_USER_ID, -1L);
        if (questionId <= 0 || userId <= 0) return Result.failure();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> status = new AtomicReference<>();
        new ChatRepository(getApplicationContext()).process(questionId, userId, new RepositoryCallback<SendResult>() {
            @Override public void onSuccess(SendResult value) {
                status.set(value.status);
                latch.countDown();
            }

            @Override public void onError(String message, Throwable error) {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(55, TimeUnit.SECONDS)) return Result.retry();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }
        String finalStatus = status.get();
        if ("COMPLETED".equals(finalStatus)) return Result.success();
        if ("FAILED".equals(finalStatus) || "CANCELLED".equals(finalStatus)) return Result.failure();
        return Result.retry();
    }
}

