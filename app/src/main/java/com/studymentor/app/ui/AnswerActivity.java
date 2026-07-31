package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityAnswerBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studymentor.app.R;
import com.studymentor.app.api.ChatResponse;
import com.studymentor.app.data.AnswerDetail;
import com.studymentor.app.data.Question;
import com.studymentor.app.repository.HistoryRepository;
import com.studymentor.app.ui.adapter.StepAdapter;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.AnswerViewModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AnswerActivity extends AppCompatActivity {
    private ActivityAnswerBinding binding;
    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    public static final String EXTRA_STEPS_JSON = "extra_steps_json";
    public static final String EXTRA_MISTAKES_JSON = "extra_mistakes_json";

    private final Gson gson = new Gson();
    private AnswerViewModel viewModel;
    private Question question;
    private MaterialButton bookmark;
    private long reviewStartedAt;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityAnswerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        bookmark = binding.btnBookmark;
        bookmark.setOnClickListener(v -> viewModel.toggleBookmark());
        binding.btnShare.setOnClickListener(v -> share());
        viewModel = new ViewModelProvider(this).get(AnswerViewModel.class);
        viewModel.detail().observe(this, this::bind);
        viewModel.error().observe(this, message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show());
        long id = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        if (id <= 0) {
            Toast.makeText(this, R.string.answer_question_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel.load(Session.userId(this), id);
    }

    @Override protected void onStart() {
        super.onStart();
        reviewStartedAt = System.currentTimeMillis();
    }

    @Override protected void onStop() {
        if (reviewStartedAt > 0 && viewModel != null) {
            long seconds = Math.max(0,
                    (System.currentTimeMillis() - reviewStartedAt) / 1000L);
            viewModel.recordReviewDuration(seconds);
            reviewStartedAt = 0;
        }
        super.onStop();
    }

    private void bind(HistoryRepository.QuestionDetail value) {
        question = value.question;
        ((TextView) binding.textQuestion).setText(question.prompt);
        ((TextView) binding.textFinalAnswer).setText(
                question.answer == null || question.answer.trim().isEmpty()
                        ? getString(R.string.answer_not_saved) : question.answer);
        bindBookmark();
        bindDetail(value.detail);
    }

    private void bindDetail(AnswerDetail detail) {
        List<ChatResponse.Step> steps = parseSteps(detail == null ? null : detail.stepsJson);
        RecyclerView recycler = binding.rvSteps;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);
        recycler.setAdapter(new StepAdapter(steps));
        recycler.setVisibility(steps.isEmpty() ? View.GONE : View.VISIBLE);
        binding.textStepsHeading.setVisibility(
                steps.isEmpty() ? View.GONE : View.VISIBLE);

        List<String> mistakes = parseStrings(
                detail == null ? null : detail.commonMistakesJson);
        binding.layoutMistakes.setVisibility(
                mistakes.isEmpty() ? View.GONE : View.VISIBLE);
        binding.textMistakesHeading.setVisibility(
                mistakes.isEmpty() ? View.GONE : View.VISIBLE);
        if (!mistakes.isEmpty())
            ((TextView) binding.textMistake1).setText(mistakes.get(0));
        TextView second = binding.textMistake2;
        second.setVisibility(mistakes.size() > 1 ? View.VISIBLE : View.GONE);
        if (mistakes.size() > 1) second.setText(mistakes.get(1));

        List<String> followUps = parseStrings(detail == null ? null : detail.followUpsJson);
        bindFollowUp(binding.chipFollowSimpler, followUps, 0);
        bindFollowUp(binding.chipFollowAnother, followUps, 1);
        Chip practice = binding.chipFollowPractice;
        if (followUps.size() > 2) practice.setText(followUps.get(2));
        practice.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(QuizActivity.EXTRA_SUBJECT, question.subject);
            startActivity(intent);
        });
    }

    private void bindFollowUp(Chip chip, List<String> followUps, int index) {
        if (index >= followUps.size()) {
            chip.setVisibility(View.GONE);
            return;
        }
        String prompt = followUps.get(index);
        chip.setVisibility(View.VISIBLE);
        chip.setText(prompt);
        chip.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_PROMPT, prompt);
            startActivity(intent);
        });
    }

    private List<ChatResponse.Step> parseSteps(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<ChatResponse.Step>>() {}.getType();
            List<ChatResponse.Step> parsed = gson.fromJson(json, type);
            return parsed == null ? new ArrayList<>() : parsed;
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

    private void bindBookmark() {
        boolean enabled = question != null && question.bookmarked;
        bookmark.setIconResource(enabled
                ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        bookmark.setIconTintResource(enabled
                ? R.color.brand_primary : R.color.text_primary);
    }

    private void share() {
        if (question == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.answer_share_body,
                question.prompt, question.answer == null ? "" : question.answer));
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
    }
}
