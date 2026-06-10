package com.studymentor.app.api;

/**
 * Mirrors {@code api-contract/chat-request.schema.json}.
 *
 * For the MVP, {@link ChatActivity} constructs requests with a
 * convenience constructor — the full schema is here for the day we
 * wire the real backend.
 */
public class ChatRequest {

    public String request_id;
    public Long conversation_id;
    public String message;
    public Context context;

    public ChatRequest() {}

    /**
     * Convenience used by {@code ChatActivity}: wraps a prompt + the
     * current conversation id, generates a request id, and leaves
     * {@link #context} for the caller to attach if it wants.
     */
    public ChatRequest(String prompt, long conversationId) {
        this.request_id = java.util.UUID.randomUUID().toString();
        this.message = prompt;
        this.conversation_id = conversationId > 0 ? conversationId : null;
    }

    public static class Context {
        public String user_level;  // "middle-school" | "high-school" | "college" | "self-taught"
        public String subject;     // "math" | "science" | ...
        public String locale;
    }
}
