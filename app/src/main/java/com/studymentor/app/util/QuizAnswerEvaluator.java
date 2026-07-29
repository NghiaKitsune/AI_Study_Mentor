package com.studymentor.app.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studymentor.app.data.QuizQuestionEntity;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure answer evaluator shared by repositories and unit tests. */
public final class QuizAnswerEvaluator {
    private static final Gson GSON = new Gson();

    private QuizAnswerEvaluator() {}

    public static Evaluation evaluate(QuizQuestionEntity question, String userAnswer) {
        if (question == null) return new Evaluation(false, "");
        String rawUser = userAnswer == null ? "" : userAnswer.trim();
        List<String> accepted = acceptableAnswers(question);
        // Alternatives extend the canonical answer; they must never replace it.
        accepted.add(0, question.correctAnswer);

        for (String expected : accepted) {
            if (matches(question, rawUser, expected)) {
                return new Evaluation(true, expected == null ? "" : expected);
            }
        }
        return new Evaluation(false, question.correctAnswer);
    }

    private static boolean matches(QuizQuestionEntity question, String actual, String expected) {
        if (expected == null) return false;
        String cleanExpected = expected.trim();
        BigDecimal actualNumber = number(actual);
        BigDecimal expectedNumber = number(cleanExpected);
        if (actualNumber != null && expectedNumber != null) {
            BigDecimal difference = actualNumber.subtract(expectedNumber).abs();
            return difference.compareTo(BigDecimal.valueOf(
                    Math.max(0d, question.numericTolerance))) <= 0;
        }
        return normalize(actual, question.caseSensitive)
                .equals(normalize(cleanExpected, question.caseSensitive));
    }

    private static List<String> acceptableAnswers(QuizQuestionEntity question) {
        if (question.acceptableAnswersJson == null
                || question.acceptableAnswersJson.trim().isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> parsed = GSON.fromJson(question.acceptableAnswersJson, type);
            return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static String normalize(String value, boolean caseSensitive) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return caseSensitive ? normalized : normalized.toLowerCase(Locale.ROOT);
    }

    private static BigDecimal number(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return new BigDecimal(value.trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return null; }
    }

    public static final class Evaluation {
        public final boolean correct;
        public final String matchedAnswer;

        Evaluation(boolean correct, String matchedAnswer) {
            this.correct = correct;
            this.matchedAnswer = matchedAnswer;
        }
    }
}
