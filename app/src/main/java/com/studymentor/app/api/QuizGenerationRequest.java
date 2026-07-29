package com.studymentor.app.api;

import java.util.ArrayList;
import java.util.List;

public class QuizGenerationRequest {
    public String subject = "general";
    public String educationLevel = "";
    public String language = "en";
    public int count = 5;
    public List<SourceItem> sources = new ArrayList<>();

    public static class SourceItem {
        public long questionId;
        public String question = "";
        public String answer = "";
    }
}

