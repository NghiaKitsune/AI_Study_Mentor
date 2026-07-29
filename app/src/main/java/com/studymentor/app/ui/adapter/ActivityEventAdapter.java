package com.studymentor.app.ui.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.studymentor.app.R;
import com.studymentor.app.data.XpEvent;

import java.util.ArrayList;
import java.util.List;

public class ActivityEventAdapter extends RecyclerView.Adapter<ActivityEventAdapter.Holder> {
    private final List<XpEvent> items = new ArrayList<>();

    public void setItems(List<XpEvent> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_row, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        XpEvent event = items.get(position);
        holder.text.setText(holder.itemView.getContext().getString(
                R.string.activity_xp_format,
                holder.itemView.getContext().getString(labelResource(event)), event.amount));
        holder.time.setText(DateUtils.getRelativeTimeSpanString(event.createdAt,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        holder.icon.setImageResource(XpEvent.TYPE_QUIZ_COMPLETED.equals(event.eventType)
                ? R.drawable.ic_medal : XpEvent.TYPE_ANSWER_REVIEWED.equals(event.eventType)
                ? R.drawable.ic_bookmark : R.drawable.ic_sparkles);
    }

    @StringRes private static int labelResource(XpEvent event) {
        if (XpEvent.TYPE_QUIZ_COMPLETED.equals(event.eventType)) return R.string.activity_quiz_completed;
        if (XpEvent.TYPE_ANSWER_REVIEWED.equals(event.eventType)) return R.string.activity_answer_reviewed;
        return R.string.activity_question_completed;
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView text;
        final TextView time;
        Holder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_icon);
            text = itemView.findViewById(R.id.text_activity);
            time = itemView.findViewById(R.id.text_time);
        }
    }
}
