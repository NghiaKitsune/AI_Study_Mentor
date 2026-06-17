package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.studymentor.app.R;
import com.studymentor.app.api.ApiClient;
import com.studymentor.app.api.ChatRequest;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.data.Message;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.MessageAdapter;
import com.studymentor.app.StudyMentorApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * UC2-3 — Chat (core feature).
 * Holds an in-memory message list, displays them in a multi-viewtype RecyclerView,
 * and POSTs each user turn to the AI service (mock by default).
 *
 * Extras:
 *   EXTRA_PROMPT  — optional prefill in the composer
 *   EXTRA_QUESTION_ID — if continuing an existing conversation (loads its messages)
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PROMPT      = "extra_prompt";
    public static final String EXTRA_QUESTION_ID = "extra_question_id";

    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView rv;
    private TextInputEditText input;
    private TextView typing;
    private View layoutSuggestions;
    private long questionId = -1L;
    private String lastStepsJson;
    private String lastMistakesJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv                = findViewById(R.id.rv_messages);
        input             = findViewById(R.id.input_message);
        typing            = findViewById(R.id.text_typing);
        layoutSuggestions = findViewById(R.id.layout_suggestions);

        adapter = new MessageAdapter(messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        if (questionId > 0) {
            messages.addAll(StudyMentorApp.get().db().messageDao().forQuestion(questionId));
            adapter.notifyDataSetChanged();
            layoutSuggestions.setVisibility(View.GONE);
        } else {
            // First-time greeting
            messages.add(Message.assistant(0L, getString(R.string.chat_first_greeting)));
            adapter.notifyDataSetChanged();
        }

        String prefill = getIntent().getStringExtra(EXTRA_PROMPT);
        if (prefill != null && !prefill.isEmpty()) {
            input.setText(prefill);
            layoutSuggestions.setVisibility(View.GONE);
        }

        bindSuggestionChips();

        FloatingActionButton btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> sendCurrent());

        // UC2.5 — camera scan from chat composer
        View btnCamera = findViewById(R.id.btn_camera);
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                Intent i = new Intent(this, CameraActivity.class);
                i.putExtra(CameraActivity.EXTRA_SOURCE, "chat");
                startActivity(i);
            });
        }

        scrollToBottom();
    }

    private void bindSuggestionChips() {
        int[] chipIds = {
            R.id.chip_suggest_1, R.id.chip_suggest_2,
            R.id.chip_suggest_3, R.id.chip_suggest_4
        };
        for (int id : chipIds) {
            com.google.android.material.chip.Chip chip = findViewById(id);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    String text = chip.getText().toString();
                    input.setText(text);
                    sendCurrent();
                });
            }
        }
    }

    private void sendCurrent() {
        String text = String.valueOf(input.getText()).trim();
        if (text.isEmpty()) return;
        input.setText("");
        layoutSuggestions.setVisibility(View.GONE);

        // Persist the question on first user turn
        if (questionId <= 0) {
            Question q = new Question();
            q.prompt = text;
            q.subject = detectSubject(text);
            q.createdAt = System.currentTimeMillis();
            questionId = StudyMentorApp.get().db().questionDao().insert(q);
        }

        Message userMsg = Message.user(questionId, text);
        StudyMentorApp.get().db().messageDao().insert(userMsg);
        messages.add(userMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        callAi(text);
    }

    private void callAi(String prompt) {
        typing.setVisibility(View.VISIBLE);

        ChatRequest req = new ChatRequest(prompt, questionId);
        ApiClient.get().chat(req).enqueue(new Callback<ChatResponse>() {
            @Override public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                typing.setVisibility(View.GONE);
                String reply = (response.body() != null && response.body().reply != null)
                        ? response.body().reply
                        : getString(R.string.chat_error_unreachable);
                appendAssistant(reply);
                if (response.body() != null && response.body().reply != null) {
                    final long qid = questionId;
                    StudyMentorApp.get().executor().execute(() ->
                            StudyMentorApp.get().db().questionDao().updateAnswer(qid, reply));
                    if (response.body().steps != null && !response.body().steps.isEmpty()) {
                        lastStepsJson   = new Gson().toJson(response.body().steps);
                        lastMistakesJson = response.body().commonMistakes != null
                                ? new Gson().toJson(response.body().commonMistakes) : null;
                        offerViewSteps();
                    }
                }
            }
            @Override public void onFailure(Call<ChatResponse> call, Throwable t) {
                typing.setVisibility(View.GONE);
                appendAssistant(getString(R.string.chat_error_unreachable));
            }
        });
    }

    private void appendAssistant(String text) {
        Message m = Message.assistant(questionId, text);
        StudyMentorApp.get().executor().execute(() ->
                StudyMentorApp.get().db().messageDao().insert(m));
        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    /** Surfaces a "View steps" Snackbar so the user can open AnswerActivity for the latest reply. */
    private void offerViewSteps() {
        if (questionId <= 0) return;
        final long qid     = questionId;
        final String steps   = lastStepsJson;
        final String mistakes = lastMistakesJson;
        com.google.android.material.snackbar.Snackbar
                .make(rv, "Step-by-step breakdown ready", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("View", v -> {
                    Intent i = new Intent(this, AnswerActivity.class);
                    i.putExtra(AnswerActivity.EXTRA_QUESTION_ID, qid);
                    if (steps   != null) i.putExtra(AnswerActivity.EXTRA_STEPS_JSON,   steps);
                    if (mistakes != null) i.putExtra(AnswerActivity.EXTRA_MISTAKES_JSON, mistakes);
                    startActivity(i);
                })
                .show();
    }

    private void scrollToBottom() {
        rv.post(() -> rv.smoothScrollToPosition(Math.max(0, messages.size() - 1)));
    }

    private static String detectSubject(String prompt) {
        String lp = prompt.toLowerCase(Locale.US);
        String[] mathKw    = {"math", "equation", "algebra", "calculus", "geometry",
                               "derivative", "integral", "quadratic", "trigonometry",
                               "fraction", "percentage", "probability", "x^2"};
        String[] scienceKw = {"physics", "chemistry", "biology", "photosynthesis",
                               "molecule", "velocity", "acceleration", "newton",
                               "gravity", "evolution", "dna", "periodic table",
                               "atom", "reaction", "organism"};
        String[] codeKw    = {"code", "program", "function", "debug", "javascript",
                               "python", "algorithm", "array", "variable", "syntax",
                               "compiler", "database", "sql", "html", "css", "bug",
                               "loop", "class", "object", "git"};
        String[] historyKw = {"history", "war", "revolution", "dynasty", "civilization",
                               "ancient", "medieval", "empire", "colonial", "battle",
                               "president", "independence", "century", "kingdom"};
        for (String kw : mathKw)    if (lp.contains(kw)) return "math";
        for (String kw : scienceKw) if (lp.contains(kw)) return "science";
        for (String kw : codeKw)    if (lp.contains(kw)) return "code";
        for (String kw : historyKw) if (lp.contains(kw)) return "history";
        return "general";
    }
}
