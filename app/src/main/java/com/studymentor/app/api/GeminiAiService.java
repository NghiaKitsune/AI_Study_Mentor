package com.studymentor.app.api;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.studymentor.app.BuildConfig;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;

/** Gemini generateContent REST client with structured JSON output and no sensitive body logging. */
public class GeminiAiService implements AiService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public Call<ChatResponse> chat(ChatRequest request) {
        return new GeminiCall<>(buildChatBody(request), ChatResponse.class);
    }

    @Override
    public Call<QuizGenerationResponse> generateQuiz(QuizGenerationRequest request) {
        return new GeminiCall<>(buildQuizBody(request), QuizGenerationResponse.class);
    }

    private Request buildRequest(JsonObject body) throws AiServiceException {
        String key = BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
        if (key.isEmpty()) {
            throw new AiServiceException(AiServiceException.MISSING_KEY,
                    "Gemini API is not configured. Add GEMINI_API_KEY to local.properties.");
        }
        String model = BuildConfig.GEMINI_MODEL == null || BuildConfig.GEMINI_MODEL.trim().isEmpty()
                ? "gemini-2.5-flash" : BuildConfig.GEMINI_MODEL.trim();
        return new Request.Builder()
                .url(BASE_URL + model + ":generateContent")
                .header("x-goog-api-key", key)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();
    }

    private JsonObject buildChatBody(ChatRequest request) {
        JsonObject root = new JsonObject();
        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", chatSystemPrompt(request.context));
        systemParts.add(systemText);
        systemInstruction.add("parts", systemParts);
        root.add("systemInstruction", systemInstruction);

        JsonArray contents = new JsonArray();
        if (request.history != null) {
            int start = Math.max(0, request.history.size() - 8);
            for (int i = start; i < request.history.size(); i++) {
                ChatRequest.ConversationMessage item = request.history.get(i);
                if (item == null || item.text == null || item.text.trim().isEmpty()) continue;
                contents.add(content("assistant".equals(item.role) ? "model" : "user", item.text));
            }
        }
        contents.add(content("user", request.message));
        root.add("contents", contents);
        root.add("generationConfig", generationConfig(chatSchema(), 0.35, 4096));
        return root;
    }

    private JsonObject buildQuizBody(QuizGenerationRequest request) {
        JsonObject root = new JsonObject();
        JsonObject systemInstruction = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("text", "Create a practice quiz only from the supplied saved questions and answers. " +
                "Do not invent user history. Use a balanced mix of MULTIPLE_CHOICE, SHORT_ANSWER, and FILL_BLANK. " +
                "For multiple choice provide exactly four distinct options and make correct_answer exactly match one option. " +
                "For other types options must be empty and acceptable_answers should include useful equivalent answers. " +
                "Use a time_limit_seconds from 15 to 120, case_sensitive=false unless casing is essential, and numeric_tolerance=0 unless a numeric approximation is valid. " +
                "Keep feedback educational and concise. " +
                "Respond in " + safe(request.language, "en") + " for a " +
                safe(request.educationLevel, "student") + " learner.");
        parts.add(text);
        systemInstruction.add("parts", parts);
        root.add("systemInstruction", systemInstruction);

        JsonObject payload = new JsonObject();
        payload.addProperty("subject", safe(request.subject, "general"));
        payload.addProperty("count", Math.max(1, Math.min(request.count, 10)));
        payload.add("sources", gson.toJsonTree(request.sources));
        root.add("contents", new JsonArray());
        root.getAsJsonArray("contents").add(content("user", gson.toJson(payload)));
        root.add("generationConfig", generationConfig(quizSchema(), 0.25, 4096));
        return root;
    }

    private static JsonObject content(String role, String text) {
        JsonObject content = new JsonObject();
        content.addProperty("role", role);
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text == null ? "" : text);
        parts.add(part);
        content.add("parts", parts);
        return content;
    }

    private static JsonObject generationConfig(JsonObject schema, double temperature, int maxOutputTokens) {
        JsonObject config = new JsonObject();
        config.addProperty("responseMimeType", "application/json");
        config.addProperty("temperature", temperature);
        config.addProperty("maxOutputTokens", maxOutputTokens);
        // JSON Schema uses lower-case type names and avoids the deprecated OpenAPI responseSchema field.
        config.add("responseJsonSchema", schema);
        return config;
    }

    private static JsonObject chatSchema() {
        JsonObject step = objectSchema();
        step.add("properties", properties(
                property("index", scalar("integer")),
                property("title", scalar("string")),
                property("body", scalar("string"))));
        step.add("required", strings("index", "title", "body"));

        JsonObject schema = objectSchema();
        schema.add("properties", properties(
                property("reply", scalar("string")),
                property("final_answer", scalar("string")),
                property("steps", arrayOf(step)),
                property("key_concepts", arrayOf(scalar("string"))),
                property("common_mistakes", arrayOf(scalar("string"))),
                property("alternative_approach", scalar("string")),
                property("examples", arrayOf(scalar("string"))),
                property("follow_ups", arrayOf(scalar("string")))));
        schema.add("required", strings("reply", "final_answer", "steps", "key_concepts",
                "common_mistakes", "alternative_approach", "examples", "follow_ups"));
        return schema;
    }

    private static JsonObject quizSchema() {
        JsonObject item = objectSchema();
        item.add("properties", properties(
                property("source_question_id", scalar("integer")),
                property("subject", scalar("string")),
                property("type", enumString("MULTIPLE_CHOICE", "SHORT_ANSWER", "FILL_BLANK")),
                property("question", scalar("string")),
                property("options", arrayOf(scalar("string"))),
                property("correct_answer", scalar("string")),
                property("acceptable_answers", arrayOf(scalar("string"))),
                property("time_limit_seconds", scalar("integer")),
                property("case_sensitive", scalar("boolean")),
                property("numeric_tolerance", scalar("number")),
                property("explanation", scalar("string"))));
        item.add("required", strings("subject", "type", "question", "options", "correct_answer",
                "acceptable_answers", "time_limit_seconds", "case_sensitive", "numeric_tolerance", "explanation"));
        JsonObject schema = objectSchema();
        schema.add("properties", properties(property("questions", arrayOf(item))));
        schema.add("required", strings("questions"));
        return schema;
    }

    private static String chatSystemPrompt(ChatRequest.Context context) {
        ChatRequest.Context c = context == null ? new ChatRequest.Context() : context;
        return "You are Milo, an AI study mentor. Provide educational explanations, not hidden chain-of-thought. " +
                "Student level: " + safe(c.educationLevel, "unspecified") + ". " +
                "Subject: " + safe(c.subject, "general") + ". " +
                "Preferred language: " + safe(c.language, "en") + ". " +
                "Explanation style: " + safe(c.explanationStyle, "detailed") + ". " +
                "reply is a brief friendly introduction; final_answer is the clear answer; steps contain concise, " +
                "student-facing solution steps; key_concepts, common_mistakes, examples and follow_ups are optional learning aids. " +
                "Never include internal reasoning or private chain-of-thought.";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static JsonObject objectSchema() {
        JsonObject object = new JsonObject();
        object.addProperty("type", "object");
        return object;
    }

    private static JsonObject scalar(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    private static JsonObject enumString(String... values) {
        JsonObject object = scalar("string");
        object.add("enum", strings(values));
        return object;
    }

    private static JsonObject arrayOf(JsonObject item) {
        JsonObject array = new JsonObject();
        array.addProperty("type", "array");
        array.add("items", item);
        return array;
    }

    private static JsonObject properties(Property... values) {
        JsonObject object = new JsonObject();
        for (Property value : values) object.add(value.name, value.schema);
        return object;
    }

    private static Property property(String name, JsonObject schema) {
        return new Property(name, schema);
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    private static final class Property {
        final String name;
        final JsonObject schema;
        Property(String name, JsonObject schema) { this.name = name; this.schema = schema; }
    }

    private final class GeminiCall<T> implements Call<T> {
        private final JsonObject body;
        private final Class<T> responseClass;
        private final AtomicBoolean executed = new AtomicBoolean(false);
        private volatile boolean cancelled;
        private volatile okhttp3.Call activeCall;

        GeminiCall(JsonObject body, Class<T> responseClass) {
            this.body = body;
            this.responseClass = responseClass;
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            if (!executed.compareAndSet(false, true)) throw new IllegalStateException("Already executed");
            Request request = buildRequest(body);
            activeCall = http.newCall(request);
            try (Response response = activeCall.execute()) {
                return retrofit2.Response.success(parse(response, responseClass));
            }
        }

        @Override
        public void enqueue(@NonNull Callback<T> callback) {
            if (!executed.compareAndSet(false, true)) throw new IllegalStateException("Already executed");
            final Request request;
            try {
                request = buildRequest(body);
            } catch (AiServiceException e) {
                main.post(() -> callback.onFailure(this, e));
                return;
            }
            if (cancelled) {
                main.post(() -> callback.onFailure(this, new IOException("Request cancelled")));
                return;
            }
            activeCall = http.newCall(request);
            activeCall.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    main.post(() -> callback.onFailure(GeminiCall.this, e));
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) {
                    try (Response closed = response) {
                        T parsed = parse(closed, responseClass);
                        main.post(() -> callback.onResponse(GeminiCall.this,
                                retrofit2.Response.success(parsed)));
                    } catch (Exception e) {
                        main.post(() -> callback.onFailure(GeminiCall.this, e));
                    }
                }
            });
        }

        @Override public boolean isExecuted() { return executed.get(); }
        @Override public void cancel() { cancelled = true; if (activeCall != null) activeCall.cancel(); }
        @Override public boolean isCanceled() { return cancelled || (activeCall != null && activeCall.isCanceled()); }
        @Override public Call<T> clone() { return new GeminiCall<>(body.deepCopy(), responseClass); }
        @Override public Request request() {
            try { return buildRequest(body); }
            catch (AiServiceException e) {
                return new Request.Builder().url("https://generativelanguage.googleapis.com/").build();
            }
        }
        @Override public Timeout timeout() { return activeCall == null ? Timeout.NONE : activeCall.timeout(); }
    }

    private <T> T parse(Response response, Class<T> responseClass) throws IOException {
        String raw = response.body() == null ? "" : response.body().string();
        if (!response.isSuccessful()) {
            String message = extractErrorMessage(raw, response.code());
            String code = response.code() == 429 ? AiServiceException.RATE_LIMIT : AiServiceException.HTTP_ERROR;
            throw new AiServiceException(code, message, response.code(), null);
        }
        if (raw.trim().isEmpty()) {
            throw new AiServiceException(AiServiceException.EMPTY_RESPONSE, "Gemini returned an empty response.");
        }
        try {
            JsonObject root = gson.fromJson(raw, JsonObject.class);
            JsonArray candidates = root == null ? null : root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                String blockReason = "";
                if (root != null && root.has("promptFeedback")) {
                    JsonObject feedback = root.getAsJsonObject("promptFeedback");
                    if (feedback.has("blockReason")) blockReason = feedback.get("blockReason").getAsString();
                }
                if (!blockReason.isEmpty()) {
                    throw new AiServiceException(AiServiceException.SAFETY_BLOCK,
                            "Gemini blocked the request for safety: " + blockReason);
                }
                throw new AiServiceException(AiServiceException.EMPTY_RESPONSE,
                        "Gemini did not return a response candidate.");
            }
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (candidate.has("finishReason") && "SAFETY".equals(candidate.get("finishReason").getAsString())) {
                throw new AiServiceException(AiServiceException.SAFETY_BLOCK,
                        "Gemini blocked the response for safety.");
            }
            JsonArray parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
            if (parts == null || parts.size() == 0 || !parts.get(0).getAsJsonObject().has("text")) {
                throw new AiServiceException(AiServiceException.EMPTY_RESPONSE,
                        "Gemini returned no text content.");
            }
            String structuredText = parts.get(0).getAsJsonObject().get("text").getAsString();
            T parsed = gson.fromJson(structuredText, responseClass);
            if (parsed == null) {
                throw new AiServiceException(AiServiceException.PARSE_ERROR,
                        "Gemini returned invalid structured content.");
            }
            return parsed;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException(AiServiceException.PARSE_ERROR,
                    "Unable to parse Gemini's structured response.", 0, e);
        }
    }

    private String extractErrorMessage(String raw, int status) {
        try {
            JsonObject root = gson.fromJson(raw, JsonObject.class);
            JsonObject error = root == null ? null : root.getAsJsonObject("error");
            JsonElement message = error == null ? null : error.get("message");
            if (message != null) return "Gemini request failed: " + message.getAsString();
        } catch (Exception ignored) {
            // Return a neutral status-only error below; never expose raw response bodies.
        }
        return "Gemini request failed with HTTP " + status + ".";
    }
}
