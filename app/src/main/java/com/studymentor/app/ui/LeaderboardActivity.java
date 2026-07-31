package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityLeaderboardBinding;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.ProgressViewModel;

import java.util.Locale;

/** Preserves the design without fabricating global/friend competitors. */
public class LeaderboardActivity extends AppCompatActivity {
    private ActivityLeaderboardBinding binding;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        RecyclerView ranks = binding.rvRanks;
        ranks.setLayoutManager(new LinearLayoutManager(this));
        ranks.setAdapter(null);
        bindUnavailablePodium();
        bindTabs();

        ProgressViewModel viewModel = new ViewModelProvider(this)
                .get(ProgressViewModel.class);
        viewModel.snapshot().observe(this, snapshot -> {
            String name = snapshot.user.displayName == null
                    || snapshot.user.displayName.trim().isEmpty()
                    ? getString(R.string.leaderboard_you) : snapshot.user.displayName;
            ((TextView) binding.podium1Name).setText(initials(name));
            ((TextView) binding.podium1User).setText(name);
            ((TextView) binding.podium1Xp).setText(
                    getString(R.string.leaderboard_local_xp, snapshot.xp));
        });
        viewModel.error().observe(this, message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show());
        viewModel.initialize(Session.userId(this));
    }

    private void bindUnavailablePodium() {
        ((TextView) binding.podium1Name).setText(R.string.value_unavailable);
        ((TextView) binding.podium1User).setText(R.string.leaderboard_loading_local);
        ((TextView) binding.podium1Xp).setText(R.string.leaderboard_local_only);
        for (TextView unavailable : new TextView[]{binding.podium2Name, binding.podium2User,
                binding.podium2Xp, binding.podium3Name, binding.podium3User,
                binding.podium3Xp}) {
            unavailable.setText(R.string.value_unavailable);
        }
    }

    private void bindTabs() {
        View[] tabs = {binding.tabGlobal, binding.tabFriends, binding.tabWeek};
        for (int i = 0; i < tabs.length; i++) {
            final int selected = i;
            tabs[i].setOnClickListener(v -> {
                for (int j = 0; j < tabs.length; j++) {
                    tabs[j].setBackground(selected == j
                            ? getDrawable(R.drawable.bg_tab_active) : null);
                    ((TextView) tabs[j]).setTextColor(getColor(selected == j
                            ? R.color.text_primary : R.color.text_tertiary));
                }
                Toast.makeText(this, R.string.leaderboard_backend_required,
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private String initials(String name) {
        String clean = name.trim();
        if (clean.isEmpty()) return getString(R.string.leaderboard_initials_fallback);
        String[] words = clean.split("\\s+");
        String value = words.length > 1
                ? words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)
                : clean.substring(0, Math.min(2, clean.length()));
        return value.toUpperCase(Locale.ROOT);
    }
}
