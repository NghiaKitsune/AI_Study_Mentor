package com.studymentor.app.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.Question;
import com.studymentor.app.util.SubjectIcons;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnRowClick     { void onClick(Question q);     }
    public interface OnRowLongClick { void onLongClick(Question q); }

    private List<Question> items;
    private final OnRowClick onClick;
    private OnRowLongClick onLongClick;

    public HistoryAdapter(List<Question> items, OnRowClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    public void setOnLongClick(OnRowLongClick listener) { this.onLongClick = listener; }

    public void setItems(List<Question> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Question q = items.get(position);
        Context ctx = h.itemView.getContext();

        // Subject icon: rounded-square bg + icon + icon tint
        int bgColor = subjectBgColor(ctx, q.subject);
        int fgColor = subjectFgColor(ctx, q.subject);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(12f * ctx.getResources().getDisplayMetrics().density);
        iconBg.setColor(bgColor);
        h.imgSubjectIcon.setBackground(iconBg);
        h.imgSubjectIcon.setImageResource(SubjectIcons.forSubject(q.subject));
        ImageViewCompat.setImageTintList(h.imgSubjectIcon, ColorStateList.valueOf(fgColor));

        // Subject chip: pill bg + text + color
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(999f);
        chipBg.setColor(bgColor);
        h.tvSubjectChip.setBackground(chipBg);
        h.tvSubjectChip.setTextColor(fgColor);
        h.tvSubjectChip.setText(subjectLabel(q.subject));

        // Relative time
        String relative = DateUtils.getRelativeTimeSpanString(
                q.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        h.tvTime.setText(relative);

        h.tvQuestion.setText(q.prompt);

        h.itemView.setOnClickListener(v -> onClick.onClick(q));
        h.itemView.setOnLongClickListener(v -> {
            if (onLongClick != null) { onLongClick.onLongClick(q); return true; }
            return false;
        });
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imgSubjectIcon;
        final TextView tvSubjectChip;
        final TextView tvTime;
        final TextView tvQuestion;

        VH(View v) {
            super(v);
            imgSubjectIcon = v.findViewById(R.id.img_subject_icon);
            tvSubjectChip  = v.findViewById(R.id.text_subject_chip);
            tvTime         = v.findViewById(R.id.text_time);
            tvQuestion     = v.findViewById(R.id.text_question);
        }
    }

    private static int subjectBgColor(Context ctx, String subject) {
        if (subject == null) return ContextCompat.getColor(ctx, R.color.brand_primary_tint);
        switch (subject) {
            case "math":    return ContextCompat.getColor(ctx, R.color.subject_math_soft);
            case "science": return ContextCompat.getColor(ctx, R.color.subject_science_soft);
            case "code":    return ContextCompat.getColor(ctx, R.color.subject_code_soft);
            case "history": return ContextCompat.getColor(ctx, R.color.subject_history_soft);
            default:        return ContextCompat.getColor(ctx, R.color.brand_primary_tint);
        }
    }

    private static int subjectFgColor(Context ctx, String subject) {
        if (subject == null) return ContextCompat.getColor(ctx, R.color.brand_primary_deep);
        switch (subject) {
            case "math":    return ContextCompat.getColor(ctx, R.color.subject_math);
            case "science": return ContextCompat.getColor(ctx, R.color.subject_science);
            case "code":    return ContextCompat.getColor(ctx, R.color.subject_code);
            case "history": return ContextCompat.getColor(ctx, R.color.subject_history);
            default:        return ContextCompat.getColor(ctx, R.color.brand_primary_deep);
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
}
