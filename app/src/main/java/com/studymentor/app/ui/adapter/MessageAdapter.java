package com.studymentor.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.Message;

import java.util.List;

/**
 * Two-viewtype adapter — user bubble (right) vs assistant bubble (left).
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int TYPE_USER      = 1;
    private static final int TYPE_ASSISTANT = 2;

    private final List<Message> items;

    public MessageAdapter(List<Message> items) { this.items = items; }

    @Override
    public int getItemViewType(int position) {
        return Message.ROLE_USER.equals(items.get(position).role) ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_assistant;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.bubble.setText(items.get(position).text);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView bubble;
        VH(View v) {
            super(v);
            bubble = v.findViewById(R.id.text_bubble);
        }
    }
}
