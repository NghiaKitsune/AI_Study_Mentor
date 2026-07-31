package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityQuizBinding;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.studymentor.app.R;
import com.studymentor.app.data.QuizQuestionEntity;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;
import com.studymentor.app.util.SubjectIcons;
import com.studymentor.app.viewmodel.QuizViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hybrid practice: Gemini-generated MCQ/input questions with the original timer and reveal UI. */
public class QuizActivity extends AppCompatActivity {
    private ActivityQuizBinding binding;
    public static final String EXTRA_SUBJECT = "extra_subject";
    private static final String[] LABELS = {"A", "B", "C", "D"};

    private QuizViewModel viewModel;
    private TextInputLayout inputLayout;
    private TextInputEditText input;
    private MaterialButton check;
    private View loading;
    private View content;
    private MaterialCardView[] optionCards;
    private TextView[] optionCircles;
    private TextView[] optionTexts;
    private QuizViewModel.State current;
    private long renderedQuestionId = -1L;
    private String selected = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loading = binding.layoutLoading;
        content = binding.layoutQuizContent;
        inputLayout = binding.answerInputLayout;
        input = binding.inputAnswer;
        check = binding.btnCheck;
        optionCards = new MaterialCardView[]{binding.optionA, binding.optionB,
                binding.optionC, binding.optionD};
        optionCircles = new TextView[]{binding.circleA, binding.circleB,
                binding.circleC, binding.circleD};
        optionTexts = new TextView[]{binding.textOptionA, binding.textOptionB,
                binding.textOptionC, binding.textOptionD};
        binding.btnClose.setOnClickListener(v -> finish());
        for (int i = 0; i < optionCards.length; i++) {
            final int option = i;
            optionCards[i].setOnClickListener(v -> selectOption(option));
        }
        check.setOnClickListener(v -> primaryAction());
        BottomNavHelper.setup(this, R.id.nav_practice);
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        viewModel.state().observe(this, this::render);
        viewModel.remainingMillis().observe(this, this::renderTimer);
        viewModel.initialize(Session.userId(this), getIntent().getStringExtra(EXTRA_SUBJECT));
    }

    private void render(QuizViewModel.State state) {
        current = state;
        if (state.error != null) {
            Toast.makeText(this, state.error, Toast.LENGTH_LONG).show();
            loading.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            binding.textQuestionLabel.setText(R.string.quiz_unavailable_title);
            binding.textQuestion.setText(state.error);
            binding.textQuestionHint.setText(R.string.quiz_unavailable_hint);
            inputLayout.setVisibility(View.GONE);
            for (MaterialCardView card : optionCards) card.setVisibility(View.GONE);
            binding.cardExplanation.setVisibility(View.GONE);
            check.setText(R.string.quiz_back_to_home);
            check.setEnabled(true);
            return;
        }
        if (state.complete && state.attempt != null) {
            Intent intent = new Intent(this, QuizResultActivity.class);
            intent.putExtra(QuizResultActivity.EXTRA_ATTEMPT_ID, state.attempt.id);
            startActivity(intent);
            finish();
            return;
        }
        loading.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        content.setVisibility(state.loading ? View.GONE : View.VISIBLE);
        check.setEnabled(!state.loading);
        if (state.question == null) return;

        if (renderedQuestionId != state.question.id) {
            renderedQuestionId = state.question.id;
            selected = "";
            input.setText("");
            bindQuestion(state);
        }
        if (state.answerChecked) reveal(state);
    }

    private void bindQuestion(QuizViewModel.State state) {
        ProgressBar progress = binding.progressQuiz;
        progress.setMax(state.total);
        progress.setProgress(state.index + 1);
        ((TextView) binding.textQuestionLabel).setText(
                getString(R.string.quiz_question_progress, state.index + 1, state.total));
        ((TextView) binding.textSubjectTag).setText(
                getString(R.string.quiz_subject_type, state.question.subject, state.question.type));
        ((ImageView) binding.imgSubject).setImageResource(
                SubjectIcons.forSubject(state.question.subject));
        ((TextView) binding.textQuestion).setText(state.question.questionText);

        boolean multipleChoice = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE.equals(state.question.type);
        inputLayout.setVisibility(multipleChoice ? View.GONE : View.VISIBLE);
        ((TextView) binding.textQuestionHint).setText(multipleChoice
                ? R.string.quiz_hint_choose : R.string.quiz_hint_input);
        List<String> options = multipleChoice ? viewModel.options(state.question) : new ArrayList<>();
        for (int i = 0; i < optionCards.length; i++) {
            boolean visible = i < options.size();
            MaterialCardView card = optionCards[i];
            card.setVisibility(visible ? View.VISIBLE : View.GONE);
            card.setEnabled(true);
            card.setAlpha(1f);
            card.setCardBackgroundColor(getColor(R.color.surface));
            card.setStrokeColor(getColor(R.color.border));
            TextView circle = optionCircles[i];
            circle.setText(LABELS[i]);
            circle.setTextColor(getColor(R.color.text_secondary));
            circle.setBackground(ovalDrawable(getColor(R.color.surface_2)));
            if (visible) optionTexts[i].setText(options.get(i));
        }
        input.setEnabled(true);
        binding.cardExplanation.setVisibility(View.GONE);
        check.setText(R.string.quiz_check_answer);
        check.setEnabled(!multipleChoice);
    }

    private void selectOption(int index) {
        if (current == null || current.answerChecked) return;
        selected = optionTexts[index].getText().toString();
        for (int i = 0; i < optionCards.length; i++) {
            MaterialCardView card = optionCards[i];
            boolean chosen = i == index;
            card.setCardBackgroundColor(getColor(chosen
                    ? R.color.brand_primary_tint : R.color.surface));
            card.setStrokeColor(getColor(chosen
                    ? R.color.brand_primary : R.color.border));
        }
        check.setEnabled(true);
    }

    private void primaryAction() {
        if (current == null) return;
        if (current.error != null) {
            finish();
            return;
        }
        if (current.question == null) return;
        if (current.answerChecked) {
            viewModel.next();
            return;
        }
        String answer = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE.equals(current.question.type)
                ? selected : String.valueOf(input.getText()).trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, R.string.quiz_error_answer_required, Toast.LENGTH_SHORT).show();
            return;
        }
        check.setEnabled(false);
        viewModel.submit(answer);
    }

    private void reveal(QuizViewModel.State state) {
        boolean multipleChoice = QuizQuestionEntity.TYPE_MULTIPLE_CHOICE.equals(state.question.type);
        if (multipleChoice) {
            List<String> options = viewModel.options(state.question);
            for (int i = 0; i < optionCards.length && i < options.size(); i++) {
                MaterialCardView card = optionCards[i];
                TextView circle = optionCircles[i];
                boolean correct = normalized(options.get(i)).equals(normalized(state.question.correctAnswer));
                boolean wrongSelection = normalized(options.get(i)).equals(
                        normalized(state.submittedAnswer)) && !correct;
                card.setEnabled(false);
                if (correct) {
                    card.setCardBackgroundColor(getColor(R.color.color_ok_soft));
                    card.setStrokeColor(getColor(R.color.color_ok));
                    circle.setBackground(ovalDrawable(getColor(R.color.color_ok)));
                    circle.setTextColor(Color.WHITE);
                    circle.setText(R.string.quiz_correct_symbol);
                } else if (wrongSelection) {
                    card.setCardBackgroundColor(getColor(R.color.error_soft));
                    card.setStrokeColor(getColor(R.color.error));
                    circle.setBackground(ovalDrawable(getColor(R.color.error)));
                    circle.setTextColor(Color.WHITE);
                    circle.setText(R.string.quiz_incorrect_symbol);
                } else card.setAlpha(0.48f);
            }
        }
        input.setEnabled(false);
        View explanation = binding.cardExplanation;
        explanation.setVisibility(View.VISIBLE);
        TextView label = binding.textResultLabel;
        label.setText(state.timedOut ? R.string.quiz_time_up
                : state.correct ? R.string.quiz_correct : R.string.quiz_incorrect);
        label.setTextColor(getColor(state.timedOut ? R.color.brand_primary_deep
                : state.correct ? R.color.color_ok : R.color.error));
        ((TextView) binding.textExplanation).setText(state.feedback);
        check.setEnabled(true);
        check.setText(state.index + 1 < state.total
                ? R.string.quiz_next_question : R.string.quiz_see_results);
    }

    private void setTimerNormal() {
        binding.timerPill.setBackgroundResource(R.drawable.bg_timer_normal);
        ((TextView) binding.textTimer).setTextColor(getColor(R.color.brand_primary_deep));
        ImageViewCompat.setImageTintList((ImageView) binding.imgTimer,
                ColorStateList.valueOf(getColor(R.color.brand_primary_deep)));
    }

    private void setTimerError() {
        binding.timerPill.setBackgroundResource(R.drawable.bg_timer_error);
        ((TextView) binding.textTimer).setTextColor(getColor(R.color.error));
        ImageViewCompat.setImageTintList((ImageView) binding.imgTimer,
                ColorStateList.valueOf(getColor(R.color.error)));
    }

    private void renderTimer(Long remainingMillis) {
        long value = remainingMillis == null ? 0L : Math.max(0L, remainingMillis);
        ((TextView) binding.textTimer).setText(
                com.studymentor.app.util.QuizTimerMath.formatRemaining(value));
        if (value <= 0L) setTimerError();
        else setTimerNormal();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static GradientDrawable ovalDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

}
