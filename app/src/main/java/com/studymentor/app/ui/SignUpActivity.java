package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivitySignUpBinding;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;
import com.studymentor.app.viewmodel.AuthViewModel;

import java.util.Arrays;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialCheckBox terms;
    private MaterialButton create;
    private View[] strengthBars;
    private AuthViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        emailLayout = binding.tilEmail;
        passwordLayout = binding.tilPassword;
        emailInput = binding.inputEmail;
        passwordInput = binding.inputPassword;
        terms = binding.checkTerms;
        create = binding.btnCreate;
        strengthBars = new View[]{binding.bar1, binding.bar2,
                binding.bar3, binding.bar4};
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnLogIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        binding.btnSsoGoogle.setOnClickListener(v -> localOnlyNotice());
        binding.btnSsoApple.setOnClickListener(v -> localOnlyNotice());
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) { updateStrength(editable.toString()); }
        });
        create.setOnClickListener(v -> attemptSignUp());
        viewModel.state().observe(this, state -> {
            create.setEnabled(state.status != AuthViewModel.AuthState.Status.LOADING);
            if (state.status == AuthViewModel.AuthState.Status.ERROR) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show();
                passwordInput.setText("");
                viewModel.clearTransientState();
            } else if (state.status == AuthViewModel.AuthState.Status.AUTHENTICATED) {
                Intent next = new Intent(this, PersonalizeActivity.class);
                next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(next);
            }
        });
    }

    private void localOnlyNotice() {
        Toast.makeText(this, R.string.auth_local_only, Toast.LENGTH_LONG).show();
    }

    private void updateStrength(String password) {
        int score = passwordScore(password);
        int[] tints = {R.color.error, R.color.warning, R.color.brand_primary, R.color.success};
        for (int i = 0; i < strengthBars.length; i++) {
            strengthBars[i].setBackgroundResource(i < score
                    ? tints[Math.max(0, score - 1)] : R.color.border);
        }
    }

    static int passwordScore(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    private void attemptSignUp() {
        String email = String.valueOf(emailInput.getText()).trim();
        String rawPassword = String.valueOf(passwordInput.getText());
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email_invalid));
            return;
        }
        emailLayout.setError(null);
        if (rawPassword.length() < 8 || passwordScore(rawPassword) < 2) {
            passwordLayout.setError(getString(R.string.auth_password_requirements));
            return;
        }
        passwordLayout.setError(null);
        if (!terms.isChecked()) {
            Toast.makeText(this, R.string.terms_agree, Toast.LENGTH_SHORT).show();
            return;
        }
        char[] password = rawPassword.toCharArray();
        viewModel.signUp(email, password);
        Arrays.fill(password, '\0');
    }
}
