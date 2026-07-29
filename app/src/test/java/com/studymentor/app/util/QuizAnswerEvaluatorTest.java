package com.studymentor.app.util;

import com.studymentor.app.data.QuizQuestionEntity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuizAnswerEvaluatorTest {
    @Test public void multipleChoiceIgnoresCaseAndOuterWhitespace() {
        QuizQuestionEntity question = question("MULTIPLE_CHOICE", "Photosynthesis");
        assertTrue(QuizAnswerEvaluator.evaluate(question, " photosynthesis ").correct);
    }

    @Test public void acceptsConfiguredAlternativeAnswer() {
        QuizQuestionEntity question = question("SHORT_ANSWER", "United Kingdom");
        question.acceptableAnswersJson = "[\"UK\",\"U.K.\"]";
        assertTrue(QuizAnswerEvaluator.evaluate(question, "uk").correct);
    }

    @Test public void alternativesDoNotReplaceCanonicalAnswer() {
        QuizQuestionEntity question = question("SHORT_ANSWER", "Room");
        question.acceptableAnswersJson = "[\"Android Room\",\"Room Database\"]";
        assertTrue(QuizAnswerEvaluator.evaluate(question, "room").correct);
        assertTrue(QuizAnswerEvaluator.evaluate(question, "Android Room").correct);
    }

    @Test public void respectsCaseSensitiveQuestion() {
        QuizQuestionEntity question = question("FILL_BLANK", "Java");
        question.caseSensitive = true;
        assertFalse(QuizAnswerEvaluator.evaluate(question, "java").correct);
    }

    @Test public void acceptsNumericValueWithinTolerance() {
        QuizQuestionEntity question = question("SHORT_ANSWER", "3.14");
        question.numericTolerance = 0.02;
        assertTrue(QuizAnswerEvaluator.evaluate(question, "3.159").correct);
        assertFalse(QuizAnswerEvaluator.evaluate(question, "3.17").correct);
    }

    private static QuizQuestionEntity question(String type, String answer) {
        QuizQuestionEntity question = new QuizQuestionEntity();
        question.type = type;
        question.correctAnswer = answer;
        return question;
    }
}
