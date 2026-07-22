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
import com.studymentor.app.api.GroqAiService;
import com.studymentor.app.util.Session;

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

        loadDbAndBind();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SettingsActivity.class)));
    }

    /** One background pass for all DB reads; drives bindLiveStats, bindSubjects, bindMiloInsight. */
    private void loadDbAndBind() {
        final int streak      = Session.streak(this);
        final int xp          = Session.xp(this);
        final String lvTitle  = Session.levelTitle(this);
        final int bestQuizPct = Session.bestQuizPct(this);

        StudyMentorApp.get().executor().execute(() -> {
            int questionCount = StudyMentorApp.get().db().questionDao().count();
            int mathCount     = StudyMentorApp.get().db().questionDao().countBySubject("math");
            int codeCount     = StudyMentorApp.get().db().questionDao().countBySubject("code");
            int scienceCount  = StudyMentorApp.get().db().questionDao().countBySubject("science");
            int historyCount  = StudyMentorApp.get().db().questionDao().countBySubject("history");

            String summary = "Student stats — total questions asked: " + questionCount
                    + ", current streak: " + streak + " day(s)"
                    + ", XP: " + xp + " (" + lvTitle + " level)"
                    + ", best quiz score: " + bestQuizPct + "%"
                    + ", subject breakdown: math=" + mathCount + ", science=" + scienceCount
                    + ", code=" + codeCount + ", history=" + historyCount + ".";

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                bindLiveStats(questionCount, streak);
                bindSubjects(mathCount, codeCount, scienceCount, historyCount);
                bindMiloInsight(summary);
            });
        });
    }

    private void bindLiveStats(int questionCount, int streak) {
        TextView tvQ = findViewById(R.id.text_stat_questions);
        if (tvQ != null) tvQ.setText(String.valueOf(questionCount));
        TextView tvStreak = findViewById(R.id.text_streak);
        if (tvStreak != null) tvStreak.setText(String.valueOf(streak));
    }

    private void bindSubjects(int mathCount, int codeCount, int scienceCount, int historyCount) {
        int total = Math.max(mathCount + codeCount + scienceCount + historyCount, 1);
        List<SubjectStat> subjects = Arrays.asList(
            new SubjectStat("Math",      mathCount,    mathCount    * 100 / total, R.color.subject_math,     R.drawable.ic_sparkles, R.color.subject_math_soft),
            new SubjectStat("Coding",    codeCount,    codeCount    * 100 / total, R.color.subject_code,     R.drawable.ic_settings, R.color.subject_code_soft),
            new SubjectStat("Science",   scienceCount, scienceCount * 100 / total, R.color.subject_science,  R.drawable.ic_target,   R.color.subject_science_soft),
            new SubjectStat("Languages", historyCount, historyCount * 100 / total, R.color.subject_language, R.drawable.ic_book,     R.color.subject_language_soft)
        );
        RecyclerView rv = findViewById(R.id.rv_subjects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SubjectAdapter(subjects));
    }

    /**
     * Shows a cached insight if one was already generated today; otherwise
     * asks Groq for a fresh 1-2 sentence take on the student's stats and
     * caches it via {@link Session#saveInsight}. Falls back to a static
     * string on network/parse failure so the card never looks broken.
     */
    private void bindMiloInsight(String statsSummary) {
        TextView tvInsight = findViewById(R.id.text_milo_insight);
        if (tvInsight == null) return;

        if (Session.hasFreshInsight(this)) {
            tvInsight.setText(Session.cachedInsight(this));
            return;
        }

        tvInsight.setText(R.string.dashboard_insight_loading);
        new GroqAiService().quickInsight(statsSummary, new GroqAiService.InsightCallback() {
            @Override
            public void onSuccess(String insight) {
                if (isFinishing() || isDestroyed()) return;
                Session.saveInsight(DashboardActivity.this, insight);
                tvInsight.setText(insight);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                tvInsight.setText(R.string.dashboard_insight_fallback);
            }
        });
    }


    static class SubjectStat {
        final String name;
        final int count;
        final int pct;
        final int colorRes;
        final int iconRes;
        final int iconBgColorRes;
        SubjectStat(String n, int c, int p, int cr, int ir, int ibcr) {
            name = n; count = c; pct = p; colorRes = cr; iconRes = ir; iconBgColorRes = ibcr;
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
            h.icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    h.itemView.getContext().getColor(s.iconBgColorRes)));
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
