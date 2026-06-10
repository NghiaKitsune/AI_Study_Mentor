package com.studymentor.app.api;

import java.util.List;

/**
 * Mirrors {@code api-contract/chat-response.schema.json}.
 */
public class ChatResponse {

    public String request_id;
    public Long conversation_id;
    public String reply;
    public String final_answer;
    public List<Step> steps;
    public List<String> follow_ups;
    public Integer tokens_used;
    public ErrorInfo error;

    public static class Step {
        public int index;
        public String title;
        public String body;
    }

    public static class ErrorInfo {
        public String code;     // "rate_limit" | "model_error" | "invalid_input"
        public String message;
    }
}
