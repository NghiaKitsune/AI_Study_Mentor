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
 * Real AI service backed by Groq (llama-3.3-70b-versatile).
 * Free tier: 30 RPM · 14,400 req/day · no billing required.
 * OpenAI-compatible API → JSON mode for structured output.
 */
public class GroqAiService implements AiService {

    private static final String TAG      = "GroqAI";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    public GroqAiService() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(msg -> Log.d(TAG, msg));
        logger.setLevel(HttpLoggingInterceptor.Level.BASIC);
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .build();
    }

    @Override
    public Call<ChatResponse> chat(ChatRequest request) {
        return new GroqCall(request);
    }

    // ── Dashboard "Milo's insight" (short plain-text summary) ─────────────────

    public interface InsightCallback {
        void onSuccess(String insight);
        void onError(String message);
    }

    /**
     * Asks Milo for a 1-2 sentence encouraging observation about the
     * student's stats. {@code statsSummary} is a plain-English line describing
     * streak/XP/subject counts/best quiz score — see DashboardActivity.
     */
    public void quickInsight(String statsSummary, InsightCallback cb) {
        RequestBody rb = RequestBody.create(buildInsightBodyJson(statsSummary), JSON_TYPE);
        Request req = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(rb)
                .build();

        http.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                Log.e(TAG, "Insight HTTP failure: " + e.getMessage(), e);
                main.post(() -> cb.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response r) {
                try {
                    String raw = r.body() != null ? r.body().string() : "";
                    String insight = parseInsightResponse(raw);
                    main.post(() -> cb.onSuccess(insight));
                } catch (Exception e) {
                    Log.e(TAG, "Insight parse error: " + e.getMessage(), e);
                    main.post(() -> cb.onError(e.getMessage()));
                } finally {
                    r.close();
                }
            }
        });
    }

    private String buildInsightBodyJson(String statsSummary) {
        String system = "You are Milo, a friendly AI study mentor mascot. "
            + "Given a student's study stats, write exactly 1-2 short, warm, encouraging "
            + "sentences (max 220 characters total) that reference a real pattern in the "
            + "stats and suggest one concrete next step. "
            + "Respond in ENGLISH with plain text only — no JSON, no markdown, no quotes.";
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0.7,"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":" + gson.toJson(system) + "},"
            +   "{\"role\":\"user\",\"content\":" + gson.toJson(statsSummary) + "}"
            + "]"
            + "}";
    }

    private String parseInsightResponse(String rawBody) throws Exception {
        com.google.gson.JsonObject root = gson.fromJson(rawBody, com.google.gson.JsonObject.class);
        if (root.has("error")) {
            throw new Exception(root.getAsJsonObject("error").get("message").getAsString());
        }
        String content = root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        return content.trim();
    }

    // ── Inner Call wrapper ────────────────────────────────────────────────────

    private class GroqCall implements Call<ChatResponse> {
        private final ChatRequest req;
        private volatile boolean cancelled;
        private okhttp3.Call activeCall;

        GroqCall(ChatRequest req) { this.req = req; }

        @Override
        public retrofit2.Response<ChatResponse> execute() throws IOException {
            try (Response r = http.newCall(buildOkRequest(req)).execute()) {
                String body = r.body() != null ? r.body().string() : "";
                return retrofit2.Response.success(parseGroqResponse(body));
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
                    main.post(() -> cb.onFailure(GroqCall.this, e));
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response r) {
                    try {
                        String body = r.body() != null ? r.body().string() : "";
                        ChatResponse resp = parseGroqResponse(body);
                        main.post(() -> cb.onResponse(GroqCall.this,
                                retrofit2.Response.success(resp)));
                    } catch (Exception e) {
                        Log.e(TAG, "Response handling error: " + e.getMessage(), e);
                        main.post(() -> cb.onFailure(GroqCall.this, e));
                    } finally {
                        r.close();
                    }
                }
            });
        }

        @Override public boolean isExecuted()       { return false; }
        @Override public void cancel()              { cancelled = true; if (activeCall != null) activeCall.cancel(); }
        @Override public boolean isCanceled()       { return cancelled; }
        @Override public Call<ChatResponse> clone() { return new GroqCall(req); }
        @Override public Request request()          { return buildOkRequest(req); }
        @Override public Timeout timeout()          { return Timeout.NONE; }
    }

    // ── Request builder ───────────────────────────────────────────────────────

    private Request buildOkRequest(ChatRequest req) {
        String subject = (req.context != null && req.context.subject != null)
                ? req.context.subject : "general";
        String bodyJson = buildBodyJson(req.message, subject);
        RequestBody rb = RequestBody.create(bodyJson, JSON_TYPE);
        return new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(rb)
                .build();
    }

    private String buildBodyJson(String message, String subject) {
        String escapedSystem = gson.toJson(buildSystemPrompt(subject));
        String escapedMsg    = gson.toJson(message != null ? message : "");
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0.7,"
            + "\"response_format\":{\"type\":\"json_object\"},"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":" + escapedSystem + "},"
            +   "{\"role\":\"user\",\"content\":" + escapedMsg + "}"
            + "]"
            + "}";
    }

    private static String buildSystemPrompt(String subject) {
        return "You are Milo, a friendly AI study mentor for students.\n"
            + "Subject area: " + subject + ".\n"
            + "IMPORTANT: Always respond in ENGLISH regardless of the language the student uses.\n"
            + "You MUST respond with a valid JSON object and nothing else. Use this exact structure:\n"
            + "{\n"
            + "  \"reply\": \"...\",\n"
            + "  \"final_answer\": \"...\",\n"
            + "  \"steps\": [{\"index\": 1, \"title\": \"Step title\", \"body\": \"Step explanation\"}],\n"
            + "  \"follow_ups\": [\"suggestion 1\", \"suggestion 2\", \"suggestion 3\"],\n"
            + "  \"commonMistakes\": [\"mistake 1\", \"mistake 2\"]\n"
            + "}\n"
            + "Rules for each field:\n"
            + "- reply: For math/calculation questions, STATE THE ANSWER DIRECTLY in the reply "
            + "(e.g. '12 + 5 = 17. Đây là cách mình tính:'). "
            + "For conceptual questions, give a warm 1-2 sentence intro.\n"
            + "- final_answer: the short standalone answer (e.g. '17', 'x = 3', 'Newton\\'s 3rd law'). "
            + "Empty string if open-ended.\n"
            + "- steps: 2-5 items for any problem-solving or calculation question. "
            + "Empty array [] only for pure greetings.\n"
            + "- follow_ups: exactly 3 short follow-up suggestions\n"
            + "- commonMistakes: exactly 2 pitfalls students make on this topic\n"
            + "- Be concise, clear, and encouraging";
    }

    // ── Response parser ───────────────────────────────────────────────────────

    private ChatResponse parseGroqResponse(String rawBody) {
        try {
            Log.d(TAG, "Raw response: " + rawBody);
            com.google.gson.JsonObject root = gson.fromJson(rawBody, com.google.gson.JsonObject.class);

            // API-level error
            if (root.has("error")) {
                String msg = root.getAsJsonObject("error").get("message").getAsString();
                Log.e(TAG, "Groq API error: " + msg);
                return errorResponse("API error: " + msg);
            }

            // Parse choices[0].message.content
            String content = root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            Log.d(TAG, "Parsed content: " + content);
            ChatResponse resp = gson.fromJson(content, ChatResponse.class);
            if (resp == null) return errorResponse("Empty parse result");

            // Guarantee non-null lists
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
