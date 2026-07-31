package com.studymentor.app.api;

/** Production entry point: all generated study content comes from Gemini. */
public final class ApiClient {
    private static volatile AiService instance;

    private ApiClient() {}

    public static AiService get() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) instance = new GeminiAiService();
            }
        }
        return instance;
    }

    public static AiService service() {
        return get();
    }

    static void setForTests(AiService service) {
        instance = service;
    }
}
