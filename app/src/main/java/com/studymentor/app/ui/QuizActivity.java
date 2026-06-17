package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.studymentor.app.R;
import com.studymentor.app.data.QuizDataSource;
import com.studymentor.app.data.QuizQuestion;
import com.studymentor.app.util.BottomNavHelper;

import java.util.List;
import java.util.Locale;

/**
 * UC6 — Quiz active screen.
 * Loads 5 questions from QuizDataSource (assets/quiz_questions.json),
 * optionally filtered by EXTRA_SUBJECT. Tracks score across all questions
 * and passes it to QuizResultActivity.
 */
public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT = "extra_subject";

    private static final int[] OPTION_IDS      = {R.id.option_a, R.id.option_b, R.id.option_c, R.id.option_d};
    private static final int[] CIRCLE_IDS      = {R.id.circle_a, R.id.circle_b, R.id.circle_c, R.id.circle_d};
    private static final int[] OPTION_TEXT_IDS = {R.id.text_option_a, R.id.text_option_b, R.id.text_option_c, R.id.text_option_d};
    private static final String[] CIRCLE_LABELS = {"A", "B", "C", "D"};

    private List<QuizQuestion> questions;
    private int currentIdx  = 0;
    private int selectedIdx = -1;
    private boolean submitted = false;
    private int score = 0;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        String subject = getIntent().getStringExtra(EXTRA_SUBJECT);
        questions = QuizDataSource.random(this, subject, 5);
        if (questions.isEmpty()) { finish(); return; }

        showQuestion(0);
        startTimer();
        setupCta();

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        BottomNavHelper.setup(this, R.id.nav_practice);
    }

    private void showQuestion(int idx) {
        QuizQuestion q = questions.get(idx);

        ProgressBar pb = findViewById(R.id.progress_quiz);
        pb.setMax(questions.size());
        pb.setProgress(idx + 1);

        ((TextView) findViewById(R.id.text_question_label))
                .setText("QUESTION " + (idx + 1) + " / " + questions.size());

        String tag = (q.subjectTag != null && !q.subjectTag.isEmpty())
                ? q.subjectTag
                : q.subject.toUpperCase(Locale.US) + " · MULTIPLE CHOICE";
        ((TextView) findViewById(R.id.text_subject_tag)).setText(tag);
        ((ImageView) findViewById(R.id.img_subject)).setImageResource(subjectIcon(q.subject));

        ((TextView) findViewById(R.id.text_question)).setText(q.question);

        for (int i = 0; i < OPTION_IDS.length; i++) {
            final int fi = i;
            ((TextView) findViewById(OPTION_TEXT_IDS[i])).setText(q.options[i]);

            MaterialCardView card = findViewById(OPTION_IDS[i]);
            card.setCardBackgroundColor(getColor(R.color.surface));
            card.setStrokeColor(getColor(R.color.border));
            card.setOnClickListener(v -> { if (!submitted) selectOption(fi); });

            TextView circle = findViewById(CIRCLE_IDS[i]);
            circle.setBackgroundResource(R.drawable.bg_blob_primary_tint);
            circle.setText(CIRCLE_LABELS[i]);
            circle.setTextColor(getColor(R.color.text_secondary));
        }

        findViewById(R.id.card_explanation).setVisibility(View.GONE);

        MaterialButton btn = findViewById(R.id.btn_check);
        btn.setText("Check answer");
        btn.setEnabled(false);
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
        ((MaterialButton) findViewById(R.id.btn_check)).setEnabled(true);
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
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
            } else if (currentIdx < questions.size() - 1) {
                advanceQuestion();
            } else {
                openResult();
            }
        });
    }

    private void revealAnswer() {
        submitted = true;
        if (timer != null) timer.cancel();

        QuizQuestion q   = questions.get(currentIdx);
        int correctIdx   = q.correctIndex;
        if (selectedIdx == correctIdx) score++;

        for (int i = 0; i < OPTION_IDS.length; i++) {
            MaterialCardView card   = findViewById(OPTION_IDS[i]);
            TextView circle         = findViewById(CIRCLE_IDS[i]);
            if (i == correctIdx) {
                card.setCardBackgroundColor(getColor(R.color.success_soft));
                card.setStrokeColor(getColor(R.color.success));
                circle.setBackgroundColor(getColor(R.color.success));
                circle.setTextColor(getColor(android.R.color.white));
                circle.setText("✓");
            } else if (i == selectedIdx) {
                card.setCardBackgroundColor(getColor(R.color.error_soft));
                card.setStrokeColor(getColor(R.color.error));
                circle.setBackgroundColor(getColor(R.color.error));
                circle.setTextColor(getColor(android.R.color.white));
                circle.setText("✗");
            }
        }

        View expCard        = findViewById(R.id.card_explanation);
        expCard.setVisibility(View.VISIBLE);
        TextView resultLabel = expCard.findViewById(R.id.text_result_label);
        TextView textExpl    = expCard.findViewById(R.id.text_explanation);

        if (selectedIdx == -1) {
            resultLabel.setText("TIME'S UP");
            resultLabel.setTextColor(getColor(R.color.error));
        } else if (selectedIdx == correctIdx) {
            resultLabel.setText("CORRECT");
            resultLabel.setTextColor(getColor(R.color.success));
        } else {
            resultLabel.setText("INCORRECT");
            resultLabel.setTextColor(getColor(R.color.error));
        }
        if (q.explanation != null) textExpl.setText(q.explanation);

        MaterialButton btn = findViewById(R.id.btn_check);
        btn.setEnabled(true);
        btn.setText(currentIdx < questions.size() - 1 ? "Next question" : "See results");
    }

    private void advanceQuestion() {
        currentIdx++;
        selectedIdx = -1;
        submitted   = false;
        showQuestion(currentIdx);
        startTimer();
    }

    private void openResult() {
        Intent i = new Intent(this, QuizResultActivity.class);
        i.putExtra(QuizResultActivity.EXTRA_SCORE, score);
        i.putExtra(QuizResultActivity.EXTRA_TOTAL, questions.size());
        startActivity(i);
        finish();
    }

    private static int subjectIcon(String subject) {
        if (subject == null) return R.drawable.ic_sparkles;
        switch (subject) {
            case "science": return R.drawable.ic_target;
            case "code":    return R.drawable.ic_settings;
            case "history": return R.drawable.ic_book;
            default:        return R.drawable.ic_sparkles;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
