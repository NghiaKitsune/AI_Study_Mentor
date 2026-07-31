package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityProfileBinding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.studymentor.app.R;
import com.studymentor.app.repository.ProgressRepository;
import com.studymentor.app.ui.adapter.AchievementAdapter;
import com.studymentor.app.ui.adapter.ActivityEventAdapter;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.ProgressViewModel;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private ProgressViewModel viewModel;
    private final AchievementAdapter achievements = new AchievementAdapter();
    private final ActivityEventAdapter events = new ActivityEventAdapter();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
        binding.btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
        RecyclerView badges = binding.rvBadges;
        badges.setLayoutManager(new GridLayoutManager(this, 4));
        badges.setAdapter(achievements);
        RecyclerView activity = binding.rvActivity;
        activity.setLayoutManager(new LinearLayoutManager(this));
        activity.setAdapter(events);
        BottomNavHelper.setup(this, R.id.nav_profile);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.snapshot().observe(this, snapshot -> {
            ((TextView) binding.textProfileName).setText(snapshot.user.displayName);
            ((TextView) binding.textLevelTitle).setText(getString(
                    R.string.profile_level_title, levelTitle(snapshot.level), snapshot.level));
            int start = ProgressRepository.levelStartXp(snapshot.level);
            int next = ProgressRepository.nextLevelXp(snapshot.level);
            int inLevel = Math.max(0, snapshot.xp - start);
            int needed = Math.max(1, next - start);
            int percent = Math.min(100, Math.round(inLevel * 100f / needed));
            ((TextView) binding.textXpCurrent).setText(
                    getString(R.string.profile_xp, inLevel));
            ((TextView) binding.textXpRemaining).setText(
                    getString(R.string.profile_xp_remaining, Math.max(0, next - snapshot.xp)));
            ((LinearProgressIndicator) binding.progressXp)
                    .setProgressCompat(percent, true);
            ((CircularProgressIndicator) binding.progressRingXp)
                    .setProgressCompat(percent, true);
            ((TextView) binding.textStatStreak).setText(
                    String.valueOf(snapshot.streak));
            ((TextView) binding.textStatXp).setText(String.valueOf(snapshot.xp));
            ((TextView) binding.textStatBadges).setText(
                    String.valueOf(snapshot.achievements));
            achievements.setItems(snapshot.achievementItems);
            events.setItems(snapshot.recentEvents);
        });
        viewModel.error().observe(this, error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show());
        viewModel.initialize(Session.userId(this));
    }

    private static String levelTitle(int level) {
        if (level >= 10) return "Master";
        if (level >= 7) return "Expert";
        if (level >= 5) return "Scholar";
        if (level >= 3) return "Explorer";
        return "Beginner";
    }

    @Override protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }
}
