package com.studymentor.app.api;

import com.studymentor.app.BuildConfig;

/**
 * Single entry-point for getting an {@link AiService}.
 * <p>
 * Reads two BuildConfig fields wired in {@code app/build.gradle}:
 * <ul>
 *   <li>{@code USE_MOCK_AI} — true in debug builds; returns {@link MockAiService}.</li>
 *   <li>{@code API_BASE_URL} — used when USE_MOCK_AI is false.</li>
 * </ul>
 */
public final class ApiClient {

    private static volatile AiService instance;

    private ApiClient() {}

    public static AiService get() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = build();
                }
            }
        }
        return instance;
    }

    private static AiService build() {
        if (BuildConfig.USE_MOCK_AI) {
            return new MockAiService();
        }
        return new GroqAiService();
    }
}
