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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UC8 — Leaderboard.
 * Ranks user against 8 simulated opponents using real Session.xp().
 * Tab Global = all-time XP; Friends = 3 friends subset; Week = scaled weekly estimate.
 */
public class LeaderboardActivity extends AppCompatActivity {

    // ── Simulated opponents ───────────────────────────────────────────────────

    // [initials, display name, all-time XP]
    private static final Object[][] GLOBAL_OPPONENTS = {
        {"AN", "AnNguyen",  5460},
        {"MI", "MinhKhoi",  4820},
        {"TH", "ThaoP",     4310},
        {"LT", "LinhT",     3980},
        {"PN", "PhuongN",   3640},
        {"TM", "TienM",     3210},
        {"HD", "HaiD",      1680},
        {"BV", "BinhV",     1520},
    };

    // Friends = 3-person subset (initials, name, XP)
    private static final Object[][] FRIENDS_OPPONENTS = {
        {"MI", "MinhKhoi",  4820},
        {"LT", "LinhT",     3980},
        {"HD", "HaiD",      1680},
    };

    // Weekly XP estimates for same 8 opponents (parallel to GLOBAL_OPPONENTS)
    private static final int[] WEEKLY_OPP_XP = {820, 750, 690, 610, 540, 480, 290, 250};

    // ── State ─────────────────────────────────────────────────────────────────

    private RankAdapter rankAdapter;
    private String currentTab = "global";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        RecyclerView rv = findViewById(R.id.rv_ranks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rankAdapter = new RankAdapter(new ArrayList<>());
        rv.setAdapter(rankAdapter);

        rebind("global");
        setupTabs();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ── Board generation ──────────────────────────────────────────────────────

    private void rebind(String tab) {
        currentTab = tab;
        List<RankEntry> board = buildBoard(tab);
        bindPodium(board);
        bindRankList(board);
    }

    private List<RankEntry> buildBoard(String tab) {
        int userXp = Session.xp(this);
        String userName = Session.name(this);
        if (userName.isEmpty() || userName.equals("Friend")) userName = "You";

        Object[][] opponents;
        int myXp;

        if ("friends".equals(tab)) {
            opponents = FRIENDS_OPPONENTS;
            myXp = userXp;
        } else if ("week".equals(tab)) {
            opponents = GLOBAL_OPPONENTS;
            // Weekly estimate: streak × 50 XP/day, floored to 0
            myXp = Math.max(Session.streak(this) * 50, 0);
        } else {
            opponents = GLOBAL_OPPONENTS;
            myXp = userXp;
        }

        // Collect [xp, opponentIndex]; -1 = user
        int n = opponents.length + 1;
        int[] xpArr  = new int[n];
        int[] idxArr = new int[n];
        xpArr[0] = myXp;
        idxArr[0] = -1;
        for (int i = 0; i < opponents.length; i++) {
            xpArr[i + 1] = "week".equals(tab) ? WEEKLY_OPP_XP[i] : (int) opponents[i][2];
            idxArr[i + 1] = i;
        }

        // Insertion sort descending by XP
        for (int i = 1; i < n; i++) {
            int kx = xpArr[i], ki = idxArr[i], j = i - 1;
            while (j >= 0 && xpArr[j] < kx) {
                xpArr[j + 1] = xpArr[j];
                idxArr[j + 1] = idxArr[j];
                j--;
            }
            xpArr[j + 1] = kx;
            idxArr[j + 1] = ki;
        }

        List<RankEntry> result = new ArrayList<>();
        for (int rank = 1; rank <= n; rank++) {
            int idx = idxArr[rank - 1];
            int xp  = xpArr[rank - 1];
            if (idx == -1) {
                String ini = userName.length() >= 2
                        ? userName.substring(0, 2).toUpperCase(Locale.US)
                        : userName.toUpperCase(Locale.US);
                result.add(new RankEntry(rank, ini, userName, fmtXp(xp), true));
            } else {
                Object[] opp = opponents[idx];
                result.add(new RankEntry(rank, (String) opp[0], (String) opp[1], fmtXp(xp), false));
            }
        }
        return result;
    }

    private static String fmtXp(int xp) {
        return String.format(Locale.US, "%,d", xp);
    }

    // ── Bind helpers ──────────────────────────────────────────────────────────

    private void bindPodium(List<RankEntry> board) {
        for (RankEntry e : board) {
            if (e.rank == 1) {
                ((TextView) findViewById(R.id.podium1_name)).setText(e.initials);
                ((TextView) findViewById(R.id.podium1_user)).setText(e.name);
                ((TextView) findViewById(R.id.podium1_xp)).setText(e.xp);
            } else if (e.rank == 2) {
                ((TextView) findViewById(R.id.podium2_name)).setText(e.initials);
                ((TextView) findViewById(R.id.podium2_user)).setText(e.name);
                ((TextView) findViewById(R.id.podium2_xp)).setText(e.xp);
            } else if (e.rank == 3) {
                ((TextView) findViewById(R.id.podium3_name)).setText(e.initials);
                ((TextView) findViewById(R.id.podium3_user)).setText(e.name);
                ((TextView) findViewById(R.id.podium3_xp)).setText(e.xp);
            }
        }
    }

    private void bindRankList(List<RankEntry> board) {
        List<RankEntry> rest = new ArrayList<>();
        for (RankEntry e : board) if (e.rank >= 4) rest.add(e);
        rankAdapter.setItems(rest);
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void setupTabs() {
        int[] tabIds  = {R.id.tab_global, R.id.tab_friends, R.id.tab_week};
        String[] keys = {"global", "friends", "week"};

        View[] tabs = new View[tabIds.length];
        for (int i = 0; i < tabIds.length; i++) tabs[i] = findViewById(tabIds[i]);

        for (int i = 0; i < tabs.length; i++) {
            final String key = keys[i];
            final int idx = i;
            tabs[i].setOnClickListener(v -> {
                for (int j = 0; j < tabs.length; j++) {
                    tabs[j].setBackground(idx == j ? getDrawable(R.drawable.bg_tab_active) : null);
                    ((TextView) tabs[j]).setTextColor(
                            getColor(idx == j ? R.color.text_primary : R.color.text_tertiary));
                }
                rebind(key);
            });
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    static class RankEntry {
        final int rank;
        final String initials, name, xp;
        final boolean you;
        RankEntry(int r, String ini, String n, String x, boolean y) {
            rank = r; initials = ini; name = n; xp = x; you = y;
        }
    }

    static class RankAdapter extends RecyclerView.Adapter<RankAdapter.VH> {
        private List<RankEntry> items;

        RankAdapter(List<RankEntry> items) { this.items = new ArrayList<>(items); }

        void setItems(List<RankEntry> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

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
            h.avatar.setText(e.initials);
            h.name.setText(e.name);
            h.xp.setText(e.xp);
            h.youBadge.setVisibility(e.you ? View.VISIBLE : View.GONE);
            h.itemView.setBackgroundColor(h.itemView.getContext().getColor(
                    e.you ? R.color.brand_primary_tint : R.color.surface));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView rank, avatar, name, xp, youBadge;
            VH(@NonNull View v) {
                super(v);
                rank     = v.findViewById(R.id.text_rank);
                avatar   = v.findViewById(R.id.text_avatar);
                name     = v.findViewById(R.id.text_name);
                xp       = v.findViewById(R.id.text_xp);
                youBadge = v.findViewById(R.id.badge_you);
            }
        }
    }
}
