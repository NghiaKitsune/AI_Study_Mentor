package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.studymentor.app.R;
import com.studymentor.app.util.BottomNavHelper;

import java.util.Locale;

/**
 * UC6 — Quiz active screen.
 * Multiple choice with timer, option reveal (correct/wrong), and Milo explanation.
 */
public class QuizActivity extends AppCompatActivity {

    private static final int[] OPTION_IDS = {
        R.id.option_a, R.id.option_b, R.id.option_c, R.id.option_d
    };
    private static final int[] CIRCLE_IDS = {
        R.id.circle_a, R.id.circle_b, R.id.circle_c, R.id.circle_d
    };
    private static final int[] OPTION_TEXT_IDS = {
        R.id.text_option_a, R.id.text_option_b, R.id.text_option_c, R.id.text_option_d
    };
    private static final String[] OPTION_TEXTS = {
        "The process by which plants make food using sunlight",
        "A chemical reaction that releases energy in animals",
        "Conversion of light energy into chemical energy stored in glucose",
        "Absorption of minerals from soil through root hairs"
    };
    private static final int CORRECT_IDX = 2; // option C is correct

    private int selectedIdx = -1;
    private boolean submitted = false;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        setupQuestion();
        setupOptions();
        setupTimer();
        setupCta();

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        BottomNavHelper.setup(this, R.id.nav_practice);
    }

    private void setupQuestion() {
        TextView label = findViewById(R.id.text_question_label);
        label.setText("QUESTION 3 / 5");
        ((android.widget.ProgressBar) findViewById(R.id.progress_quiz)).setProgress(3);
        TextView question = findViewById(R.id.text_question);
        if (question != null) question.setText("What is photosynthesis?");
    }

    private void setupOptions() {
        for (int i = 0; i < OPTION_IDS.length; i++) {
            final int idx = i;
            MaterialCardView card = findViewById(OPTION_IDS[i]);
            card.setOnClickListener(v -> {
                if (!submitted) selectOption(idx);
            });
            ((TextView) findViewById(OPTION_TEXT_IDS[i])).setText(OPTION_TEXTS[i]);
        }
    }

    private void selectOption(int idx) {
        selectedIdx = idx;
        for (int i = 0; i < OPTION_IDS.length; i++) {
            MaterialCardView card = findViewById(OPTION_IDS[i]);
            boolean sel = (i == idx);
            card.setCardBackgroundColor(sel
                ? getColor(R.color.brand_primary_tint)
                : getColor(R.color.surface));
            card.setStrokeColor(sel
                ? getColor(R.color.brand_primary)
                : getColor(R.color.border));
        }
        MaterialButton btn = findViewById(R.id.btn_check);
        btn.setEnabled(true);
    }

    private void setupTimer() {
        TextView tvTimer = findViewById(R.id.text_timer);
        timer = new CountDownTimer(24_000, 1_000) {
            @Override public void onTick(long ms) {
                int secs = (int) (ms / 1000);
                tvTimer.setText(String.format(Locale.US, "0:%02d", secs));
            }
            @Override public void onFinish() {
                tvTimer.setText("0:00");
                if (!submitted) revealAnswer();
            }
        }.start();
    }

    private void setupCta() {
        MaterialButton btn = findViewById(R.id.btn_check);
        btn.setEnabled(false);
        btn.setOnClickListener(v -> {
            if (!submitted) {
                revealAnswer();
            } else {
                openResult();
            }
        });
    }

    private void revealAnswer() {
        submitted = true;
        if (timer != null) timer.cancel();

        for (int i = 0; i < OPTION_IDS.length; i++) {
            MaterialCardView card = findViewById(OPTION_IDS[i]);
            TextView circle = findViewById(CIRCLE_IDS[i]);
            boolean isCorrect = (i == CORRECT_IDX);
            boolean isWrong = (i == selectedIdx && i != CORRECT_IDX);

            if (isCorrect) {
                card.setCardBackgroundColor(getColor(R.color.success_soft));
                card.setStrokeColor(getColor(R.color.success));
                circle.setBackgroundColor(getColor(R.color.success));
                circle.setTextColor(getColor(android.R.color.white));
                circle.setText("✓");
            } else if (isWrong) {
                card.setCardBackgroundColor(getColor(R.color.error_soft));
                card.setStrokeColor(getColor(R.color.error));
                circle.setBackgroundColor(getColor(R.color.error));
                circle.setTextColor(getColor(android.R.color.white));
                circle.setText("✗");
            }
        }

        // Show explanation
        View explanation = findViewById(R.id.card_explanation);
        explanation.setVisibility(View.VISIBLE);

        TextView resultLabel = explanation.findViewById(R.id.text_result_label);
        if (selectedIdx == CORRECT_IDX) {
            resultLabel.setText("CORRECT");
            resultLabel.setTextColor(getColor(R.color.success));
        } else {
            resultLabel.setText("INCORRECT");
            resultLabel.setTextColor(getColor(R.color.error));
        }

        MaterialButton btn = findViewById(R.id.btn_check);
        btn.setText("Next question");
        btn.setEnabled(true);
    }

    private void openResult() {
        startActivity(new Intent(this, QuizResultActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
