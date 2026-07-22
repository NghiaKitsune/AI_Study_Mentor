package com.studymentor.app.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.studymentor.app.BuildConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Calls Groq to generate 4-tab educational content for AnswerTabbedActivity.
 * Returns TabbedResponse: solution steps, concept, 2 practice MCQs, 3 pitfalls.
 */
public class GroqTabbedService {

    public interface Callback {
        void onSuccess(TabbedResponse response);
        void onError(String message);
    }

    private static final String TAG      = "TabbedAI";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    public GroqTabbedService() {
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public void generate(String question, String subject, String stepsContext, Callback cb) {
        String bodyJson = buildBody(question, subject, stepsContext);
        Request req = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(bodyJson, JSON_TYPE))
                .build();

        http.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "HTTP failure: " + e.getMessage(), e);
                main.post(() -> cb.onError(e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response r) {
                try {
                    String raw = r.body() != null ? r.body().string() : "";
                    Log.d(TAG, "Raw: " + raw.substring(0, Math.min(300, raw.length())));
                    TabbedResponse parsed = parseResponse(raw);
                    main.post(() -> cb.onSuccess(parsed));
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage(), e);
                    main.post(() -> cb.onError(e.getMessage()));
                } finally {
                    r.close();
                }
            }
        });
    }

    private String buildBody(String question, String subject, String steps) {
        String prompt = "Question: " + question + "\n"
                + "Subject: " + (subject != null ? subject : "general") + "\n"
                + (steps != null && !steps.isEmpty() ? "Solution steps already computed: " + steps + "\n" : "");
        String system = buildSystemPrompt();
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0.6,"
            + "\"response_format\":{\"type\":\"json_object\"},"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":" + gson.toJson(system) + "},"
            +   "{\"role\":\"user\",\"content\":" + gson.toJson(prompt) + "}"
            + "]}";
    }

    private static String buildSystemPrompt() {
        return "You are Milo, an AI study mentor. Given a student's question, generate "
            + "structured educational content for 4 learning tabs.\n"
            + "IMPORTANT: Respond ONLY with a valid JSON object, nothing else.\n"
            + "Use this exact schema:\n"
            + "{\n"
            + "  \"solution\": [\n"
            + "    {\"index\": 1, \"title\": \"Step title\", \"body\": \"Step explanation\"}\n"
            + "  ],\n"
            + "  \"concept\": {\n"
            + "    \"formula\": \"Key formula or concept (e.g. F = ma)\",\n"
            + "    \"explanation\": \"Clear 2-3 sentence explanation\",\n"
            + "    \"funFact\": \"An interesting related fact\"\n"
            + "  },\n"
            + "  \"practice\": [\n"
            + "    {\n"
            + "      \"question\": \"MCQ question text\",\n"
            + "      \"options\": [\"A. option\", \"B. option\", \"C. option\", \"D. option\"],\n"
            + "      \"correctIndex\": 0,\n"
            + "      \"hint\": \"Short hint\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"pitfalls\": [\"Mistake 1\", \"Mistake 2\", \"Mistake 3\"]\n"
            + "}\n"
            + "Rules:\n"
            + "- solution: 2-4 steps that clearly explain how to solve the problem\n"
            + "- concept: focus on the key formula or underlying theory\n"
            + "- practice: exactly 2 MCQ questions related to this topic; correctIndex is 0-based\n"
            + "- pitfalls: exactly 3 common mistakes students make on this topic\n"
            + "- Use English only, be concise and student-friendly";
    }

    private TabbedResponse parseResponse(String raw) throws Exception {
        com.google.gson.JsonObject root = gson.fromJson(raw, com.google.gson.JsonObject.class);
        if (root.has("error")) {
            String msg = root.getAsJsonObject("error").get("message").getAsString();
            throw new Exception("API error: " + msg);
        }
        String content = root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        Log.d(TAG, "Content: " + content.substring(0, Math.min(400, content.length())));
        TabbedResponse resp = gson.fromJson(content, TabbedResponse.class);
        if (resp == null) throw new Exception("Null parse result");
        if (resp.solution  == null) resp.solution  = new ArrayList<>();
        if (resp.practice  == null) resp.practice  = new ArrayList<>();
        if (resp.pitfalls  == null) resp.pitfalls  = new ArrayList<>();
        if (resp.concept   == null) {
            resp.concept = new TabbedResponse.Concept();
            resp.concept.formula = "";
            resp.concept.explanation = "";
            resp.concept.funFact = "";
        }
        return resp;
    }
}
