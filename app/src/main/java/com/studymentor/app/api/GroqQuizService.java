package com.studymentor.app.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.studymentor.app.BuildConfig;
import com.studymentor.app.data.QuizQuestion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Calls Groq to generate MCQ quiz questions for a given subject + user level.
 * Returns List<QuizQuestion> (same POJO as QuizDataSource) so QuizActivity
 * can use AI questions and static JSON interchangeably.
 */
public class GroqQuizService {

    public interface Callback {
        void onSuccess(List<QuizQuestion> questions);
        void onError(String message);
    }

    private static final String TAG      = "QuizAI";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    public GroqQuizService() {
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public void generate(String subject, int levelNumber, int count, Callback cb) {
        String bodyJson = buildBody(subject, levelNumber, count);
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
                    List<QuizQuestion> parsed = parseResponse(raw, subject);
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

    private String buildBody(String subject, int levelNumber, int count) {
        String levelName = levelName(levelNumber);
        String prompt = "Generate " + count + " multiple-choice quiz questions for subject: "
                + (subject != null && !subject.isEmpty() ? subject : "general")
                + " at difficulty level: " + levelName + ".";
        String system = buildSystemPrompt(subject, levelName, count);
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0.8,"
            + "\"response_format\":{\"type\":\"json_object\"},"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":" + gson.toJson(system) + "},"
            +   "{\"role\":\"user\",\"content\":" + gson.toJson(prompt) + "}"
            + "]}";
    }

    private static String levelName(int levelNum) {
        switch (levelNum) {
            case 5:  return "Master";
            case 4:  return "Expert";
            case 3:  return "Scholar";
            case 2:  return "Explorer";
            default: return "Beginner";
        }
    }

    private static String buildSystemPrompt(String subject, String level, int count) {
        String subjectStr = (subject != null && !subject.isEmpty()) ? subject : "general topics";
        return "You are Milo, an AI study mentor. Generate exactly " + count
            + " multiple-choice quiz questions for a " + level
            + "-level student studying " + subjectStr + ".\n"
            + "IMPORTANT: Respond ONLY with a valid JSON object, nothing else.\n"
            + "Use this exact schema:\n"
            + "{\n"
            + "  \"questions\": [\n"
            + "    {\n"
            + "      \"question\": \"Question text\",\n"
            + "      \"subject\": \"" + (subject != null ? subject : "general") + "\",\n"
            + "      \"subjectTag\": \"SUBJECT · TOPIC · MULTIPLE CHOICE\",\n"
            + "      \"options\": [\"A. option\", \"B. option\", \"C. option\", \"D. option\"],\n"
            + "      \"correctIndex\": 0,\n"
            + "      \"explanation\": \"Why this answer is correct\"\n"
            + "    }\n"
            + "  ]\n"
            + "}\n"
            + "Rules:\n"
            + "- Generate exactly " + count + " questions\n"
            + "- Each question must have exactly 4 options\n"
            + "- correctIndex is 0-based (0=A, 1=B, 2=C, 3=D)\n"
            + "- Difficulty should match " + level + " level\n"
            + "- Make questions diverse — cover different concepts within the subject\n"
            + "- explanation: 1-2 sentences explaining the correct answer\n"
            + "- English only, be concise and student-friendly";
    }

    private List<QuizQuestion> parseResponse(String raw, String subject) throws Exception {
        com.google.gson.JsonObject root = gson.fromJson(raw, com.google.gson.JsonObject.class);
        if (root.has("error")) {
            String msg = root.getAsJsonObject("error").get("message").getAsString();
            throw new Exception("API error: " + msg);
        }
        String content = root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        Log.d(TAG, "Content: " + content.substring(0, Math.min(500, content.length())));

        com.google.gson.JsonObject contentObj = gson.fromJson(content, com.google.gson.JsonObject.class);
        com.google.gson.JsonArray questionsArr = contentObj.getAsJsonArray("questions");
        if (questionsArr == null) throw new Exception("No 'questions' array in response");

        List<QuizQuestion> list = new ArrayList<>();
        for (int i = 0; i < questionsArr.size(); i++) {
            QuizQuestion q = gson.fromJson(questionsArr.get(i), QuizQuestion.class);
            if (q != null && q.question != null && q.options != null && q.options.length == 4) {
                if (q.subject == null || q.subject.isEmpty())
                    q.subject = (subject != null ? subject : "general");
                if (q.subjectTag == null || q.subjectTag.isEmpty())
                    q.subjectTag = q.subject.toUpperCase() + " · MULTIPLE CHOICE";
                list.add(q);
            }
        }
        if (list.isEmpty()) throw new Exception("No valid questions parsed from response");
        return list;
    }
}
