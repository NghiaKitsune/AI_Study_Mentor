package com.studymentor.app.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuizDataSource {

    private static List<QuizQuestion> cache;

    private QuizDataSource() {}

    /**
     * Returns up to {@code count} randomly-ordered questions.
     * If {@code subject} is null/empty/"all", all subjects are included.
     */
    public static List<QuizQuestion> random(Context c, String subject, int count) {
        List<QuizQuestion> all = loadAll(c);
        List<QuizQuestion> filtered = new ArrayList<>();
        for (QuizQuestion q : all) {
            if (subject == null || subject.isEmpty() || "all".equals(subject)
                    || subject.equals(q.subject)) {
                filtered.add(q);
            }
        }
        if (filtered.isEmpty()) filtered = new ArrayList<>(all);
        Collections.shuffle(filtered);
        int take = Math.min(count, filtered.size());
        return new ArrayList<>(filtered.subList(0, take));
    }

    private static List<QuizQuestion> loadAll(Context c) {
        if (cache != null) return cache;
        try {
            InputStreamReader reader =
                    new InputStreamReader(c.getAssets().open("quiz_questions.json"));
            Type listType = new TypeToken<List<QuizQuestion>>() {}.getType();
            cache = new Gson().fromJson(reader, listType);
            if (cache == null) cache = new ArrayList<>();
        } catch (Exception e) {
            android.util.Log.e("QuizDataSource", "parse failed", e);
            cache = new ArrayList<>();
        }
        return cache;
    }
}
