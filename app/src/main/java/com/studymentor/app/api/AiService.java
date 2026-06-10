package com.studymentor.app.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Real Retrofit service. Built by {@link ApiClient} when
 * {@code BuildConfig.USE_MOCK_AI} is false.
 */
public interface AiService {

    @POST("api/chat")
    Call<ChatResponse> chat(@Body ChatRequest request);
}
