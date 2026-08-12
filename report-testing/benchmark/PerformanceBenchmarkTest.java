package com.studymentor.app.performance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.studymentor.app.BuildConfig;
import com.studymentor.app.api.AiServiceException;
import com.studymentor.app.api.ChatRequest;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.api.GeminiAiService;
import com.studymentor.app.ocr.OcrService;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Response;

/**
 * Test-only benchmark for report evidence.
 *
 * Runs:
 * - 10 real Gemini requests with different prompts.
 * - 10 OCR runs on one fixed generated text image.
 *
 * Raw results are written to the DEBUG app's internal storage:
 * files/report-testing/performance-results-auto.csv
 *
 * The companion PowerShell script runs instrumentation manually and immediately
 * exports that CSV with `adb exec-out run-as`, before any package cleanup can
 * remove the result.
 */
@RunWith(AndroidJUnit4.class)
public class PerformanceBenchmarkTest {
    private static final String TAG = "PerfBenchmark";
    private static final int RUNS = 10;
    private static final long OCR_TIMEOUT_SECONDS = 30L;

    private static final String[] GEMINI_PROMPTS = {
            "What is photosynthesis? Give a concise student explanation.",
            "Explain Newton's first law with one simple example.",
            "What is a prime number? Give two examples.",
            "Explain binary search in simple terms.",
            "What is DNA and what is its main role?",
            "What is cloud computing? Give one everyday example.",
            "Explain the water cycle in four short steps.",
            "What is object-oriented programming? Define it briefly.",
            "Explain gravity in simple terms for a student.",
            "What is an algorithm? Give one simple example."
    };

    @Test
    public void benchmarkGeminiAndOcrAndWriteCsv() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            throw new AssertionError(
                    "GEMINI_API_KEY is empty. Configure it in local.properties before running this benchmark.");
        }

        File outputDir = new File(context.getFilesDir(), "report-testing");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new AssertionError("Cannot create benchmark output directory: " + outputDir);
        }

        File output = new File(outputDir, "performance-results-auto.csv");
        writeHeader(output);

        final String device = Build.MANUFACTURER + " " + Build.MODEL;
        final String api = String.valueOf(Build.VERSION.SDK_INT);
        final String testDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        final String network = networkLabel(context);

        Log.i(TAG, "Starting benchmark.");

        runGeminiBenchmark(output, device, api, network, testDate);
        runOcrBenchmark(context, output, device, api, testDate);

        Log.i(TAG, "Benchmark complete.");
        System.out.println("PERFORMANCE_BENCHMARK_COMPLETE");
    }

    private void runGeminiBenchmark(
            File output,
            String device,
            String api,
            String network,
            String testDate
    ) throws IOException {
        GeminiAiService service = new GeminiAiService();

        for (int i = 0; i < RUNS; i++) {
            ChatRequest request = new ChatRequest();
            request.message = GEMINI_PROMPTS[i];
            request.context.educationLevel = "university";
            request.context.subject = "general";
            request.context.language = "en";
            request.context.explanationStyle = "concise";

            long start = SystemClock.elapsedRealtime();
            long duration;
            int success = 0;
            String note = "";

            try {
                Response<ChatResponse> response = service.chat(request).execute();
                duration = SystemClock.elapsedRealtime() - start;

                ChatResponse body = response.body();
                if (body != null && hasText(body)) {
                    success = 1;
                } else {
                    note = "EMPTY_PARSED_RESPONSE";
                }
            } catch (Exception error) {
                duration = SystemClock.elapsedRealtime() - start;
                note = errorCode(error);
            }

            appendRow(
                    output,
                    "Gemini response",
                    i + 1,
                    GEMINI_PROMPTS[i],
                    "GeminiAiService.chat execute start",
                    "Parsed structured response or error returned",
                    duration,
                    success,
                    note,
                    device,
                    api,
                    network,
                    testDate
            );

            Log.i(TAG, "Gemini run " + (i + 1)
                    + ": " + duration + " ms, success=" + success
                    + (note.isEmpty() ? "" : ", note=" + note));

            SystemClock.sleep(1200L);
        }
    }

    private void runOcrBenchmark(
            Context context,
            File output,
            String device,
            String api,
            String testDate
    ) throws Exception {
        File imageFile = createFixedOcrImage(context);
        Uri imageUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                imageFile
        );

        OcrService service = new OcrService(context);
        try {
            for (int i = 0; i < RUNS; i++) {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<String> text = new AtomicReference<>("");
                AtomicReference<String> errorNote = new AtomicReference<>("");

                long start = SystemClock.elapsedRealtime();

                service.recognize(imageUri, new OcrService.Callback() {
                    @Override
                    public void onSuccess(String recognizedText) {
                        text.set(recognizedText == null ? "" : recognizedText.trim());
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message, Throwable error) {
                        String type = error == null ? "" : error.getClass().getSimpleName();
                        errorNote.set((message == null ? "OCR_ERROR" : message)
                                + (type.isEmpty() ? "" : " [" + type + "]"));
                        latch.countDown();
                    }
                });

                boolean completed = latch.await(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                long duration = SystemClock.elapsedRealtime() - start;

                int success;
                String note;
                if (!completed) {
                    success = 0;
                    note = "OCR_TIMEOUT_" + OCR_TIMEOUT_SECONDS + "S";
                } else if (!text.get().isEmpty()) {
                    success = 1;
                    note = "";
                } else {
                    success = 0;
                    note = errorNote.get().isEmpty() ? "NO_RECOGNIZED_TEXT" : errorNote.get();
                }

                appendRow(
                        output,
                        "OCR",
                        i + 1,
                        "Fixed generated printed-text PNG",
                        "OcrService.recognize called",
                        "onSuccess/onError callback returned",
                        duration,
                        success,
                        note,
                        device,
                        api,
                        "On-device",
                        testDate
                );

                Log.i(TAG, "OCR run " + (i + 1)
                        + ": " + duration + " ms, success=" + success
                        + (note.isEmpty() ? "" : ", note=" + note));

                SystemClock.sleep(300L);
            }
        } finally {
            service.close();
        }
    }

    private static boolean hasText(ChatResponse response) {
        return (response.finalAnswer != null && !response.finalAnswer.trim().isEmpty())
                || (response.reply != null && !response.reply.trim().isEmpty());
    }

    private static String errorCode(Exception error) {
        if (error instanceof AiServiceException) {
            AiServiceException ai = (AiServiceException) error;
            if (ai.httpStatus > 0) {
                return ai.code + "_HTTP_" + ai.httpStatus;
            }
            return ai.code;
        }
        String name = error.getClass().getSimpleName();
        return name == null || name.isEmpty() ? "ERROR" : name;
    }

    private static File createFixedOcrImage(Context context) throws IOException {
        File scansDir = new File(context.getCacheDir(), "scans");
        if (!scansDir.exists() && !scansDir.mkdirs()) {
            throw new IOException("Cannot create OCR benchmark scan directory.");
        }

        File output = new File(scansDir, "performance_ocr_fixture.png");

        Bitmap bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextSize(64f);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);
        body.setTextSize(52f);
        body.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        canvas.drawText("AI STUDY MENTOR OCR BENCHMARK", 80f, 120f, title);
        canvas.drawText("Photosynthesis is the process by which plants", 80f, 260f, body);
        canvas.drawText("convert light energy into chemical energy.", 80f, 340f, body);
        canvas.drawText("This fixed image is used for ten OCR runs.", 80f, 460f, body);
        canvas.drawText("The text and image remain unchanged each time.", 80f, 540f, body);

        try (FileOutputStream stream = new FileOutputStream(output, false)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new IOException("Could not write OCR benchmark image.");
            }
        } finally {
            bitmap.recycle();
        }

        return output;
    }

    private static String networkLabel(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";

        Network active = cm.getActiveNetwork();
        if (active == null) return "Offline";

        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        if (caps == null) return "Unknown";

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile";
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
        return "Other";
    }

    private static void writeHeader(File output) throws IOException {
        try (FileWriter writer = new FileWriter(output, false)) {
            writer.write(
                    "Metric,Run,Test input / condition,Start condition,End condition,"
                            + "Duration_ms,Success (1/0),Error code / note,"
                            + "Device or AVD,Android API,Network,Test date\n"
            );
        }
    }

    private static synchronized void appendRow(
            File output,
            String metric,
            int run,
            String input,
            String start,
            String end,
            long durationMs,
            int success,
            String note,
            String device,
            String api,
            String network,
            String date
    ) throws IOException {
        try (FileWriter writer = new FileWriter(output, true)) {
            writer.write(csv(metric));
            writer.write(",");
            writer.write(String.valueOf(run));
            writer.write(",");
            writer.write(csv(input));
            writer.write(",");
            writer.write(csv(start));
            writer.write(",");
            writer.write(csv(end));
            writer.write(",");
            writer.write(String.valueOf(durationMs));
            writer.write(",");
            writer.write(String.valueOf(success));
            writer.write(",");
            writer.write(csv(note));
            writer.write(",");
            writer.write(csv(device));
            writer.write(",");
            writer.write(csv(api));
            writer.write(",");
            writer.write(csv(network));
            writer.write(",");
            writer.write(csv(date));
            writer.write("\n");
        }
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
