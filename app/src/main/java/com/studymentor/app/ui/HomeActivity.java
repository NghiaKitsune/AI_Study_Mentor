package com.studymentor.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studymentor.app.util.BottomNavHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.studymentor.app.R;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.HistoryAdapter;
import com.studymentor.app.util.Session;
import com.studymentor.app.StudyMentorApp;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.studymentor.app.StudyReminderWorker;

/**
 * UC2 — Home.
 *   Greeting + user · streak · daily challenge · 4 quick-start tiles · recent questions · composer · bottom nav.
 *   Tapping composer → ChatActivity. Tapping a tile → ChatActivity with prefilled prompt.
 */
public class HomeActivity extends AppCompatActivity {

    private HistoryAdapter recentAdapter;

    /** Android 13+ runtime permission for system notifications. */
    private final androidx.activity.result.ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Result is ignored — user choice persists at system level.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bindGreeting();
        bindQuickStartTiles();
        bindRecent();
        bindComposer();
        bindBottomNav();
        maybeAskForNotifications();
        scheduleStudyReminder();
    }

    /** Shows the system POST_NOTIFICATIONS prompt once on Android 13+. */
    private void maybeAskForNotifications() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return;
        int state = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
        if (state != PackageManager.PERMISSION_GRANTED) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void bindGreeting() {
        TextView greeting = findViewById(R.id.text_greeting);
        TextView name     = findViewById(R.id.text_user_name);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int resId = hour < 12 ? R.string.greeting_morning
                              : hour < 18 ? R.string.greeting_afternoon
                                          : R.string.greeting_evening;
        greeting.setText(resId);
        name.setText(Session.name(this));

        // Streak chip — read from SharedPreferences, default to 0
        int streak = Session.streak(this);
        Chip chipStreak = findViewById(R.id.chip_streak);
        chipStreak.setText(streak + " days");
        chipStreak.setVisibility(streak > 0 ? View.VISIBLE : View.GONE);

        // XP progress stripe
        bindXpProgress();

        findViewById(R.id.btn_bell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.btn_start_now).setOnClickListener(v -> openChat(""));
    }

    private void bindXpProgress() {
        int totalQuestions = StudyMentorApp.get().db().questionDao().count();
        // Simple XP calculation: 10 XP per question answered
        int totalXp = totalQuestions * 10;
        int level = Math.max(1, totalXp / 100 + 1);
        int xpInLevel = totalXp % 100;
        int xpNextLevel = 100;

        TextView tvLevel = findViewById(R.id.text_xp_level);
        TextView tvXp    = findViewById(R.id.text_xp_value);
        LinearProgressIndicator bar = findViewById(R.id.progress_xp);

        tvLevel.setText(levelTitle(level) + " · Level " + level);
        tvXp.setText(xpInLevel + " / " + xpNextLevel + " XP");
        bar.setProgressCompat((int) ((xpInLevel / (float) xpNextLevel) * 100), true);
    }

    /** Configure the 4 included tiles. Each `<include>` is its own MaterialCardView root. */
    private void bindQuickStartTiles() {
        configTile(R.id.tile_math,    R.string.subject_math,    R.color.subject_math,    "Solve a problem");
        configTile(R.id.tile_science, R.string.subject_science, R.color.subject_science, "Ask a question");
        configTile(R.id.tile_code,    R.string.subject_code,    R.color.subject_code,    "Debug some code");
        configTile(R.id.tile_history, R.string.subject_history, R.color.subject_history, "Explore a topic");
    }

    private void configTile(int rootId, int titleRes, int tintRes, String subtitle) {
        View root = findViewById(rootId);
        ((TextView) root.findViewById(R.id.text_tile_title)).setText(titleRes);
        ((TextView) root.findViewById(R.id.text_tile_subtitle)).setText(subtitle);
        ImageView icon = root.findViewById(R.id.img_tile_icon);
        icon.setImageTintList(ContextCompat.getColorStateList(this, tintRes));
        root.setOnClickListener(v -> openChat(getString(titleRes)));
    }

    private void bindRecent() {
        RecyclerView rv = findViewById(R.id.rv_recent);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Question> recent = StudyMentorApp.get().db().questionDao().recent(5);
        recentAdapter = new HistoryAdapter(recent, q -> {
            Intent i = new Intent(this, AnswerActivity.class);
            i.putExtra(AnswerActivity.EXTRA_QUESTION_ID, q.id);
            startActivity(i);
        });
        rv.setAdapter(recentAdapter);
    }

    private void bindComposer() {
        View composer = findViewById(R.id.card_composer);
        composer.setOnClickListener(v -> openChat(""));
        findViewById(R.id.btn_compose_send).setOnClickListener(v -> openChat(""));
        findViewById(R.id.btn_compose_camera).setOnClickListener(v -> openCamera());
    }

    /** UC2.5 — Camera scan flow. Opens CameraActivity which routes to ScanPreviewActivity. */
    private void openCamera() {
        Intent i = new Intent(this, CameraActivity.class);
        i.putExtra(CameraActivity.EXTRA_SOURCE, "home");
        startActivity(i);
    }

    private void bindBottomNav() {
        BottomNavHelper.setup(this, R.id.nav_home);
    }

    private void scheduleStudyReminder() {
        if (!Session.notificationsOn(this)) return;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                StudyReminderWorker.class, 1, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "study_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }

    private static String levelTitle(int level) {
        if (level >= 10) return "Master";
        if (level >= 7)  return "Expert";
        if (level >= 5)  return "Scholar";
        if (level >= 3)  return "Explorer";
        return "Beginner";
    }

    private void openChat(String prefill) {
        Intent i = new Intent(this, ChatActivity.class);
        if (prefill != null && !prefill.isEmpty()) i.putExtra(ChatActivity.EXTRA_PROMPT, prefill);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recentAdapter != null) {
            recentAdapter.setItems(StudyMentorApp.get().db().questionDao().recent(5));
        }
    }
}
