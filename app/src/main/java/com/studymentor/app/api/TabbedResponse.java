package com.studymentor.app.api;

import java.util.List;

public class TabbedResponse {
    public List<SolutionStep>    solution;
    public Concept               concept;
    public List<PracticeQuestion> practice;
    public List<String>          pitfalls;

    public static class SolutionStep {
        public int    index;
        public String title;
        public String body;
    }

    public static class Concept {
        public String formula;
        public String explanation;
        public String funFact;
    }

    public static class PracticeQuestion {
        public String   question;
        public String[] options;
        public int      correctIndex;
        public String   hint;
    }
}
