package com.studymentor.app.ui.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.Question;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecentQuestionAdapter extends RecyclerView.Adapter<RecentQuestionAdapter.VH> {

    public interface OnItemClick { void onClick(Question q); }

    private List<Question> items;
    private final OnItemClick listener;

    public RecentQuestionAdapter(List<Question> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Question> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_question, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Question q = items.get(position);
        Context ctx = h.itemView.getContext();

        h.question.setText(q.prompt);
        h.meta.setText(relativeTime(q.createdAt));

        String subject = q.subject != null ? q.subject : "general";
        int barColor = subjectColor(ctx, subject);
        int softColor = subjectSoftColor(ctx, subject);

        h.bar.setBackgroundColor(barColor);

        String label = subject.substring(0, 1).toUpperCase() + subject.substring(1);
        h.chip.setText(label);
        h.chip.setTextColor(barColor);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(999f);
        chipBg.setColor(softColor);
        h.chip.setBackground(chipBg);

        h.itemView.setOnClickListener(v -> listener.onClick(q));
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final View bar;
        final TextView chip, meta, question;
        VH(View v) {
            super(v);
            bar      = v.findViewById(R.id.view_subject_bar);
            chip     = v.findViewById(R.id.chip_subject);
            meta     = v.findViewById(R.id.text_meta);
            question = v.findViewById(R.id.text_question);
        }
    }

    private static String relativeTime(long epochMs) {
        long diff = System.currentTimeMillis() - epochMs;
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 1) return "Just now";
        if (hours < 24) return hours + "h ago";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return days + "d ago";
    }

    private static int subjectColor(Context ctx, String subject) {
        switch (subject) {
            case "math":    return ContextCompat.getColor(ctx, R.color.subject_math);
            case "science": return ContextCompat.getColor(ctx, R.color.subject_science);
            case "code":    return ContextCompat.getColor(ctx, R.color.subject_code);
            case "history": return ContextCompat.getColor(ctx, R.color.subject_history);
            default:        return ContextCompat.getColor(ctx, R.color.brand_primary_deep);
        }
    }

    private static int subjectSoftColor(Context ctx, String subject) {
        switch (subject) {
            case "math":    return ContextCompat.getColor(ctx, R.color.subject_math_soft);
            case "science": return ContextCompat.getColor(ctx, R.color.subject_science_soft);
            case "code":    return ContextCompat.getColor(ctx, R.color.subject_code_soft);
            case "history": return ContextCompat.getColor(ctx, R.color.subject_history_soft);
            default:        return ContextCompat.getColor(ctx, R.color.brand_primary_tint);
        }
    }
}
