package com.studymentor.app.ui.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.NotificationEntity;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    public interface Listener { void onClick(NotificationEntity item); }
    private final List<NotificationEntity> items = new ArrayList<>();
    private final Listener listener;

    public NotificationAdapter(Listener listener) { this.listener = listener; }
    public void setItems(List<NotificationEntity> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_row, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        NotificationEntity item = items.get(position);
        holder.title.setText(item.title);
        holder.body.setText(item.body);
        holder.time.setText(DateUtils.getRelativeTimeSpanString(item.createdAt,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        holder.dot.setVisibility(item.read ? View.GONE : View.VISIBLE);
        holder.icon.setImageResource(NotificationEntity.TYPE_REVIEW.equals(item.type)
                ? R.drawable.ic_bookmark : NotificationEntity.TYPE_PROGRESS.equals(item.type)
                ? R.drawable.ic_medal : R.drawable.ic_bell);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView body;
        final TextView time;
        final View dot;
        Holder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notif_icon);
            title = itemView.findViewById(R.id.notif_title);
            body = itemView.findViewById(R.id.notif_body);
            time = itemView.findViewById(R.id.notif_time);
            dot = itemView.findViewById(R.id.unread_dot);
        }
    }
}
