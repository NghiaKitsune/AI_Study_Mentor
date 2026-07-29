package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityDashboardBinding;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.SubjectCount;
import com.studymentor.app.repository.ProgressRepository;
import com.studymentor.app.repository.RepositoryCallback;
import com.studymentor.app.util.Session;
import com.studymentor.app.util.SubjectIcons;

import java.util.ArrayList;
import java.util.List;

/** Dashboard computed exclusively from persisted learning events, attempts and questions. */
public class DashboardActivity extends AppCompatActivity {
    private ActivityDashboardBinding binding;
    private final SubjectAdapter subjectAdapter = new SubjectAdapter();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        RecyclerView subjects = binding.rvSubjects;
        subjects.setLayoutManager(new LinearLayoutManager(this));
        subjects.setAdapter(subjectAdapter);
        load();
    }

    private void load() {
        new ProgressRepository(this).load(Session.userId(this),
                new RepositoryCallback<ProgressRepository.Snapshot>() {
                    @Override public void onSuccess(ProgressRepository.Snapshot snapshot) {
                        ((TextView) binding.textStatQuestions)
                                .setText(String.valueOf(snapshot.totalQuestions));
                        ((TextView) binding.textStatXp)
                                .setText(String.valueOf(snapshot.xp));
                        ((TextView) binding.textStatAccuracy)
                                .setText(getString(R.string.dashboard_accuracy_value,
                                        snapshot.accuracy));
                        ((TextView) binding.textStatReviewTime)
                                .setText(formatReviewTime(snapshot.reviewSeconds));
                        ((TextView) binding.textStreak)
                                .setText(String.valueOf(snapshot.streak));
                        ((TextView) binding.textStreakBest).setText(
                                snapshot.streak == 0
                                        ? getString(R.string.dashboard_no_streak)
                                        : getString(R.string.dashboard_streak_active));
                        subjectAdapter.setItems(snapshot.subjects, snapshot.totalQuestions);
                        ((TextView) binding.textMiloInsight).setText(
                                getString(R.string.dashboard_local_insight,
                                        snapshot.totalQuestions, snapshot.accuracy,
                                        snapshot.streak));
                    }

                    @Override public void onError(String message, Throwable error) {
                        Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String formatReviewTime(long reviewSeconds) {
        long minutes = Math.max(0L, reviewSeconds / 60L);
        if (minutes < 60L) return getString(R.string.dashboard_review_minutes, minutes);
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        return getString(R.string.dashboard_review_hours, hours, remainder);
    }

    @Override protected void onResume() {
        super.onResume();
        if (Session.isLoggedIn(this)) load();
    }

    private static final class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.Holder> {
        private final List<SubjectCount> items = new ArrayList<>();
        private int total = 1;

        void setItems(List<SubjectCount> values, int totalQuestions) {
            items.clear();
            if (values != null) items.addAll(values);
            total = Math.max(1, totalQuestions);
            notifyDataSetChanged();
        }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_stat_row, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            SubjectCount item = items.get(position);
            String subject = item.subject == null || item.subject.isEmpty()
                    ? "general" : item.subject;
            holder.name.setText(holder.itemView.getContext().getString(nameForSubject(subject)));
            int percent = Math.round(item.count * 100f / total);
            holder.count.setText(holder.itemView.getResources().getQuantityString(
                    R.plurals.dashboard_subject_count, item.count, item.count, percent));
            holder.bar.setProgress(percent);
            int color = holder.itemView.getContext().getColor(colorForSubject(subject));
            holder.bar.setProgressTintList(ColorStateList.valueOf(color));
            holder.icon.setImageResource(SubjectIcons.forSubject(subject));
            holder.icon.setImageTintList(ColorStateList.valueOf(color));
        }

        @Override public int getItemCount() { return items.size(); }

        private static int nameForSubject(String subject) {
            if ("math".equals(subject)) return R.string.subject_math;
            if ("science".equals(subject)) return R.string.subject_science;
            if ("code".equals(subject)) return R.string.subject_code;
            if ("history".equals(subject)) return R.string.subject_history;
            if ("language".equals(subject)) return R.string.subject_language;
            if ("geography".equals(subject)) return R.string.subject_geo;
            return R.string.subject_general;
        }

        private static int colorForSubject(String subject) {
            if ("math".equals(subject)) return R.color.subject_math;
            if ("science".equals(subject)) return R.color.subject_science;
            if ("code".equals(subject)) return R.color.subject_code;
            if ("history".equals(subject)) return R.color.subject_history;
            if ("language".equals(subject)) return R.color.subject_language;
            return R.color.info;
        }

        private static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView count;
            final ProgressBar bar;

            Holder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.img_subject);
                name = itemView.findViewById(R.id.text_subject_name);
                count = itemView.findViewById(R.id.text_subject_count);
                bar = itemView.findViewById(R.id.bar_subject);
            }
        }
    }
}
