package com.studymentor.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {
    public String reply = "";

    @SerializedName("final_answer")
    public String finalAnswer = "";

    public List<Step> steps = new ArrayList<>();

    @SerializedName("key_concepts")
    public List<String> keyConcepts = new ArrayList<>();

    @SerializedName("common_mistakes")
    public List<String> commonMistakes = new ArrayList<>();

    @SerializedName("alternative_approach")
    public String alternativeApproach = "";

    public List<String> examples = new ArrayList<>();

    @SerializedName("follow_ups")
    public List<String> followUps = new ArrayList<>();

    // Deprecated aliases retained for source compatibility. The snake_case aliases that
    // overlap @SerializedName fields must stay transient so Gson sees each JSON name once.
    @Deprecated public String request_id;
    @Deprecated public Long conversation_id;
    @Deprecated public transient String final_answer;
    @Deprecated public transient List<String> follow_ups;
    @Deprecated public Integer tokens_used;
    @Deprecated public ErrorInfo error;

    public static class Step {
        public int index;
        public String title = "";
        public String body = "";
    }

    public static class ErrorInfo {
        public String code;
        public String message;
    }
}
