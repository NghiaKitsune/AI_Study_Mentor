package com.studymentor.app.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {}

    public static String normalizeQuestion(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public static String normalizeAnswer(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[.,;:!?]+$", "")
                .toLowerCase(Locale.ROOT);
    }
}

