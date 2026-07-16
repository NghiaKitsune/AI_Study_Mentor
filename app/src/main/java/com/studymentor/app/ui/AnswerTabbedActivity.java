package com.studymentor.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.api.GroqTabbedService;
import com.studymentor.app.api.TabbedResponse;
import com.studymentor.app.data.Question;

public class AnswerTabbedActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    public static final String EXTRA_STEPS_JSON  = "extra_steps_json";

    private static final int TAB_SOLUTION = 0;
    private static final int TAB_CONCEPT  = 1;
    private static final int TAB_PRACTICE = 2;
    private static final int TAB_PITFALLS = 3;

    private int activeTab = TAB_SOLUTION;
    private TextView[] tabs;
    private View[]     indicators;
    private TabbedResponse tabbedData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_tabbed);

        // Load question from DB
        long qid = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        Question question = (qid > 0)
                ? StudyMentorApp.get().db().questionDao().byId(qid)
                : null;

        String questionText = (question != null) ? question.prompt
                : getIntent().getStringExtra("extra_question");
        if (questionText == null) questionText = "";

        String subject   = (question != null) ? question.subject : "general";
        String stepsJson = getIntent().getStringExtra(EXTRA_STEPS_JSON);

        ((TextView) findViewById(R.id.text_question)).setText(questionText);

        tabs = new TextView[]{
            findViewById(R.id.tab_solution),
            findViewById(R.id.tab_concept),
            findViewById(R.id.tab_practice),
            findViewById(R.id.tab_pitfalls),
        };
        indicators = new View[]{
            findViewById(R.id.ind_solution),
            findViewById(R.id.ind_concept),
            findViewById(R.id.ind_practice),
            findViewById(R.id.ind_pitfalls),
        };

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTab(idx));
        }

        // Show loading state
        switchTab(TAB_SOLUTION);

        // Call Groq API (skip if no API key / empty question)
        if (!questionText.isEmpty()) {
            fetchTabbedContent(questionText, subject, stepsJson);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v ->
            android.widget.Toast.makeText(this, R.string.toast_coming_soon,
                    android.widget.Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_bookmark).setOnClickListener(v ->
            android.widget.Toast.makeText(this, R.string.action_bookmark,
                    android.widget.Toast.LENGTH_SHORT).show());
    }

    private void fetchTabbedContent(String question, String subject, String stepsJson) {
        new GroqTabbedService().generate(question, subject, stepsJson,
                new GroqTabbedService.Callback() {
                    @Override
                    public void onSuccess(TabbedResponse response) {
                        tabbedData = response;
                        renderContent(activeTab);
                    }

                    @Override
                    public void onError(String message) {
                        showError();
                    }
                });
    }

    private void switchTab(int idx) {
        activeTab = idx;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == idx);
            tabs[i].setTextColor(getColor(active ? R.color.text_primary : R.color.text_tertiary));
            indicators[i].setVisibility(active ? View.VISIBLE : View.GONE);
        }
        renderContent(idx);
    }

    private void renderContent(int idx) {
        LinearLayout container = findViewById(R.id.content_container);
        container.removeAllViews();

        if (tabbedData == null) {
            addBody(container, getString(R.string.answer_tabbed_loading));
            return;
        }

        switch (idx) {
            case TAB_SOLUTION: renderSolution(container); break;
            case TAB_CONCEPT:  renderConcept(container);  break;
            case TAB_PRACTICE: renderPractice(container); break;
            case TAB_PITFALLS: renderPitfalls(container); break;
        }
    }

    private void renderSolution(LinearLayout container) {
        if (tabbedData.solution == null || tabbedData.solution.isEmpty()) {
            addBody(container, "No solution steps available.");
            return;
        }
        for (TabbedResponse.SolutionStep step : tabbedData.solution) {
            addSection(container, step.title, step.body);
        }
    }

    private void renderConcept(LinearLayout container) {
        TabbedResponse.Concept c = tabbedData.concept;
        if (c == null) { addBody(container, "No concept data."); return; }

        if (c.formula != null && !c.formula.isEmpty()) {
            addSection(container, "Key Formula / Concept", c.formula);
        }
        if (c.explanation != null && !c.explanation.isEmpty()) {
            addSection(container, "Explanation", c.explanation);
        }
        if (c.funFact != null && !c.funFact.isEmpty()) {
            addSection(container, "Fun Fact", c.funFact);
        }
    }

    private void renderPractice(LinearLayout container) {
        if (tabbedData.practice == null || tabbedData.practice.isEmpty()) {
            addBody(container, "No practice questions available.");
            return;
        }
        addBody(container, "Quick check — pick the correct answer:");
        int num = 1;
        for (TabbedResponse.PracticeQuestion pq : tabbedData.practice) {
            StringBuilder sb = new StringBuilder();
            if (pq.options != null) {
                for (String opt : pq.options) sb.append(opt).append("\n");
            }
            if (pq.hint != null && !pq.hint.isEmpty()) {
                sb.append("\nHint: ").append(pq.hint);
            }
            addSection(container, "Q" + num + ": " + pq.question, sb.toString().trim());
            num++;
        }
    }

    private void renderPitfalls(LinearLayout container) {
        if (tabbedData.pitfalls == null || tabbedData.pitfalls.isEmpty()) {
            addBody(container, "No pitfalls data.");
            return;
        }
        for (String pitfall : tabbedData.pitfalls) {
            addSection(container, "✗ Common Mistake", pitfall);
        }
    }

    private void showError() {
        LinearLayout container = findViewById(R.id.content_container);
        container.removeAllViews();
        addBody(container, getString(R.string.answer_tabbed_error));
    }

    private void addSection(LinearLayout container, String title, String body) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        TextView tvTitle = item.findViewById(R.id.text_section_title);
        tvTitle.setText(title);
        tvTitle.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
        lp.topMargin = dpToPx(14);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(body);
        container.addView(item);
    }

    private void addBody(LinearLayout container, String text) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(text);
        container.addView(item);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
