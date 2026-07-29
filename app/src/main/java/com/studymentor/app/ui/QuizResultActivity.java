package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityQuizResultBinding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.studymentor.app.R;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.QuizResultViewModel;

public class QuizResultActivity extends AppCompatActivity {
    private ActivityQuizResultBinding binding;
    public static final String EXTRA_ATTEMPT_ID = "extra_attempt_id";
    private int score;
    private int total;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityQuizResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnTryAgain.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        binding.btnShare.setOnClickListener(v -> share());

        long attemptId = getIntent().getLongExtra(EXTRA_ATTEMPT_ID, -1L);
        if (attemptId <= 0L) {
            Toast.makeText(this, R.string.quiz_result_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        QuizResultViewModel viewModel = new ViewModelProvider(this)
                .get(QuizResultViewModel.class);
        viewModel.data().observe(this, data -> {
            score = data.attempt.score;
            total = data.attempt.total;
            bind();
        });
        viewModel.error().observe(this, error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show());
        viewModel.load(Session.userId(this), attemptId);
    }

    private void bind() {
        int incorrect = Math.max(0, total - score);
        int percent = total == 0 ? 0 : Math.round(score * 100f / total);
        ((TextView) binding.textScoreNum).setText(String.valueOf(score));
        ((TextView) binding.textScoreDenom).setText(getString(
                R.string.quiz_score_denominator, total));
        ((TextView) binding.textResultLabel).setText(percent >= 80
                ? R.string.quiz_result_great : percent >= 50
                ? R.string.quiz_result_keep : R.string.quiz_result_review);
        ((TextView) binding.textXpPill).setText(R.string.quiz_xp_recorded);
        ((TextView) binding.textStatCorrect).setText(String.valueOf(score));
        ((TextView) binding.textStatIncorrect).setText(String.valueOf(incorrect));
        ((TextView) binding.textStatAccuracy).setText(
                getString(R.string.percent_value, percent));
        binding.containerBreakdown.setVisibility(android.view.View.GONE);
    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.quiz_share_result, score, total));
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
    }
}
