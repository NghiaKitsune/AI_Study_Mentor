package com.studymentor.app.api;

import com.studymentor.app.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(log)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AiService.class);
    }
}
