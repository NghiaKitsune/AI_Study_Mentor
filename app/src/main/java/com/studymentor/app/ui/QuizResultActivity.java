package com.studymentor.app.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.studymentor.app.R;
import com.studymentor.app.util.Session;

import java.util.LinkedHashMap;
import java.util.Map;

public class QuizResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE        = "extra_score";
    public static final String EXTRA_TOTAL        = "extra_total";
    public static final String EXTRA_SUBJECTS_CSV = "extra_subjects_csv";
    public static final String EXTRA_CORRECT_CSV  = "extra_correct_csv";

    private int score;
    private int total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        total = getIntent().getIntExtra(EXTRA_TOTAL, 1);
        Session.saveQuizResult(this, score, total);

        String subjectsCsv = getIntent().getStringExtra(EXTRA_SUBJECTS_CSV);
        String correctCsv  = getIntent().getStringExtra(EXTRA_CORRECT_CSV);
        int pct = total > 0 ? (score * 100 / total) : 0;

        setupHero(pct);
        setupStats(pct);
        if (subjectsCsv != null && correctCsv != null) {
            setupBreakdown(subjectsCsv.split(","), correctCsv.split(","));
        }

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> shareResult());
        findViewById(R.id.btn_try_again).setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });
        findViewById(R.id.btn_home).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
    }

    private void setupHero(int pct) {
        ((TextView) findViewById(R.id.text_score_num)).setText(String.valueOf(score));
        ((TextView) findViewById(R.id.text_score_denom)).setText("/ " + total);

        String label;
        if (pct >= 80)      label = "Nice work!";
        else if (pct >= 60) label = "Good effort!";
        else                label = "Keep practicing!";
        ((TextView) findViewById(R.id.text_result_label)).setText(label);

        int xp = Math.max(10, score * 10);
        ((TextView) findViewById(R.id.text_xp_pill)).setText("+" + xp + " XP");
    }

    private void setupStats(int pct) {
        int incorrect = total - score;
        ((TextView) findViewById(R.id.text_stat_correct)).setText(String.valueOf(score));
        ((TextView) findViewById(R.id.text_stat_incorrect)).setText(String.valueOf(incorrect));
        ((TextView) findViewById(R.id.text_stat_accuracy)).setText(pct + "%");
    }

    private void setupBreakdown(String[] subjects, String[] corrects) {
        Map<String, int[]> map = new LinkedHashMap<>();
        for (int i = 0; i < subjects.length && i < corrects.length; i++) {
            String s = subjects[i].trim();
            if (s.isEmpty()) continue;
            int[] tally = map.getOrDefault(s, new int[]{0, 0});
            tally[1]++;
            if ("1".equals(corrects[i].trim())) tally[0]++;
            map.put(s, tally);
        }

        LinearLayout container = findViewById(R.id.container_breakdown);
        container.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (Map.Entry<String, int[]> entry : map.entrySet()) {
            String subject = entry.getKey();
            int[] tally    = entry.getValue();
            View row = inf.inflate(R.layout.item_quiz_subject_row, container, false);

            GradientDrawable bar = new GradientDrawable();
            bar.setCornerRadius(4f);
            bar.setColor(subjectColor(subject));
            row.findViewById(R.id.view_bar).setBackground(bar);

            ((TextView) row.findViewById(R.id.text_subject)).setText(subjectLabel(subject));

            TextView tvFrac = row.findViewById(R.id.text_fraction);
            tvFrac.setText(tally[0] + "/" + tally[1]);
            tvFrac.setTextColor(tally[0] == tally[1]
                    ? getColor(R.color.color_ok)
                    : getColor(R.color.error));

            container.addView(row);
        }
    }

    private int subjectColor(String subject) {
        if (subject == null) return getColor(R.color.brand_primary);
        switch (subject) {
            case "math":    return getColor(R.color.subject_math);
            case "science": return getColor(R.color.subject_science);
            case "code":    return getColor(R.color.subject_code);
            case "history": return getColor(R.color.subject_history);
            default:        return getColor(R.color.brand_primary);
        }
    }

    private static String subjectLabel(String subject) {
        if (subject == null) return "General";
        switch (subject) {
            case "math":    return "Math";
            case "science": return "Science";
            case "code":    return "Coding";
            case "history": return "History";
            default:        return "General";
        }
    }

    private void shareResult() {
        int pct = total > 0 ? (score * 100 / total) : 0;
        String body = "I scored " + score + "/" + total + " (" + pct
                + "%) on a quiz in AI Study Mentor!\n\n— shared from AI Study Mentor";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(share, getString(R.string.action_share)));
    }
}
