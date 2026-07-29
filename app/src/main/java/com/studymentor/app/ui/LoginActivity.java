package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityLoginBinding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;
import com.studymentor.app.viewmodel.AuthViewModel;

import java.util.Arrays;

/** Device-local Room account login with salted PBKDF2 password verification. */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton login;
    private AuthViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        emailLayout = binding.tilEmail;
        passwordLayout = binding.tilPassword;
        emailInput = binding.inputEmail;
        passwordInput = binding.inputPassword;
        login = binding.btnLogin;
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.btnClose.setOnClickListener(v -> finish());
        login.setOnClickListener(v -> attemptLogin());
        binding.btnForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
        viewModel.state().observe(this, state -> {
            login.setEnabled(state.status != AuthViewModel.AuthState.Status.LOADING);
            if (state.status == AuthViewModel.AuthState.Status.ERROR) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show();
                passwordInput.setText("");
                viewModel.clearTransientState();
            } else if (state.status == AuthViewModel.AuthState.Status.AUTHENTICATED) {
                Intent next = new Intent(this, state.result.onboardingComplete
                        ? HomeActivity.class : PersonalizeActivity.class);
                next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(next);
            }
        });
    }

    private void attemptLogin() {
        String email = String.valueOf(emailInput.getText()).trim();
        String rawPassword = String.valueOf(passwordInput.getText());
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email_invalid));
            return;
        }
        emailLayout.setError(null);
        if (rawPassword.length() < 8) {
            passwordLayout.setError(getString(R.string.error_password_short));
            return;
        }
        passwordLayout.setError(null);
        char[] password = rawPassword.toCharArray();
        viewModel.login(email, password);
        Arrays.fill(password, '\0');
    }
}
