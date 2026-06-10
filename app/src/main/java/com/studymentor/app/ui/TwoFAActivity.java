package com.studymentor.app.ui;

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

    private static final int[] OTP_IDS = {
        R.id.otp_1, R.id.otp_2, R.id.otp_3, R.id.otp_4, R.id.otp_5, R.id.otp_6
    };
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_fa);

        setupOtpBoxes();
        startCountdown();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_resend).setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            startCountdown();
            Toast.makeText(this, "New code sent!", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_verify).setOnClickListener(v -> onVerify());
    }

    private void setupOtpBoxes() {
        EditText[] boxes = new EditText[OTP_IDS.length];
        for (int i = 0; i < OTP_IDS.length; i++) boxes[i] = findViewById(OTP_IDS[i]);

        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            final EditText[] bs = boxes;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < bs.length - 1) {
                        bs[idx + 1].requestFocus();
                    }
                }
            });
        }
    }

    private void startCountdown() {
        TextView tvExpiry = findViewById(R.id.text_expiry);
        timer = new CountDownTimer(30_000, 1_000) {
            @Override public void onTick(long ms) {
                int s = (int) (ms / 1000);
                tvExpiry.setText("Code expires in 0:" + String.format(Locale.US, "%02d", s));
            }
            @Override public void onFinish() {
                tvExpiry.setText("Code expired");
            }
        }.start();
    }

    private void onVerify() {
        StringBuilder code = new StringBuilder();
        for (int id : OTP_IDS) {
            code.append(((EditText) findViewById(id)).getText().toString().trim());
        }
        if (code.length() < 6) {
            Toast.makeText(this, "Enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        // Mock success
        Toast.makeText(this, "2FA enabled successfully!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
