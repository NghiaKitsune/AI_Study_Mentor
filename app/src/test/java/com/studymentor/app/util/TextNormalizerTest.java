package com.studymentor.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextNormalizerTest {
    @Test
    public void questionNormalizationIsStable() {
        assertEquals("solve x + 1", TextNormalizer.normalizeQuestion("  Solve   X + 1  "));
        assertEquals("", TextNormalizer.normalizeQuestion(null));
    }

    @Test
    public void answerNormalizationIgnoresTrailingPunctuation() {
        assertEquals("paris", TextNormalizer.normalizeAnswer(" Paris!!! "));
    }
}

