package com.studymentor.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.UserAchievement;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.Holder> {
    private final List<UserAchievement> items = new ArrayList<>();

    public void setItems(List<UserAchievement> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_cell, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        String key = items.get(position).achievementKey;
        holder.label.setText(label(key));
        holder.description.setText(description(key));
        holder.icon.setImageResource(icon(key));
    }

    @Override public int getItemCount() { return items.size(); }

    private static String label(String key) {
        switch (key) {
            case "FIRST_QUESTION": return "First Question";
            case "QUESTION_10": return "Curious Mind";
            case "BOOKMARK_5": return "Knowledge Keeper";
            case "FIRST_QUIZ": return "Quiz Starter";
            case "QUIZ_ACCURACY_80": return "Sharp Shooter";
            case "STREAK_3": return "Study Streak";
            default: return "Achievement";
        }
    }

    private static String description(String key) {
        switch (key) {
            case "FIRST_QUESTION": return "Asked one question";
            case "QUESTION_10": return "Asked 10 questions";
            case "BOOKMARK_5": return "Saved 5 answers";
            case "FIRST_QUIZ": return "Finished a quiz";
            case "QUIZ_ACCURACY_80": return "80% quiz accuracy";
            case "STREAK_3": return "3-day activity streak";
            default: return "Unlocked from real progress";
        }
    }

    private static int icon(String key) {
        if (key.contains("QUIZ")) return R.drawable.ic_medal;
        if (key.contains("STREAK")) return R.drawable.ic_flame;
        if (key.contains("BOOKMARK")) return R.drawable.ic_bookmark;
        return R.drawable.ic_sparkles;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView description;
        Holder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.badge_icon);
            label = itemView.findViewById(R.id.badge_label);
            description = itemView.findViewById(R.id.badge_desc);
        }
    }
}
