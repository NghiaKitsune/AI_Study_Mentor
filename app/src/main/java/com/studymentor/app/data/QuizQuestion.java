package com.studymentor.app.data;

public class QuizQuestion {
    public String question;
    public String subject;      // "math" | "science" | "code" | "history" | "general"
    public String subjectTag;   // e.g. "MATH · ALGEBRA · MULTIPLE CHOICE"
    public String[] options;    // always 4 options
    public int correctIndex;    // 0–3
    public String explanation;
}
