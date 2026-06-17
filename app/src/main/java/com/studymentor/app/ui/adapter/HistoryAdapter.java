package com.studymentor.app.ui.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.Question;

import java.util.List;

/**
 * Compact list adapter used by Home (recent) and History (full list).
 */
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
        h.question.setText(q.prompt);
        String relative = DateUtils.getRelativeTimeSpanString(
                q.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        h.meta.setText(q.subject + " · " + relative);
        h.itemView.setOnClickListener(v -> onClick.onClick(q));
        h.itemView.setOnLongClickListener(v -> {
            if (onLongClick != null) { onLongClick.onLongClick(q); return true; }
            return false;
        });
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView question, meta;
        VH(View v) {
            super(v);
            question = v.findViewById(R.id.text_question);
            meta     = v.findViewById(R.id.text_meta);
        }
    }
}
