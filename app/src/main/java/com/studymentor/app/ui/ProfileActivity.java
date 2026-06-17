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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;

import java.util.Arrays;
import java.util.List;

/**
 * UC8 — Profile + Achievements.
 * Hero avatar | XP bar | Stats row | Badge grid | Activity feed
 */
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        bindProfile();
        bindBadges();
        bindActivity();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        BottomNavHelper.setup(this, R.id.nav_profile);
    }

    private void bindProfile() {
        String name = Session.name(this);
        if (name == null || name.isEmpty() || name.equals("Friend")) name = "Nghia Mentor";

        ((TextView) findViewById(R.id.text_profile_name)).setText(name);
    }

    private void bindBadges() {
        int streak       = Session.streak(this);
        int qCount       = StudyMentorApp.get().db().questionDao().count();
        int bookmarks    = StudyMentorApp.get().db().questionDao().bookmarkedCount();
        int mathCount    = StudyMentorApp.get().db().questionDao().countBySubject("math");
        int bestQuizPct  = Session.bestQuizPct(this);

        List<BadgeItem> badges = Arrays.asList(
            new BadgeItem(R.drawable.ic_flame,    "Week Warrior",  "7-day streak",       streak >= 7,       R.color.brand_accent),
            new BadgeItem(R.drawable.ic_sparkles, "First Steps",   "Ask 1 question",     qCount >= 1,       R.color.subject_math),
            new BadgeItem(R.drawable.ic_target,   "Sharp Shooter", "Score 100% on quiz", bestQuizPct == 100, R.color.subject_science),
            new BadgeItem(R.drawable.ic_book,     "Bookworm",      "Bookmark 3+ answers",bookmarks >= 3,    R.color.info),
            new BadgeItem(R.drawable.ic_trophy,   "Top 10",        "Weekly leaderboard", false,             R.color.brand_primary),
            new BadgeItem(R.drawable.ic_crown,    "Math Master",   "10+ math questions", mathCount >= 10,   R.color.subject_history),
            new BadgeItem(R.drawable.ic_zap,      "Speed Demon",   "Quiz in < 30s",      false,             R.color.warning),
            new BadgeItem(R.drawable.ic_medal,    "Marathon",      "30-day streak",       streak >= 30,      R.color.subject_math)
        );

        RecyclerView rv = findViewById(R.id.rv_badges);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        rv.setAdapter(new BadgeAdapter(badges));
    }

    private void bindActivity() {
        List<ActivityItem> items = Arrays.asList(
            new ActivityItem(R.drawable.ic_medal,  "Earned \"Sharp Shooter\" badge", "10m",   R.color.subject_science),
            new ActivityItem(R.drawable.ic_target, "Completed Physics quiz (80%)",   "12m",   R.color.brand_primary),
            new ActivityItem(R.drawable.ic_flame,  "7-day streak — keep going!",     "Today", R.color.brand_accent),
            new ActivityItem(R.drawable.ic_zap,    "Reached Level 7 · Algebra Apprentice", "2d", R.color.brand_primary)
        );

        RecyclerView rv = findViewById(R.id.rv_activity);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ActivityAdapter(items));
    }

    static class BadgeItem {
        final int icon; final String label, desc; final boolean unlocked; final int colorRes;
        BadgeItem(int i, String l, String d, boolean u, int c) { icon=i; label=l; desc=d; unlocked=u; colorRes=c; }
    }

    static class ActivityItem {
        final int icon; final String text, time; final int colorRes;
        ActivityItem(int i, String t, String tm, int c) { icon=i; text=t; time=tm; colorRes=c; }
    }

    static class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.VH> {
        private final List<BadgeItem> items;
        BadgeAdapter(List<BadgeItem> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_badge_cell, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BadgeItem b = items.get(pos);
            h.icon.setImageResource(b.icon);
            h.label.setText(b.label);
            h.desc.setText(b.desc);
            int color = h.itemView.getContext().getColor(b.colorRes);
            if (b.unlocked) {
                h.icon.getDrawable().setTint(color);
                h.itemView.setAlpha(1f);
            } else {
                h.icon.getDrawable().setTint(h.itemView.getContext().getColor(R.color.text_tertiary));
                h.itemView.setAlpha(0.55f);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon; final TextView label, desc;
            VH(@NonNull View v) { super(v); icon=v.findViewById(R.id.badge_icon); label=v.findViewById(R.id.badge_label); desc=v.findViewById(R.id.badge_desc); }
        }
    }

    static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.VH> {
        private final List<ActivityItem> items;
        ActivityAdapter(List<ActivityItem> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_activity_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ActivityItem a = items.get(pos);
            h.icon.setImageResource(a.icon);
            int color = h.itemView.getContext().getColor(a.colorRes);
            h.icon.getDrawable().setTint(color);
            h.text.setText(a.text);
            h.time.setText(a.time);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon; final TextView text, time;
            VH(@NonNull View v) { super(v); icon=v.findViewById(R.id.img_icon); text=v.findViewById(R.id.text_activity); time=v.findViewById(R.id.text_time); }
        }
    }
}
