package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityOnboardingBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * First-launch onboarding carousel — 3 steps before SignUp.
 * Shown only once (guarded by Session.hasSeenOnboarding).
 */
public class OnboardingActivity extends AppCompatActivity {
    private ActivityOnboardingBinding binding;

    private static final int[] TAGS   = { R.string.onboard_tag_0, R.string.onboard_tag_1, R.string.onboard_tag_2 };
    private static final int[] TITLES = { R.string.onboard_title_0, R.string.onboard_title_1, R.string.onboard_title_2 };
    private static final int[] BODIES = { R.string.onboard_body_0, R.string.onboard_body_1, R.string.onboard_body_2 };

    private int currentStep = 0;
    private TextView tvTag, tvTitle, tvBody;
    private MaterialButton btnNext;
    private View dot0, dot1, dot2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tvTag   = binding.textOnboardTag;
        tvTitle = binding.textOnboardTitle;
        tvBody  = binding.textOnboardBody;
        btnNext = binding.btnNext;
        dot0    = binding.dot0;
        dot1    = binding.dot1;
        dot2    = binding.dot2;

        updateStep();

        btnNext.setOnClickListener(v -> {
            if (currentStep < TITLES.length - 1) {
                currentStep++;
                updateStep();
            } else {
                finish();
            }
        });

        binding.btnSkip.setOnClickListener(v -> finish());
    }

    private void updateStep() {
        tvTag.setText(TAGS[currentStep]);
        tvTitle.setText(TITLES[currentStep]);
        tvBody.setText(BODIES[currentStep]);

        boolean isLast = currentStep == TITLES.length - 1;
        btnNext.setText(isLast ? getString(R.string.onboard_btn_get_started) : getString(R.string.onboard_btn_next));

        updateDots();
    }

    private void updateDots() {
        View[] dots = { dot0, dot1, dot2 };
        for (int i = 0; i < dots.length; i++) {
            android.view.ViewGroup.LayoutParams lp = dots[i].getLayoutParams();
            if (i == currentStep) {
                lp.width = (int) (24 * getResources().getDisplayMetrics().density);
                dots[i].setBackgroundResource(R.drawable.bg_dot_active);
            } else {
                lp.width = (int) (8 * getResources().getDisplayMetrics().density);
                dots[i].setBackgroundResource(R.drawable.bg_dot_inactive);
            }
            dots[i].setLayoutParams(lp);
        }
    }

    @Override
    public void finish() {
        Session.markOnboardingSeen(this);
        startActivity(new Intent(this, SignUpActivity.class));
        super.finish();
    }
}
