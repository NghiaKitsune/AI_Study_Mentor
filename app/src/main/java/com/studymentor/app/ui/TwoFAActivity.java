package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityTwoFaBinding;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.studymentor.app.R;

import java.util.Locale;

/**
 * UC10 — 2FA Setup: 6-digit OTP verification screen.
 * Auto-advances focus between OTP boxes; countdown timer; Verify button.
 */
public class TwoFAActivity extends AppCompatActivity {
    private ActivityTwoFaBinding binding;

    private CountDownTimer timer;
    private EditText[] otpBoxes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTwoFaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        otpBoxes = new EditText[]{binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6};

        setupOtpBoxes();
        startCountdown();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnResend.setOnClickListener(v -> {
            Toast.makeText(this, R.string.two_fa_backend_required, Toast.LENGTH_LONG).show();
        });
        binding.btnVerify.setOnClickListener(v -> onVerify());
    }

    private void setupOtpBoxes() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int idx = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < otpBoxes.length - 1) {
                        otpBoxes[idx + 1].requestFocus();
                    }
                }
            });
        }
    }

    private void startCountdown() {
        TextView tvExpiry = binding.textExpiry;
        timer = new CountDownTimer(30_000, 1_000) {
            @Override public void onTick(long ms) {
                int s = (int) (ms / 1000);
                tvExpiry.setText(getString(R.string.two_fa_expires,
                        String.format(Locale.US, "%02d", s)));
            }
            @Override public void onFinish() {
                tvExpiry.setText(R.string.two_fa_expired);
            }
        }.start();
    }

    private void onVerify() {
        StringBuilder code = new StringBuilder();
        for (EditText box : otpBoxes) {
            code.append(box.getText().toString().trim());
        }
        if (code.length() < 6) {
            Toast.makeText(this, R.string.two_fa_enter_all_digits,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.two_fa_backend_required, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
