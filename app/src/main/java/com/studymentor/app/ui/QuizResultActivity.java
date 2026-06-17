package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;

import java.util.Arrays;
import java.util.List;

/**
 * UC6 — Quiz result screen.
 * Score hero, XP/streak/badge rewards, per-question breakdown.
 */
public class QuizResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_TOTAL = "extra_total";

    private int score;
    private int total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        total = getIntent().getIntExtra(EXTRA_TOTAL, 1);
        com.studymentor.app.util.Session.saveQuizResult(this, score, total);
        int pct = total > 0 ? (score * 100 / total) : 0;

        ((TextView) findViewById(R.id.text_score)).setText(score + " / " + total);
        ((TextView) findViewById(R.id.text_accuracy)).setText(pct + "% accuracy · Quiz result");
        int streak = com.studymentor.app.util.Session.streak(this);
        ((TextView) findViewById(R.id.text_streak)).setText(String.valueOf(streak));

        setupAnswerList();

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_done).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        findViewById(R.id.btn_next_quiz).setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });
        findViewById(R.id.btn_share).setOnClickListener(v -> shareResult());
    }

    private void setupAnswerList() {
        List<QuizAnswer> answers = Arrays.asList(
            new QuizAnswer("What is Newton's first law?", true),
            new QuizAnswer("Define acceleration", true),
            new QuizAnswer("What is a force in physics?", true),
            new QuizAnswer("Unit of force is...", false),
            new QuizAnswer("Gravity on the Moon vs Earth", true)
        );

        RecyclerView rv = findViewById(R.id.rv_answers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new AnswerAdapter(answers));
    }

    private void shareResult() {
        int pct = total > 0 ? (score * 100 / total) : 0;
        String body = "I scored " + score + "/" + total + " (" + pct + "%) on a quiz in AI Study Mentor!\n\n— shared from AI Study Mentor";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(share, getString(R.string.action_share)));
    }

    static class QuizAnswer {
        final String question;
        final boolean correct;
        QuizAnswer(String q, boolean c) { question = q; correct = c; }
    }

    static class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.VH> {
        private final List<QuizAnswer> items;
        AnswerAdapter(List<QuizAnswer> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz_answer_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            QuizAnswer a = items.get(pos);
            h.question.setText(a.question);
            h.icon.setImageResource(a.correct ? R.drawable.ic_check : R.drawable.ic_close);
            int tint = a.correct
                ? h.itemView.getContext().getColor(R.color.success)
                : h.itemView.getContext().getColor(R.color.error);
            h.icon.getDrawable().setTint(tint);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView question;
            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.icon_result);
                question = v.findViewById(R.id.text_question);
            }
        }
    }
}
