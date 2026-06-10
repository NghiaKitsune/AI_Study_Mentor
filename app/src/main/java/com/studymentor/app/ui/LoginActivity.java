package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * UC1 — Login. For returning users.
 * Mock auth: validates email + password length, then signs in.
 * Replace `mockAuth(...)` with a real API call when wiring a backend.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout    tilEmail, tilPassword;
    private TextInputEditText  inputEmail, inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail      = findViewById(R.id.til_email);
        tilPassword   = findViewById(R.id.til_password);
        inputEmail    = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        findViewById(R.id.btn_login).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.btn_forgot).setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        findViewById(R.id.btn_signup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void attemptLogin() {
        String email = String.valueOf(inputEmail.getText()).trim();
        String pw    = String.valueOf(inputPassword.getText());

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }
        tilEmail.setError(null);

        if (pw.length() < 8) {
            tilPassword.setError(getString(R.string.error_password_short));
            return;
        }
        tilPassword.setError(null);

        // TODO: real auth call. Mock for the MVP — any valid email + 8+ char pw works.
        Session.saveAuth(this, "mock-token-" + System.currentTimeMillis(), email);
        // Login path skips Personalize if already onboarded; otherwise go to Personalize.
        Intent next = Session.isOnboarded(this)
                ? new Intent(this, HomeActivity.class)
                : new Intent(this, PersonalizeActivity.class);
        next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(next);
        finish();
    }
}
