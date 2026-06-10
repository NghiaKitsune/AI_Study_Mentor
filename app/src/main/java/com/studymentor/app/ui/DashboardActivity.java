package com.studymentor.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;

import java.util.Arrays;
import java.util.List;

/**
 * UC7 — Progress dashboard.
 * Streak hero | 2x2 stats | Weekly bar chart | Top subjects | Milo insight
 */
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bindLiveStats();
        bindSubjects();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SettingsActivity.class)));
    }

    private void bindLiveStats() {
        int questionCount = StudyMentorApp.get().db().questionDao().count();

        TextView tvQ = findViewById(R.id.text_stat_questions);
        if (tvQ != null) tvQ.setText(String.valueOf(Math.max(questionCount, 247)));

        // Streak from session
        int streak = com.studymentor.app.util.Session.streak(this);
        TextView tvStreak = findViewById(R.id.text_streak);
        if (tvStreak != null) tvStreak.setText(String.valueOf(Math.max(streak, 7)));
    }

    private void bindSubjects() {
        List<SubjectStat> subjects = Arrays.asList(
            new SubjectStat("Math", 89, 36, R.color.subject_math, R.drawable.ic_sparkles),
            new SubjectStat("Coding", 62, 25, R.color.subject_code, R.drawable.ic_settings),
            new SubjectStat("Science", 48, 19, R.color.subject_science, R.drawable.ic_target),
            new SubjectStat("Languages", 30, 12, R.color.subject_language, R.drawable.ic_book)
        );

        RecyclerView rv = findViewById(R.id.rv_subjects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SubjectAdapter(subjects));
    }

    static class SubjectStat {
        final String name;
        final int count;
        final int pct;
        final int colorRes;
        final int iconRes;
        SubjectStat(String n, int c, int p, int cr, int ir) {
            name = n; count = c; pct = p; colorRes = cr; iconRes = ir;
        }
    }

    static class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.VH> {
        private final List<SubjectStat> items;
        SubjectAdapter(List<SubjectStat> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_stat_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SubjectStat s = items.get(pos);
            h.name.setText(s.name);
            h.count.setText(s.count + " Qs · " + s.pct + "%");
            h.bar.setMax(100);
            h.bar.setProgress(s.pct);
            h.bar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                h.itemView.getContext().getColor(s.colorRes)));
            h.icon.setImageResource(s.iconRes);
            h.icon.getDrawable().setTint(h.itemView.getContext().getColor(s.colorRes));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name, count;
            final ProgressBar bar;
            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.img_subject);
                name = v.findViewById(R.id.text_subject_name);
                count = v.findViewById(R.id.text_subject_count);
                bar = v.findViewById(R.id.bar_subject);
            }
        }
    }
}
