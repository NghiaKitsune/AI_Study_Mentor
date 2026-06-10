package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * UC10 — Settings.
 *   - Profile mini-row reads from Session
 *   - Theme row cycles System → Light → Dark and persists via Session
 *   - Notifications row sends user to system app settings (POST_NOTIFICATIONS)
 *   - Sign out shows a confirmation dialog before clearing the session
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.text_profile_name)).setText(Session.name(this));
        ((TextView) findViewById(R.id.text_profile_email)).setText(Session.email(this));

        // Profile card → ProfileActivity
        View cardProfile = findViewById(R.id.card_profile);
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        bindThemeRow();
        bindLanguageRow();
        bindNotificationsRow();
        bindSignOut();
    }

    /** Cycles System → Light → Dark on tap, and applies immediately. */
    private void bindThemeRow() {
        View row = findViewById(R.id.row_theme);
        ImageView icon = row.findViewById(R.id.img_row_icon);
        TextView label = row.findViewById(R.id.text_row_label);
        TextView value = row.findViewById(R.id.text_row_value);

        icon.setImageResource(R.drawable.ic_sparkles);
        label.setText(R.string.row_theme_label);
        value.setText(themeLabel(Session.themeMode(this)));

        row.setOnClickListener(v -> {
            int next = nextTheme(Session.themeMode(this));
            Session.setThemeMode(this, next);
            AppCompatDelegate.setDefaultNightMode(next);
            value.setText(themeLabel(next));
            recreate(); // Re-inflate to pick up the new mode in this Activity
        });
    }

    private int nextTheme(int current) {
        if (current == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) return AppCompatDelegate.MODE_NIGHT_NO;
        if (current == AppCompatDelegate.MODE_NIGHT_NO)            return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private String themeLabel(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)  return getString(R.string.theme_light);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return getString(R.string.theme_dark);
        return getString(R.string.theme_system);
    }

    private void bindLanguageRow() {
        View row = findViewById(R.id.row_language);
        ((ImageView) row.findViewById(R.id.img_row_icon)).setImageResource(R.drawable.ic_book);
        ((TextView)  row.findViewById(R.id.text_row_label)).setText(R.string.row_language_label);
        ((TextView)  row.findViewById(R.id.text_row_value)).setText("English");
        row.setOnClickListener(v -> Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show());
    }

    private void bindNotificationsRow() {
        View row = findViewById(R.id.row_notifications);
        ((ImageView) row.findViewById(R.id.img_row_icon)).setImageResource(R.drawable.ic_bell);
        ((TextView)  row.findViewById(R.id.text_row_label)).setText(R.string.row_notifications_label);
        ((TextView)  row.findViewById(R.id.text_row_value)).setText("On");
        row.setOnClickListener(v -> {
            // Send user to the system app notification settings page
            Intent i = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });
    }

    private void bindSignOut() {
        findViewById(R.id.btn_sign_out).setOnClickListener(v -> showSignOutConfirm());
    }

    private void showSignOutConfirm() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.signout_title)
                .setMessage(R.string.signout_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.signout_confirm, (d, w) -> {
                    Session.clear(this);
                    Intent i = new Intent(this, SignUpActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .show();
    }
}
