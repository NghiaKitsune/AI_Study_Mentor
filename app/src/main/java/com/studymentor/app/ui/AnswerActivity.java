package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.StepAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * UC4 — Answer detail.
 *   Reads a Question from Room. Renders prompt + final answer + step-by-step list.
 *   Steps come from EXTRA_STEPS_JSON (passed by Chat) — falls back to a friendly hint when missing.
 *   Bookmark toggles persist to Room. Share emits a standard ACTION_SEND intent.
 */
public class AnswerActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID  = "extra_question_id";
    public static final String EXTRA_STEPS_JSON   = "extra_steps_json";
    public static final String EXTRA_MISTAKES_JSON = "extra_mistakes_json";

    private Question question;
    private MaterialButton btnBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        long qid = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        question = qid > 0 ? StudyMentorApp.get().db().questionDao().byId(qid) : null;

        ((TextView) findViewById(R.id.text_question))
                .setText(question != null ? question.prompt : "—");
        ((TextView) findViewById(R.id.text_final_answer))
                .setText(question != null && question.answer != null
                        ? extractFinalAnswer(question.answer)
                        : getString(R.string.open_in_chat_hint));

        bindSteps();
        bindMistakes();
        bindFollowUps();
        bindBookmark();
        bindShare();
    }

    /** Picks the last sentence-like fragment as the "final answer" tag. */
    private String extractFinalAnswer(String full) {
        if (full == null) return "—";
        // Look for "= X" pattern
        int eq = full.lastIndexOf('=');
        if (eq != -1 && eq < full.length() - 1) {
            String tail = full.substring(eq).trim();
            if (tail.length() < 40) return tail;
        }
        return full.length() > 80 ? full.substring(0, 80) + "…" : full;
    }

    private void bindSteps() {
        RecyclerView rv = findViewById(R.id.rv_steps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);

        String json = getIntent().getStringExtra(EXTRA_STEPS_JSON);
        List<ChatResponse.Step> steps = parseSteps(json);
        rv.setAdapter(new StepAdapter(steps));
    }

    private List<ChatResponse.Step> parseSteps(String json) {
        if (json == null || json.isEmpty()) return fallbackSteps();
        try {
            Type listType = new TypeToken<List<ChatResponse.Step>>(){}.getType();
            List<ChatResponse.Step> parsed = new Gson().fromJson(json, listType);
            return parsed != null && !parsed.isEmpty() ? parsed : fallbackSteps();
        } catch (Exception e) {
            return fallbackSteps();
        }
    }

    /** Friendly placeholder shown when there are no real steps yet. */
    private List<ChatResponse.Step> fallbackSteps() {
        ChatResponse.Step s = new ChatResponse.Step();
        s.index = 1;
        s.title = "Full answer";
        s.body  = question != null && question.answer != null
                ? question.answer
                : getString(R.string.open_in_chat_hint);
        List<ChatResponse.Step> list = new ArrayList<>();
        list.add(s);
        return list;
    }

    private void bindFollowUps() {
        String prompt  = question != null ? question.prompt  : "";
        String subject = question != null ? question.subject : "general";

        findViewById(R.id.chip_follow_simpler).setOnClickListener(v ->
                openChat("Explain this more simply: " + prompt));

        findViewById(R.id.chip_follow_another).setOnClickListener(v ->
                openChat("Show me another method to solve: " + prompt));

        findViewById(R.id.chip_follow_practice).setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity.class);
            if (subject != null && !subject.equals("general"))
                i.putExtra(QuizActivity.EXTRA_SUBJECT, subject);
            startActivity(i);
        });
    }

    private void openChat(String prefill) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_PROMPT, prefill);
        startActivity(i);
    }

    private void bindMistakes() {
        String json = getIntent().getStringExtra(EXTRA_MISTAKES_JSON);
        if (json == null || json.isEmpty()) return;
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> mistakes = new Gson().fromJson(json, listType);
            if (mistakes == null || mistakes.isEmpty()) return;
            if (mistakes.size() >= 1) {
                TextView tv = findViewById(R.id.text_mistake_1);
                if (tv != null) tv.setText(mistakes.get(0));
            }
            if (mistakes.size() >= 2) {
                TextView tv = findViewById(R.id.text_mistake_2);
                if (tv != null) tv.setText(mistakes.get(1));
            }
        } catch (Exception ignored) {}
    }

    private void bindBookmark() {
        btnBookmark = findViewById(R.id.btn_bookmark);
        refreshBookmarkIcon();
        btnBookmark.setOnClickListener(v -> {
            if (question == null) return;
            question.bookmarked = !question.bookmarked;
            StudyMentorApp.get().db().questionDao().update(question);
            refreshBookmarkIcon();
        });
    }

    private void refreshBookmarkIcon() {
        boolean on = question != null && question.bookmarked;
        btnBookmark.setIconResource(on ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        btnBookmark.setIconTintResource(on ? R.color.brand_primary : R.color.text_primary);
    }

    private void bindShare() {
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            if (question == null) return;
            String body = "Q: " + question.prompt + "\n\nA: " +
                    (question.answer != null ? question.answer : "—") +
                    "\n\n— shared from AI Study Mentor";
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            share.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(share, getString(R.string.action_share)));
        });
    }
}
