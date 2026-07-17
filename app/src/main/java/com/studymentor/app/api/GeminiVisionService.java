package com.studymentor.app.api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
 * Phase 6A — real OCR via Gemini 1.5 Flash Vision.
 * Falls back to MockOcrService when GEMINI_API_KEY is empty or the call fails.
 */
public class GeminiVisionService {

    private static final String TAG = "GeminiOCR";
    private static final String URL  =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson GSON = new Gson();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GeminiVisionService() {}

    /**
     * Reads text from {@code imageUri} via Gemini Vision.
     * Falls back to {@link MockOcrService} when the API key is absent or the call fails.
     * Callback is always delivered on the main thread.
     */
    public static void recognize(Context ctx, Uri imageUri, MockOcrService.Listener listener) {
        String key = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(key)) {
            Log.w(TAG, "GEMINI_API_KEY empty — falling back to mock");
            MockOcrService.recognize(imageUri, listener);
            return;
        }

        new Thread(() -> {
            try {
                byte[] bytes = readBytes(ctx, imageUri);
                if (bytes == null || bytes.length == 0) throw new IOException("Empty image");

                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                String mime   = resolveMime(ctx, imageUri);

                Request req = new Request.Builder()
                        .url(URL + "?key=" + key)
                        .post(RequestBody.create(buildJson(base64, mime), JSON))
                        .build();

                try (Response resp = HTTP.newCall(req).execute()) {
                    String raw = resp.body() != null ? resp.body().string() : "";
                    Log.d(TAG, "raw=" + raw);
                    MockOcrService.Result result = parse(raw);
                    MAIN.post(() -> listener.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Vision failed: " + e.getMessage() + " — using mock fallback", e);
                MAIN.post(() -> MockOcrService.recognize(imageUri, listener));
            }
        }, "GeminiVision").start();
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
        String prompt = GSON.toJson(
            "Read every piece of text visible in this image exactly as written. "
          + "Reply ONLY with a JSON object — no markdown fences, no extra text:\n"
          + "{\"text\":\"<all recognized text>\","
          + "\"subject\":\"math|code|science|history|general\","
          + "\"language\":\"en|vi\"}"
        );
        return "{"
            + "\"contents\":[{\"parts\":["
            +   "{\"inlineData\":{\"mimeType\":\"" + mime + "\",\"data\":\"" + base64 + "\"}},"
            +   "{\"text\":" + prompt + "}"
            + "]}]}";
    }

    private static MockOcrService.Result parse(String raw) throws Exception {
        JsonObject root = GSON.fromJson(raw, JsonObject.class);
        if (root.has("error")) {
            throw new Exception(root.getAsJsonObject("error").get("message").getAsString());
        }

        String content = root.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString().trim();

        // Strip markdown fences if model adds them
        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }

        JsonObject j = GSON.fromJson(content, JsonObject.class);
        String text    = j.has("text")     ? j.get("text").getAsString()     : content;
        String subject = j.has("subject")  ? j.get("subject").getAsString()  : "general";
        String lang    = j.has("language") ? j.get("language").getAsString() : "en";

        if (!subject.matches("math|code|science|history|general")) subject = "general";
        String style = (subject.equals("math") || subject.equals("science")) ? "step-by-step" : "in-depth";

        return new MockOcrService.Result(text, 90, subject, style, lang);
    }
}
