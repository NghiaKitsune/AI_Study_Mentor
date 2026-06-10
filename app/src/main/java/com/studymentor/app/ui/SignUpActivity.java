package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * UC1 — Sign Up.
 * - Validates email + password length.
 * - 4-bar strength meter updates as user types.
 * - On success, fakes auth (no backend yet) and goes to Personalize.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText inputEmail, inputPassword;
    private MaterialCheckBox checkTerms;
    private MaterialButton btnCreate;
    private View[] strengthBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        tilEmail      = findViewById(R.id.til_email);
        tilPassword   = findViewById(R.id.til_password);
        inputEmail    = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        checkTerms    = findViewById(R.id.check_terms);
        btnCreate     = findViewById(R.id.btn_create);

        strengthBars = new View[]{
                findViewById(R.id.bar_1),
                findViewById(R.id.bar_2),
                findViewById(R.id.bar_3),
                findViewById(R.id.bar_4)
        };

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_log_in).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateStrength(s.toString()); }
        });

        btnCreate.setOnClickListener(v -> attemptSignUp());
    }

    /** Score 0..4 — length, mixed case, digit, symbol. */
    private void updateStrength(String pw) {
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[A-Z].*") && pw.matches(".*[a-z].*")) score++;
        if (pw.matches(".*\\d.*")) score++;
        if (pw.matches(".*[^A-Za-z0-9].*")) score++;

        int[] tints = {
                R.color.error,
                R.color.warning,
                R.color.brand_primary,
                R.color.success
        };
        for (int i = 0; i < strengthBars.length; i++) {
            int color = i < score ? tints[Math.min(score - 1, 3)] : R.color.border;
            strengthBars[i].setBackgroundResource(color);
        }
    }

    private void attemptSignUp() {
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

        if (!checkTerms.isChecked()) {
            Toast.makeText(this, R.string.terms_agree, Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: replace with a real auth call. Mock for the MVP.
        Session.saveAuth(this, "mock-token-" + System.currentTimeMillis(), email);

        startActivity(new Intent(this, PersonalizeActivity.class));
        finish();
    }
}
