package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityHomeBinding;
import com.studymentor.app.databinding.ItemQuickStartTileBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.studymentor.app.R;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.RecentQuestionAdapter;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.HistoryViewModel;
import com.studymentor.app.viewmodel.ProgressViewModel;
import com.studymentor.app.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private RecentQuestionAdapter recentAdapter;
    private HistoryViewModel historyViewModel;
    private ProgressViewModel progressViewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { routeLogin(); return; }
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bindUi();
        setupViewModels();
        BottomNavHelper.setup(this, R.id.nav_home);
    }

    private void bindUi() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ((TextView) binding.textGreetingSub).setText(hour < 12
                ? R.string.greeting_morning : hour < 18
                ? R.string.greeting_afternoon : R.string.greeting_evening);
        binding.btnBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        binding.btnChallengeStart.setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class)));
        configureTile(binding.cardQsMath, R.string.subject_math, R.color.subject_math,
                R.color.subject_math_soft, R.string.qs_subtitle_math);
        configureTile(binding.cardQsScience, R.string.subject_science, R.color.subject_science,
                R.color.subject_science_soft, R.string.qs_subtitle_science);
        configureTile(binding.cardQsCode, R.string.subject_code, R.color.subject_code,
                R.color.subject_code_soft, R.string.qs_subtitle_code);
        configureTile(binding.cardQsHistory, R.string.subject_history, R.color.subject_history,
                R.color.subject_history_soft, R.string.qs_subtitle_history);
        RecyclerView recent = binding.rvRecent;
        recent.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentQuestionAdapter(new ArrayList<>(), this::openAnswer);
        recent.setAdapter(recentAdapter);
        binding.cardComposer.setOnClickListener(v -> openChat(null));
        binding.btnComposeSend.setOnClickListener(v -> openChat(null));
        binding.btnComposeCamera.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_SOURCE, "home");
            startActivity(intent);
        });
    }

    private void setupViewModels() {
        long userId = Session.userId(this);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.profile().observe(this, profile ->
                ((TextView) binding.textUserName).setText(profile.user.displayName));
        userViewModel.error().observe(this, error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show());
        userViewModel.load(userId);

        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        progressViewModel.snapshot().observe(this, snapshot -> {
            Chip streak = binding.chipStreak;
            streak.setText(getResources().getQuantityString(
                    R.plurals.home_streak_days, snapshot.streak, snapshot.streak));
            streak.setVisibility(snapshot.streak > 0 ? View.VISIBLE : View.GONE);
            ((TextView) binding.textRingNum).setText(
                    String.valueOf(snapshot.quizAttempts));
            ((TextView) binding.textRingDenom).setText(R.string.home_quizzes);
            ((TextView) binding.textRingLbl).setText(
                    getString(R.string.home_accuracy, snapshot.accuracy));
            ((TextView) binding.textChallengeTitle).setText(
                    snapshot.totalQuestions == 0
                            ? R.string.home_ask_first : R.string.home_practice_saved);
        });
        progressViewModel.initialize(userId);

        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        historyViewModel.state().observe(this, state -> {
            if (state.data == null) return;
            List<Question> source = state.data.items;
            recentAdapter.setItems(new ArrayList<>(source.subList(
                    0, Math.min(5, source.size()))));
        });
        historyViewModel.initialize(userId);
    }

    private void configureTile(ItemQuickStartTileBinding tile, int titleRes, int tintRes,
                               int backgroundRes, int subtitleRes) {
        tile.textTileTitle.setText(titleRes);
        tile.textTileSubtitle.setText(subtitleRes);
        tile.containerTileIcon.setBackgroundTintList(
                ContextCompat.getColorStateList(this, backgroundRes));
        tile.imgTileIcon.setImageTintList(
                ContextCompat.getColorStateList(this, tintRes));
        tile.getRoot().setOnClickListener(v -> openChat(
                getString(R.string.home_study_subject, getString(titleRes))));
    }

    private void openAnswer(Question question) {
        Intent intent = new Intent(this, AnswerActivity.class);
        intent.putExtra(AnswerActivity.EXTRA_QUESTION_ID, question.id);
        startActivity(intent);
    }

    private void openChat(String prompt) {
        Intent intent = new Intent(this, ChatActivity.class);
        if (prompt != null && !prompt.trim().isEmpty())
            intent.putExtra(ChatActivity.EXTRA_PROMPT, prompt.trim());
        startActivity(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        if (historyViewModel != null) {
            historyViewModel.refresh();
            progressViewModel.refresh();
        }
    }

    private void routeLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
