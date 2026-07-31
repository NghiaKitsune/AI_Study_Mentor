package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivitySettingsBinding;
import com.studymentor.app.databinding.ItemSettingRowBinding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.studymentor.app.R;
import com.studymentor.app.data.ReminderSchedule;
import com.studymentor.app.repository.UserRepository;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.NotificationViewModel;
import com.studymentor.app.viewmodel.UserViewModel;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private UserViewModel userViewModel;
    private NotificationViewModel notificationViewModel;
    private UserRepository.UserProfile profile;
    private ReminderSchedule schedule;
    private TextView languageValue;
    private TextView notificationValue;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) setReminder(true);
                else Toast.makeText(this, R.string.notifications_permission_denied,
                        Toast.LENGTH_LONG).show();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        bindTheme();
        bindLanguage();
        bindNotifications();
        bindSignOut();
        observeState();
        long userId = Session.userId(this);
        userViewModel.load(userId);
        notificationViewModel.initialize(userId);
    }

    private void observeState() {
        userViewModel.profile().observe(this, value -> {
            if (value == null) return;
            profile = value;
            ((TextView) binding.textProfileName).setText(value.user.displayName);
            ((TextView) binding.textProfileEmail).setText(value.user.email);
            languageValue.setText(value.preference != null
                    && "vi".equals(value.preference.language)
                    ? R.string.language_vietnamese : R.string.language_english);
        });
        userViewModel.error().observe(this, this::showError);
        notificationViewModel.data().observe(this, value -> {
            if (value == null) return;
            schedule = value.schedule;
            updateReminderLabel();
        });
        notificationViewModel.error().observe(this, this::showError);
    }

    private void bindTheme() {
        ItemSettingRowBinding row = binding.rowTheme;
        row.imgRowIcon.setImageResource(R.drawable.ic_sparkles);
        row.textRowLabel.setText(R.string.row_theme_label);
        TextView value = row.textRowValue;
        value.setText(themeLabel(Session.themeMode(this)));
        row.getRoot().setOnClickListener(v -> {
            int next = nextTheme(Session.themeMode(this));
            Session.setThemeMode(this, next);
            AppCompatDelegate.setDefaultNightMode(next);
            recreate();
        });
    }

    private void bindLanguage() {
        ItemSettingRowBinding row = binding.rowLanguage;
        row.imgRowIcon.setImageResource(R.drawable.ic_book);
        row.textRowLabel.setText(R.string.row_language_label);
        languageValue = row.textRowValue;
        row.getRoot().setOnClickListener(v -> {
            if (profile == null) return;
            String next = profile.preference != null
                    && "vi".equals(profile.preference.language) ? "en" : "vi";
            Session.setLanguage(this, next);
            userViewModel.setLanguage(Session.userId(this), next);
        });
    }

    private void bindNotifications() {
        ItemSettingRowBinding row = binding.rowNotifications;
        row.imgRowIcon.setImageResource(R.drawable.ic_bell);
        row.textRowLabel.setText(R.string.row_notifications_label);
        notificationValue = row.textRowValue;
        row.getRoot().setOnClickListener(v -> {
            boolean enable = schedule == null || !schedule.enabled;
            if (enable && Build.VERSION.SDK_INT >= 33
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else setReminder(enable);
        });
    }

    private void setReminder(boolean enabled) {
        int days = schedule == null ? 1 : schedule.intervalDays;
        int hour = schedule == null ? 19 : schedule.preferredHour;
        notificationViewModel.setReminder(enabled, days, hour);
    }

    private void updateReminderLabel() {
        if (notificationValue == null || schedule == null) return;
        notificationValue.setText(schedule.enabled
                ? getString(R.string.notifications_on_hour, schedule.preferredHour)
                : getString(R.string.notifications_off));
    }

    private void bindSignOut() {
        binding.btnSignOut.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.signout_title)
                        .setMessage(R.string.signout_body)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.signout_confirm, (dialog, which) -> {
                            Session.clearAuth(this);
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }).show());
    }

    private void showError(String message) {
        if (message != null && !message.trim().isEmpty())
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static int nextTheme(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            return AppCompatDelegate.MODE_NIGHT_NO;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private String themeLabel(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return getString(R.string.theme_light);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return getString(R.string.theme_dark);
        return getString(R.string.theme_system);
    }
}
