package com.studymentor.app.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.studymentor.app.BuildConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Real AI service backed by Google Gemini 2.0 Flash.
 * Replaces MockAiService when BuildConfig.USE_MOCK_AI = false.
 *
 * Uses Gemini structured JSON output so the response maps directly
 * to ChatResponse without any manual parsing heuristics.
 */
public class GeminiAiService implements AiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    private static final String TAG = "GeminiAI";

    public GeminiAiService() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(
                msg -> Log.d(TAG, msg));
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .build();
    }

    @Override
    public Call<ChatResponse> chat(ChatRequest request) {
        return new GeminiCall(request);
    }

    // ── Inner Call wrapper ────────────────────────────────────────────────────

    private class GeminiCall implements Call<ChatResponse> {
        private final ChatRequest req;
        private volatile boolean cancelled;
        private okhttp3.Call activeCall;

        GeminiCall(ChatRequest req) { this.req = req; }

        @Override
        public retrofit2.Response<ChatResponse> execute() throws IOException {
            try (Response r = http.newCall(buildOkRequest(req)).execute()) {
                String body = r.body() != null ? r.body().string() : "";
                return retrofit2.Response.success(parseGeminiResponse(body));
            }
        }

        @Override
        public void enqueue(@NonNull Callback<ChatResponse> cb) {
            if (cancelled) return;
            activeCall = http.newCall(buildOkRequest(req));
            activeCall.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    Log.e(TAG, "HTTP call failed: " + e.getMessage(), e);
                    main.post(() -> cb.onFailure(GeminiCall.this, e));
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response r) {
                    try {
                        String body = r.body() != null ? r.body().string() : "";
                        ChatResponse resp = parseGeminiResponse(body);
                        main.post(() -> cb.onResponse(GeminiCall.this,
                                retrofit2.Response.success(resp)));
                    } catch (Exception e) {
                        main.post(() -> cb.onFailure(GeminiCall.this, e));
                    } finally {
                        r.close();
                    }
                }
            });
        }

        @Override public boolean isExecuted()              { return false; }
        @Override public void cancel()                     { cancelled = true; if (activeCall != null) activeCall.cancel(); }
        @Override public boolean isCanceled()              { return cancelled; }
        @Override public Call<ChatResponse> clone()        { return new GeminiCall(req); }
        @Override public Request request()                 { return buildOkRequest(req); }
        @Override public Timeout timeout()                 { return Timeout.NONE; }
    }

    // ── Request builder ───────────────────────────────────────────────────────

    private Request buildOkRequest(ChatRequest req) {
        String subject = (req.context != null && req.context.subject != null)
                ? req.context.subject : "general";
        String bodyJson = buildBodyJson(req.message, subject);
        RequestBody rb = RequestBody.create(bodyJson, JSON_TYPE);
        return new Request.Builder()
                .url(GEMINI_URL + BuildConfig.GEMINI_API_KEY)
                .post(rb)
                .build();
    }

    private String buildBodyJson(String message, String subject) {
        // gson.toJson(string) adds surrounding quotes + escapes special chars safely
        String escapedMsg    = gson.toJson(message != null ? message : "");
        String escapedSystem = gson.toJson(buildSystemPrompt(subject));

        return "{"
            + "\"system_instruction\":{\"parts\":[{\"text\":" + escapedSystem + "}]},"
            + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":" + escapedMsg + "}]}],"
            + "\"generationConfig\":{"
            +   "\"responseMimeType\":\"application/json\","
            +   "\"temperature\":0.7,"
            +   "\"responseSchema\":{"
            +     "\"type\":\"object\","
            +     "\"properties\":{"
            +       "\"reply\":{\"type\":\"string\"},"
            +       "\"final_answer\":{\"type\":\"string\"},"
            +       "\"steps\":{\"type\":\"array\",\"items\":{"
            +         "\"type\":\"object\","
            +         "\"properties\":{"
            +           "\"index\":{\"type\":\"integer\"},"
            +           "\"title\":{\"type\":\"string\"},"
            +           "\"body\":{\"type\":\"string\"}"
            +         "},"
            +         "\"required\":[\"index\",\"title\",\"body\"]"
            +       "}},"
            +       "\"follow_ups\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},"
            +       "\"commonMistakes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"
            +     "},"
            +     "\"required\":[\"reply\",\"steps\",\"follow_ups\",\"commonMistakes\"]"
            +   "}"
            + "}"
            + "}";
    }

    private static String buildSystemPrompt(String subject) {
        return "You are Milo, a friendly AI study mentor for students.\n"
            + "The student's subject area is: " + subject + ".\n"
            + "IMPORTANT: Always respond in the SAME LANGUAGE as the student's question "
            + "(Vietnamese if asked in Vietnamese, English if asked in English).\n"
            + "Fill each field as follows:\n"
            + "- reply: warm 1-2 sentence intro acknowledging the question\n"
            + "- final_answer: the concise final answer (empty string if the question is open-ended or conceptual)\n"
            + "- steps: 2-5 numbered solution steps; empty array if not applicable\n"
            + "- follow_ups: exactly 3 short follow-up suggestions the student might ask next\n"
            + "- commonMistakes: exactly 2 common mistakes students make on this specific topic\n"
            + "Be clear, encouraging, and educational. Avoid overly long responses.";
    }

    // ── Response parser ───────────────────────────────────────────────────────

    private ChatResponse parseGeminiResponse(String rawBody) {
        try {
            com.google.gson.JsonObject root = gson.fromJson(rawBody, com.google.gson.JsonObject.class);

            // Check for API-level error
            if (root.has("error")) {
                String msg = root.getAsJsonObject("error").get("message").getAsString();
                return errorResponse("API error: " + msg);
            }

            com.google.gson.JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return errorResponse("No candidates returned");
            }

            String text = candidates.get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0)
                    .getAsJsonObject()
                    .get("text")
                    .getAsString();

            ChatResponse resp = gson.fromJson(text, ChatResponse.class);
            if (resp == null) return errorResponse("Empty response body");

            // Guarantee non-null lists so UI never NPEs
            if (resp.steps          == null) resp.steps          = new ArrayList<>();
            if (resp.follow_ups     == null) resp.follow_ups     = new ArrayList<>();
            if (resp.commonMistakes == null) resp.commonMistakes = new ArrayList<>();

            return resp;

        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage() + " | body=" + rawBody, e);
            return errorResponse(e.getMessage());
        }
    }

    private static ChatResponse errorResponse(String detail) {
        ChatResponse r = new ChatResponse();
        r.reply = "I couldn't reach my brain right now. Try again in a sec!";
        r.steps = new ArrayList<>();
        r.follow_ups = new ArrayList<>();
        r.commonMistakes = new ArrayList<>();
        r.error = new ChatResponse.ErrorInfo();
        r.error.code = "model_error";
        r.error.message = detail;
        return r;
    }
}
