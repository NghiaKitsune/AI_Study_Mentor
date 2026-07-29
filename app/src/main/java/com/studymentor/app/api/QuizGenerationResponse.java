package com.studymentor.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class QuizGenerationResponse {
    public List<Item> questions = new ArrayList<>();

    public static class Item {
        @SerializedName("source_question_id")
        public Long sourceQuestionId;
        public String subject = "general";
        public String type = "MULTIPLE_CHOICE";
        public String question = "";
        public List<String> options = new ArrayList<>();
        @SerializedName("correct_answer")
        public String correctAnswer = "";
        @SerializedName("acceptable_answers")
        public List<String> acceptableAnswers = new ArrayList<>();
        @SerializedName("time_limit_seconds")
        public int timeLimitSeconds = 24;
        @SerializedName("case_sensitive")
        public boolean caseSensitive;
        @SerializedName("numeric_tolerance")
        public double numericTolerance;
        public String explanation = "";
    }
}
