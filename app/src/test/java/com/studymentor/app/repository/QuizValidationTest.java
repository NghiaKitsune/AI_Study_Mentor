package com.studymentor.app.repository;

import com.studymentor.app.api.QuizGenerationResponse;
import com.studymentor.app.data.QuizQuestionEntity;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QuizValidationTest {
    @Test
    public void acceptsValidMultipleChoiceQuestion() {
        QuizGenerationResponse.Item item = item();
        item.type = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE;
        item.options = Arrays.asList("2", "3", "4", "5");
        item.correctAnswer = "4";
        assertNull(QuizRepository.validationError(item));
    }

    @Test
    public void rejectsMultipleChoiceWithoutExactlyFourOptions() {
        QuizGenerationResponse.Item item = item();
        item.type = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE;
        item.options = Arrays.asList("2", "3", "4");
        item.correctAnswer = "4";
        assertEquals("invalid options", QuizRepository.validationError(item));
    }

    @Test
    public void rejectsAnswerOutsideOptions() {
        QuizGenerationResponse.Item item = item();
        item.type = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE;
        item.options = Arrays.asList("2", "3", "4", "5");
        item.correctAnswer = "6";
        assertEquals("correct answer is not an option", QuizRepository.validationError(item));
    }

    @Test
    public void acceptsShortAnswerWithoutOptions() {
        QuizGenerationResponse.Item item = item();
        item.type = QuizQuestionEntity.TYPE_SHORT_ANSWER;
        item.correctAnswer = "photosynthesis";
        assertNull(QuizRepository.validationError(item));
    }

    private static QuizGenerationResponse.Item item() {
        QuizGenerationResponse.Item item = new QuizGenerationResponse.Item();
        item.question = "What is the answer?";
        item.explanation = "Review the concept.";
        return item;
    }
}

