package com.studymentor.app.api;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * MockOcrService — UC2.5 stand-in for a real vision/OCR call.
 *
 * Produces a fake "recognized text" + confidence score for a captured image
 * after a short delay, so the UX flow (Camera → ScanPreview → Chat) is
 * end-to-end testable without a backend.
 *
 * Swap with a real implementation in Phase 4 (e.g. POST the image bytes to
 * your own backend, which proxies to OpenAI Vision / Claude Vision and
 * returns the parsed text).
 */
public class MockOcrService {

    /** Result struct — what a real OCR/vision call returns to the UI. */
    public static class Result {
        public final String recognizedText;
        public final int confidencePercent; // 0..100
        public final String detectedSubject; // "math" | "code" | "general"
        public final String suggestedStyle;  // "step-by-step" | "brief" | "in-depth"
        public final String detectedLanguage; // ISO code-ish ("en" | "vi")

        public Result(String text, int conf, String subject, String style, String lang) {
            this.recognizedText = text;
            this.confidencePercent = conf;
            this.detectedSubject = subject;
            this.suggestedStyle = style;
            this.detectedLanguage = lang;
        }
    }

    public interface Listener {
        void onSuccess(@NonNull Result result);
        void onError(@NonNull Throwable t);
    }

    private static final String[] SAMPLES = {
            "Giải phương trình bậc hai:\n2x² + 5x − 3 = 0\n\nTìm nghiệm x₁, x₂ và vẽ đồ thị parabol.",
            "Find the derivative of:\nf(x) = 3x³ − 2x² + 5x − 7\n\nThen find f'(2).",
            "What is a Python decorator?\nGive an example using @staticmethod.",
            "Solve for x:\n4(x − 3) + 7 = 2x + 11",
            "Explain Newton's third law of motion with two real-world examples.",
            "Tính tích phân:\n∫(2x + 3) dx từ 0 đến 4",
    };

    /**
     * Mock async OCR. In a real implementation the imageUri would be uploaded
     * to a vision endpoint; here we ignore it and return a random sample after
     * a delay so the UI shows a loading state.
     */
    public static void recognize(final Uri imageUri, final Listener listener) {
        final Random rng = new Random();
        final long delayMs = 900 + rng.nextInt(700); // 0.9–1.6s

        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
                String text = SAMPLES[rng.nextInt(SAMPLES.length)];

                // Pick subject + style + lang heuristically so different samples feel different
                String subject;
                if (text.matches("(?s).*(x²|x³|phương trình|∫|derivative|Solve|Tính).*")) subject = "math";
                else if (text.contains("Python") || text.contains("decorator")) subject = "code";
                else subject = "general";

                String style = subject.equals("math") ? "step-by-step" : "in-depth";
                String lang = text.matches("(?s).*[ìíòóùúảạếẹợỗỹăôơưđ].*") ? "vi" : "en";

                int conf = 85 + rng.nextInt(11); // 85..95
                final Result r = new Result(text, conf, subject, style, lang);

                postToMain(() -> listener.onSuccess(r));
            } catch (InterruptedException e) {
                postToMain(() -> listener.onError(e));
            }
        }, "MockOcr").start();
    }

    private static void postToMain(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    private MockOcrService() {}
}
