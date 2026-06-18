package com.studymentor.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.StudyReminderWorker;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.RecentQuestionAdapter;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

/**
 * UC2 — Home.
 * Greeting · streak · daily challenge card + progress ring · horizontal quick-start tiles
 * · recent questions list · composer · bottom nav.
 */
public class HomeActivity extends AppCompatActivity {

    private RecentQuestionAdapter recentAdapter;

    private final androidx.activity.result.ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

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

    private void maybeAskForNotifications() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void bindGreeting() {
        TextView greeting = findViewById(R.id.text_greeting_sub);
        TextView name     = findViewById(R.id.text_user_name);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int resId = hour < 12 ? R.string.greeting_morning
                              : hour < 18 ? R.string.greeting_afternoon
                                          : R.string.greeting_evening;
        greeting.setText(resId);
        name.setText(Session.name(this));

        int streak = Session.streak(this);
        Chip chipStreak = findViewById(R.id.chip_streak);
        chipStreak.setText(streak + " days");
        chipStreak.setVisibility(streak > 0 ? View.VISIBLE : View.GONE);

        findViewById(R.id.btn_bell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.btn_challenge_start).setOnClickListener(v -> openChat(""));
    }

    private void bindQuickStartTiles() {
        configTile(R.id.card_qs_math,    R.string.subject_math,    R.color.subject_math,    R.color.subject_math_soft,    R.string.qs_subtitle_math);
        configTile(R.id.card_qs_science, R.string.subject_science, R.color.subject_science, R.color.subject_science_soft, R.string.qs_subtitle_science);
        configTile(R.id.card_qs_code,    R.string.subject_code,    R.color.subject_code,    R.color.subject_code_soft,    R.string.qs_subtitle_code);
        configTile(R.id.card_qs_history, R.string.subject_history, R.color.subject_history, R.color.subject_history_soft, R.string.qs_subtitle_history);
    }

    private void configTile(int rootId, int titleRes, int tintRes, int bgTintRes, int subtitleRes) {
        View root = findViewById(rootId);
        ((TextView) root.findViewById(R.id.text_tile_title)).setText(titleRes);
        ((TextView) root.findViewById(R.id.text_tile_subtitle)).setText(subtitleRes);
        View container = root.findViewById(R.id.container_tile_icon);
        container.setBackgroundTintList(ContextCompat.getColorStateList(this, bgTintRes));
        ImageView icon = root.findViewById(R.id.img_tile_icon);
        icon.setImageTintList(ContextCompat.getColorStateList(this, tintRes));
        root.setOnClickListener(v -> openChat(getString(titleRes)));
    }

    private void bindRecent() {
        RecyclerView rv = findViewById(R.id.rv_recent);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Question> recent = StudyMentorApp.get().db().questionDao().recent(5);
        recentAdapter = new RecentQuestionAdapter(recent, q -> {
            Intent i = new Intent(this, AnswerActivity.class);
            i.putExtra(AnswerActivity.EXTRA_QUESTION_ID, q.id);
            startActivity(i);
        });
        rv.setAdapter(recentAdapter);
    }

    private void bindComposer() {
        findViewById(R.id.card_composer).setOnClickListener(v -> openChat(""));
        findViewById(R.id.btn_compose_send).setOnClickListener(v -> openChat(""));
        findViewById(R.id.btn_compose_camera).setOnClickListener(v -> openCamera());
    }

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
