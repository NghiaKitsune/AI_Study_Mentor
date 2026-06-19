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

import com.studymentor.app.R;
import com.studymentor.app.util.Session;

import java.util.Arrays;
import java.util.List;

/**
 * UC8 — Leaderboard.
 * Podium top-3 | Ranked list (rank 4–9) with YOU row highlighted
 */
public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        bindPodium();
        bindRankList();
        setupTabs();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void bindPodium() {
        // Top 3 entries: index 0 = gold (#1), index 1 = silver (#2), index 2 = bronze (#3)
        String[][] top3 = {
            {"AN", "AnNguyen", "5,460"},
            {"MI", "MinhKhoi", "4,820"},
            {"TH", "ThaoP",    "4,310"},
        };
        ((TextView) findViewById(R.id.podium1_name)).setText(top3[0][0]);
        ((TextView) findViewById(R.id.podium1_user)).setText(top3[0][1]);
        ((TextView) findViewById(R.id.podium1_xp)).setText(top3[0][2]);
        ((TextView) findViewById(R.id.podium2_name)).setText(top3[1][0]);
        ((TextView) findViewById(R.id.podium2_user)).setText(top3[1][1]);
        ((TextView) findViewById(R.id.podium2_xp)).setText(top3[1][2]);
        ((TextView) findViewById(R.id.podium3_name)).setText(top3[2][0]);
        ((TextView) findViewById(R.id.podium3_user)).setText(top3[2][1]);
        ((TextView) findViewById(R.id.podium3_xp)).setText(top3[2][2]);
    }

    private void bindRankList() {
        String me = Session.name(this);
        List<RankEntry> entries = Arrays.asList(
            new RankEntry(4, "LinhT",   "3,980", false),
            new RankEntry(5, "PhuongN", "3,640", false),
            new RankEntry(6, "TienM",   "3,210", false),
            new RankEntry(7, me.isEmpty() || me.equals("Friend") ? "NghiaM" : me, "1,840", true),
            new RankEntry(8, "HaiD",    "1,680", false),
            new RankEntry(9, "BinhV",   "1,520", false)
        );

        RecyclerView rv = findViewById(R.id.rv_ranks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RankAdapter(entries));
    }

    private void setupTabs() {
        View[] tabs = {
            findViewById(R.id.tab_global),
            findViewById(R.id.tab_friends),
            findViewById(R.id.tab_week)
        };
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> {
                for (int j = 0; j < tabs.length; j++) {
                    tabs[j].setBackground(idx == j ? getDrawable(R.drawable.bg_tab_active) : null);
                    ((TextView) tabs[j]).setTextColor(getColor(idx == j ? R.color.text_primary : R.color.text_tertiary));
                }
            });
        }
    }

    static class RankEntry {
        final int rank; final String name, xp; final boolean you;
        RankEntry(int r, String n, String x, boolean y) { rank=r; name=n; xp=x; you=y; }
    }

    static class RankAdapter extends RecyclerView.Adapter<RankAdapter.VH> {
        private final List<RankEntry> items;
        RankAdapter(List<RankEntry> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rank_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            RankEntry e = items.get(pos);
            h.rank.setText(String.valueOf(e.rank));
            h.avatar.setText(e.name.length() >= 2 ? e.name.substring(0, 2).toUpperCase() : e.name.toUpperCase());
            h.name.setText(e.name);
            h.xp.setText(e.xp);
            h.youBadge.setVisibility(e.you ? View.VISIBLE : View.GONE);

            int bg = e.you ? R.color.brand_primary_tint : R.color.surface;
            int stroke = e.you ? R.color.brand_primary : R.color.border;
            h.itemView.setBackgroundColor(h.itemView.getContext().getColor(bg));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView rank, avatar, name, xp, youBadge;
            VH(@NonNull View v) {
                super(v);
                rank = v.findViewById(R.id.text_rank);
                avatar = v.findViewById(R.id.text_avatar);
                name = v.findViewById(R.id.text_name);
                xp = v.findViewById(R.id.text_xp);
                youBadge = v.findViewById(R.id.badge_you);
            }
        }
    }
}
