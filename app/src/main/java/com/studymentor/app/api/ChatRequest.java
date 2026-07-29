package com.studymentor.app.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatRequest {
    public String requestId = UUID.randomUUID().toString();
    public String message = "";
    public Context context = new Context();
    public List<ConversationMessage> history = new ArrayList<>();

    // Deprecated wire aliases retained only while old screens are migrated.
    @Deprecated public String request_id;
    @Deprecated public Long conversation_id;

    public ChatRequest() {}

    public ChatRequest(String requestId, String message) {
        this.requestId = requestId == null || requestId.trim().isEmpty()
                ? UUID.randomUUID().toString() : requestId;
        this.request_id = this.requestId;
        this.message = message == null ? "" : message;
    }

    public ChatRequest(String prompt, long conversationId) {
        this(UUID.randomUUID().toString(), prompt);
        this.conversation_id = conversationId > 0 ? conversationId : null;
    }

    public static class Context {
        public String educationLevel = "";
        public String subject = "general";
        public String language = "en";
        public String explanationStyle = "detailed";

        @Deprecated public String user_level;
        @Deprecated public String locale;
    }

    public static class ConversationMessage {
        public String role;
        public String text;

        public ConversationMessage(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }
}
