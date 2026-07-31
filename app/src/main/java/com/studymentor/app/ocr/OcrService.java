package com.studymentor.app.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.studymentor.app.StudyMentorApp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real on-device OCR with bounded background decoding and lifecycle-safe callbacks. */
public final class OcrService implements AutoCloseable {
    private static final int MAX_IMAGE_DIMENSION = 2048;

    private final Context appContext;
    private final StudyMentorApp app;
    private final TextRecognizer recognizer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean recognizerClosed = new AtomicBoolean(false);
    private volatile Task<Text> activeTask;

    public OcrService(Context context) {
        appContext = context.getApplicationContext();
        app = StudyMentorApp.get();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void recognize(@NonNull Uri uri, @NonNull Callback callback) {
        if (closed.get()) {
            callback.onError("OCR is no longer available for this screen.", null);
            return;
        }
        app.executor().execute(() -> decodeAndRecognize(uri, callback));
    }

    private void decodeAndRecognize(Uri uri, Callback callback) {
        final Bitmap bitmap;
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(appContext.getContentResolver(), uri);
            bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                int width = info.getSize().getWidth();
                int height = info.getSize().getHeight();
                int largest = Math.max(width, height);
                if (largest > MAX_IMAGE_DIMENSION) {
                    float scale = MAX_IMAGE_DIMENSION / (float) largest;
                    decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                            Math.max(1, Math.round(height * scale)));
                }
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } catch (IOException | SecurityException error) {
            postError(callback, "The selected image could not be opened.", error);
            return;
        }

        if (closed.get()) {
            recycle(bitmap);
            return;
        }
        app.postToMain(() -> startRecognition(bitmap, callback));
    }

    private void startRecognition(Bitmap bitmap, Callback callback) {
        if (closed.get()) {
            recycle(bitmap);
            closeRecognizer();
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        activeTask = recognizer.process(image)
                .addOnSuccessListener(result -> {
                    if (closed.get()) return;
                    String text = result.getText() == null ? "" : result.getText().trim();
                    if (text.isEmpty()) {
                        callback.onError("No readable text was detected in this image.", null);
                    } else {
                        callback.onSuccess(text);
                    }
                })
                .addOnFailureListener(error -> {
                    if (!closed.get()) {
                        callback.onError("The image could not be recognized. Try a clearer, well-lit photo.", error);
                    }
                })
                .addOnCompleteListener(task -> {
                    recycle(bitmap);
                    activeTask = null;
                    if (closed.get()) closeRecognizer();
                });
    }

    private void postError(Callback callback, String message, Throwable error) {
        if (closed.get()) return;
        app.postToMain(() -> {
            if (!closed.get()) callback.onError(message, error);
        });
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
    }

    public void cancel() {
        // ML Kit Tasks do not expose cancellation. Closing suppresses late callbacks;
        // the decoded bitmap is recycled by the completion listener.
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && activeTask == null) {
            closeRecognizer();
        }
    }

    private void closeRecognizer() {
        if (recognizerClosed.compareAndSet(false, true)) recognizer.close();
    }

    public interface Callback {
        void onSuccess(String text);
        void onError(String message, Throwable error);
    }
}

