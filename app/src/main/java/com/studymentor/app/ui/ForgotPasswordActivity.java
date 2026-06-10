package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;

/**
 * UC1 — Forgot password.
 * Single Activity, two stacked sub-views (form / success). Mock backend.
 * Replace `mockSendResetLink()` with a real API call when wiring a backend.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private View viewForm, viewSuccess;
    private TextInputLayout   tilEmail;
    private TextInputEditText inputEmail;
    private TextView          textSuccessBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        viewForm        = findViewById(R.id.view_form);
        viewSuccess     = findViewById(R.id.view_success);
        tilEmail        = findViewById(R.id.til_email);
        inputEmail      = findViewById(R.id.input_email);
        textSuccessBody = findViewById(R.id.text_success_body);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v -> attemptSend());
        findViewById(R.id.btn_back_to_login).setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void attemptSend() {
        String email = String.valueOf(inputEmail.getText()).trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }
        tilEmail.setError(null);

        mockSendResetLink(email);
        showSuccess(email);
    }

    /** Real implementation would POST to /api/auth/forgot-password. */
    private void mockSendResetLink(String email) {
        // no-op for MVP
    }

    private void showSuccess(String email) {
        textSuccessBody.setText(getString(R.string.forgot_success_body, email));
        viewForm.setVisibility(View.GONE);
        viewSuccess.setVisibility(View.VISIBLE);
    }
}
