package com.studymentor.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.api.ChatResponse;

import java.util.List;

/**
 * Step-by-step breakdown adapter for AnswerActivity.
 * Each item is expandable — tap header to toggle the body visibility.
 */
public class StepAdapter extends RecyclerView.Adapter<StepAdapter.VH> {

    private final List<ChatResponse.Step> items;
    private final boolean[] expanded;

    public StepAdapter(List<ChatResponse.Step> items) {
        this.items = items;
        this.expanded = new boolean[items.size()];
        // First step starts expanded
        if (expanded.length > 0) expanded[0] = true;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer_step, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatResponse.Step s = items.get(position);
        h.index.setText(String.valueOf(s.index));
        h.title.setText(s.title);
        h.body.setText(s.body);
        h.body.setVisibility(expanded[position] ? View.VISIBLE : View.GONE);
        h.chevron.setRotation(expanded[position] ? 180f : 0f);

        h.itemView.setOnClickListener(v -> {
            expanded[position] = !expanded[position];
            notifyItemChanged(position);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView index, title, body;
        final ImageView chevron;
        VH(View v) {
            super(v);
            index   = v.findViewById(R.id.text_step_index);
            title   = v.findViewById(R.id.text_step_title);
            body    = v.findViewById(R.id.text_step_body);
            chevron = v.findViewById(R.id.img_step_chevron);
        }
    }
}
