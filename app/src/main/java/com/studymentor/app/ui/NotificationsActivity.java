package com.studymentor.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UC9 — Notifications.
 * Filter chips (All / Reminders / Wins / Review) | Notification list with unread dots
 */
public class NotificationsActivity extends AppCompatActivity {

    private NotifAdapter adapter;
    private List<NotifItem> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        allItems = buildItems();

        int unread = 0;
        for (NotifItem n : allItems) if (n.unread) unread++;
        TextView tvCount = findViewById(R.id.text_unread_count);
        tvCount.setText(unread > 0 ? unread + " new" : "All caught up");

        RecyclerView rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifAdapter(allItems);
        rv.setAdapter(adapter);

        setupFilter();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SettingsActivity.class)));
    }

    private void setupFilter() {
        ChipGroup chips = findViewById(R.id.chips_filter);
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_all : ids.get(0);
            String filter;
            if      (id == R.id.chip_reminders) filter = "reminders";
            else if (id == R.id.chip_wins)      filter = "achievements";
            else if (id == R.id.chip_review)    filter = "mistakes";
            else                                filter = "all";

            List<NotifItem> filtered = new ArrayList<>();
            for (NotifItem n : allItems) {
                if (filter.equals("all") || filter.equals(n.type)) filtered.add(n);
            }
            adapter.setItems(filtered);
        });
    }

    private List<NotifItem> buildItems() {
        return Arrays.asList(
            new NotifItem("achievements", R.drawable.ic_star, R.color.subject_math,
                "New badge unlocked!",
                "You earned \"Sharp Shooter\" — 10 perfect quizzes in a row.",
                "10m ago", true, R.color.subject_math_soft),
            new NotifItem("reminders", R.drawable.ic_bookmark, R.color.brand_primary,
                "Review your bookmark",
                "Solve: 2x² + 5x − 3 = 0 — saved 3 days ago. Quick refresher?",
                "1h ago", true, R.color.brand_primary_soft),
            new NotifItem("mistakes", R.drawable.ic_info, R.color.error,
                "You keep mixing these up",
                "Mitosis vs meiosis — you missed it 3 times this week. Want a quick visual?",
                "2h ago", true, R.color.error_soft),
            new NotifItem("reminders", R.drawable.ic_flame, R.color.brand_accent,
                "Keep your streak alive",
                "You're on day 7. Ask 1 question before midnight to make it 8.",
                "5h ago", false, 0),
            new NotifItem("achievements", R.drawable.ic_star, R.color.brand_primary,
                "Daily summary",
                "Yesterday: 12 questions · 45 XP · Top subject was Math. Great work!",
                "Yesterday", false, 0),
            new NotifItem("reminders", R.drawable.ic_target, R.color.subject_science,
                "Weekly recap is ready",
                "74 questions, 92% quiz accuracy. Tap to see what you mastered.",
                "2d ago", false, 0)
        );
    }

    static class NotifItem {
        final String type, title, body, time;
        final int iconRes, colorRes, iconBgColorRes;
        final boolean unread;
        NotifItem(String t, int i, int c, String ti, String b, String tm, boolean u, int ibcr) {
            type=t; iconRes=i; colorRes=c; title=ti; body=b; time=tm; unread=u; iconBgColorRes=ibcr;
        }
    }

    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private List<NotifItem> items;
        NotifAdapter(List<NotifItem> items) { this.items = new ArrayList<>(items); }

        void setItems(List<NotifItem> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NotifItem n = items.get(pos);
            android.content.Context ctx = h.itemView.getContext();
            h.icon.setImageResource(n.iconRes);
            h.icon.getDrawable().setTint(ctx.getColor(n.colorRes));
            h.title.setText(n.title);
            h.body.setText(n.body);
            h.time.setText(n.time);
            h.unreadDot.setVisibility(n.unread ? View.VISIBLE : View.GONE);

            float d = ctx.getResources().getDisplayMetrics().density;
            int dp14 = (int)(14 * d); int dp12 = (int)(12 * d);
            if (n.unread) {
                h.itemView.setBackground(ctx.getDrawable(R.drawable.bg_notif_card_unread));
                h.itemView.setPadding(dp14, dp14, dp14, dp14);
                h.divider.setVisibility(View.GONE);
                if (n.iconBgColorRes != 0)
                    h.iconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ctx.getColor(n.iconBgColorRes)));
                else
                    h.iconBg.setBackgroundTintList(null);
            } else {
                h.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                h.itemView.setPadding(0, dp12, 0, dp12);
                h.divider.setVisibility(View.VISIBLE);
                h.iconBg.setBackgroundTintList(null);
                h.iconBg.setBackground(null);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon; final TextView title, body, time; final View unreadDot, iconBg, divider;
            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.notif_icon);
                title = v.findViewById(R.id.notif_title);
                body = v.findViewById(R.id.notif_body);
                time = v.findViewById(R.id.notif_time);
                unreadDot = v.findViewById(R.id.unread_dot);
                iconBg = v.findViewById(R.id.notif_icon_bg);
                divider = v.findViewById(R.id.notif_divider);
            }
        }
    }
}
