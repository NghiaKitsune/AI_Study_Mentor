package com.studymentor.app.api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.studymentor.app.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OCR via Groq Vision (llama-3.2-11b-vision-preview).
 * Uses the same GROQ_API_KEY already in local.properties.
 * Falls back to MockOcrService on any error.
 */
public class GroqVisionService {

    private static final String TAG   = "GroqVision";
    private static final String URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build();

    private static final Gson GSON = new Gson();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GroqVisionService() {}

    /**
     * Reads text from {@code imageUri} via Groq Vision.
     * Falls back to {@link MockOcrService} when the call fails.
     * Callback always delivered on the main thread.
     */
    public static void recognize(Context ctx, Uri imageUri, MockOcrService.Listener listener) {
        new Thread(() -> {
            try {
                byte[] bytes = readBytes(ctx, imageUri);
                if (bytes == null || bytes.length == 0) throw new IOException("Empty image");

                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String mime   = resolveMime(ctx, imageUri);

                Request req = new Request.Builder()
                        .url(URL)
                        .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(buildJson(base64, mime), JSON_TYPE))
                        .build();

                try (Response resp = HTTP.newCall(req).execute()) {
                    String raw = resp.body() != null ? resp.body().string() : "";
                    Log.d(TAG, "raw=" + raw.substring(0, Math.min(raw.length(), 300)));
                    MockOcrService.Result result = parse(raw);
                    MAIN.post(() -> listener.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Vision failed: " + e.getMessage() + " — using mock fallback", e);
                MAIN.post(() -> MockOcrService.recognize(imageUri, listener));
            }
        }, "GroqVision").start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static byte[] readBytes(Context ctx, Uri uri) {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (is == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "readBytes: " + e.getMessage());
            return null;
        }
    }

    private static String resolveMime(Context ctx, Uri uri) {
        String t = ctx.getContentResolver().getType(uri);
        return (t != null && t.startsWith("image/")) ? t : "image/jpeg";
    }

    private static String buildJson(String base64, String mime) {
        String prompt = "Read every piece of text visible in this image exactly as written. "
                + "Reply ONLY with a JSON object — no markdown fences, no extra text:\n"
                + "{\"text\":\"<all recognized text>\","
                + "\"subject\":\"math|code|science|history|general\","
                + "\"language\":\"en|vi\"}";

        // Groq vision uses OpenAI-compatible multipart content format
        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"temperature\":0,"
            + "\"messages\":[{"
            +   "\"role\":\"user\","
            +   "\"content\":["
            +     "{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:" + mime + ";base64," + base64 + "\"}},"
            +     "{\"type\":\"text\",\"text\":" + GSON.toJson(prompt) + "}"
            +   "]"
            + "}]}";
    }

    private static MockOcrService.Result parse(String raw) throws Exception {
        JsonObject root = GSON.fromJson(raw, JsonObject.class);
        if (root.has("error")) {
            throw new Exception(root.getAsJsonObject("error").get("message").getAsString());
        }

        String content = root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();

        // Strip markdown fences if model adds them
        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }

        // Model may return plain text instead of JSON when no text is found
        String text, subject, lang;
        try {
            JsonObject j = GSON.fromJson(content, JsonObject.class);
            text    = j.has("text")     ? j.get("text").getAsString()     : content;
            subject = j.has("subject")  ? j.get("subject").getAsString()  : "general";
            lang    = j.has("language") ? j.get("language").getAsString() : "en";
        } catch (Exception e) {
            // Model returned plain text — use it directly
            text = content;
            subject = "general";
            lang = "en";
        }

        if (!subject.matches("math|code|science|history|general")) subject = "general";
        String style = (subject.equals("math") || subject.equals("science")) ? "step-by-step" : "in-depth";

        return new MockOcrService.Result(text, 90, subject, style, lang);
    }
}
