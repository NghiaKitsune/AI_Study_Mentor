package com.studymentor.app.api;

import retrofit2.Call;

public interface AiService {
    Call<ChatResponse> chat(ChatRequest request);
    Call<QuizGenerationResponse> generateQuiz(QuizGenerationRequest request);
}
