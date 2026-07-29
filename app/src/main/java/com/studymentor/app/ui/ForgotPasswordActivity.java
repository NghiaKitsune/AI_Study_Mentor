package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityForgotPasswordBinding;

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
 * Password reset requires a trusted backend and is never simulated locally.
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;

    private View viewForm, viewSuccess;
    private TextInputLayout   tilEmail;
    private TextInputEditText inputEmail;
    private TextView          textSuccessBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewForm        = binding.viewForm;
        viewSuccess     = binding.viewSuccess;
        tilEmail        = binding.tilEmail;
        inputEmail      = binding.inputEmail;
        textSuccessBody = binding.textSuccessBody;

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> attemptSend());
        binding.btnBackToLogin.setOnClickListener(v -> {
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

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.local_account_limit_title)
                .setMessage(R.string.forgot_local_limit)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
