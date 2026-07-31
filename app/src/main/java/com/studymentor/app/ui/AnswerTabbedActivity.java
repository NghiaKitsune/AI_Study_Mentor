package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityAnswerTabbedBinding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studymentor.app.R;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.data.AnswerDetail;
import com.studymentor.app.data.Question;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.AnswerViewModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Four-tab offline view backed by the structured Gemini answer stored in Room. */
public class AnswerTabbedActivity extends AppCompatActivity {
    private ActivityAnswerTabbedBinding binding;
    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    public static final String EXTRA_STEPS_JSON = "extra_steps_json";
    private static final int TAB_SOLUTION = 0;
    private static final int TAB_CONCEPT = 1;
    private static final int TAB_PRACTICE = 2;
    private static final int TAB_PITFALLS = 3;

    private final Gson gson = new Gson();
    private int activeTab;
    private TextView[] tabs;
    private View[] indicators;
    private Question question;
    private AnswerDetail detail;
    private AnswerViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityAnswerTabbedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tabs = new TextView[]{binding.tabSolution, binding.tabConcept,
                binding.tabPractice, binding.tabPitfalls};
        indicators = new View[]{binding.indSolution, binding.indConcept,
                binding.indPractice, binding.indPitfalls};
        for (int i = 0; i < tabs.length; i++) {
            final int tab = i;
            tabs[i].setOnClickListener(v -> switchTab(tab));
        }
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v ->
                android.widget.Toast.makeText(this, R.string.toast_coming_soon,
                        android.widget.Toast.LENGTH_SHORT).show());
        binding.btnBookmark.setOnClickListener(v -> toggleBookmark());
        switchTab(TAB_SOLUTION);

        long questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        if (questionId <= 0) { showError(); return; }
        viewModel = new ViewModelProvider(this).get(AnswerViewModel.class);
        viewModel.detail().observe(this, value -> {
            if (value == null || value.question == null) { showError(); return; }
            question = value.question;
            detail = value.detail;
            ((TextView) binding.textQuestion).setText(question.prompt);
            renderContent(activeTab);
        });
        viewModel.error().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) showError();
        });
        viewModel.load(Session.userId(this), questionId);
    }

    private void switchTab(int index) {
        activeTab = index;
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == index;
            tabs[i].setTextColor(getColor(selected ? R.color.text_primary : R.color.text_tertiary));
            indicators[i].setVisibility(selected ? View.VISIBLE : View.GONE);
        }
        renderContent(index);
    }

    private void renderContent(int index) {
        LinearLayout container = binding.contentContainer;
        container.removeAllViews();
        if (question == null) {
            addBody(container, getString(R.string.answer_tabbed_loading));
            return;
        }
        if (index == TAB_SOLUTION) renderSolution(container);
        else if (index == TAB_CONCEPT) renderConcept(container);
        else if (index == TAB_PRACTICE) renderPractice(container);
        else renderPitfalls(container);
    }

    private void renderSolution(LinearLayout container) {
        List<ChatResponse.Step> steps = parseSteps(detail == null ? null : detail.stepsJson);
        if (steps.isEmpty()) {
            addSection(container, getString(R.string.answer_final),
                    question.answer == null ? "" : question.answer);
            return;
        }
        for (ChatResponse.Step step : steps) addSection(container, step.title, step.body);
    }

    private void renderConcept(LinearLayout container) {
        List<String> concepts = parseStrings(detail == null ? null : detail.keyConceptsJson);
        for (String concept : concepts) addSection(container,
                getString(R.string.answer_key_concept), concept);
        if (detail != null && detail.alternativeApproach != null
                && !detail.alternativeApproach.trim().isEmpty()) {
            addSection(container, getString(R.string.answer_alternative), detail.alternativeApproach);
        }
        if (concepts.isEmpty() && (detail == null
                || detail.alternativeApproach.trim().isEmpty())) {
            addBody(container, getString(R.string.answer_no_concepts));
        }
    }

    private void renderPractice(LinearLayout container) {
        List<String> examples = parseStrings(detail == null ? null : detail.examplesJson);
        List<String> followUps = parseStrings(detail == null ? null : detail.followUpsJson);
        int number = 1;
        for (String example : examples) addSection(container,
                getString(R.string.answer_example_number, number++), example);
        for (String followUp : followUps) addSection(container,
                getString(R.string.answer_try_next), followUp);
        if (examples.isEmpty() && followUps.isEmpty())
            addBody(container, getString(R.string.answer_no_practice));
    }

    private void renderPitfalls(LinearLayout container) {
        List<String> mistakes = parseStrings(detail == null ? null : detail.commonMistakesJson);
        if (mistakes.isEmpty()) {
            addBody(container, getString(R.string.answer_no_pitfalls));
            return;
        }
        for (String mistake : mistakes) addSection(container,
                getString(R.string.answer_common_mistake), mistake);
    }

    private List<ChatResponse.Step> parseSteps(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<ChatResponse.Step>>() {}.getType();
            List<ChatResponse.Step> values = gson.fromJson(json, type);
            return values == null ? new ArrayList<>() : values;
        } catch (Exception ignored) { return new ArrayList<>(); }
    }

    private List<String> parseStrings(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> values = gson.fromJson(json, type);
            return values == null ? new ArrayList<>() : values;
        } catch (Exception ignored) { return new ArrayList<>(); }
    }

    private void toggleBookmark() {
        if (question != null && viewModel != null) viewModel.toggleBookmark();
    }

    private void showError() {
        LinearLayout container = binding.contentContainer;
        container.removeAllViews();
        addBody(container, getString(R.string.answer_tabbed_error));
    }

    private void addSection(LinearLayout container, String title, String body) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        TextView heading = item.findViewById(R.id.text_section_title);
        heading.setText(title);
        heading.setVisibility(View.VISIBLE);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(body);
        container.addView(item);
    }

    private void addBody(LinearLayout container, String text) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(text);
        container.addView(item);
    }
}
