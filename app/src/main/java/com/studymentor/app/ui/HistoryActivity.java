package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;
import com.studymentor.app.StudyMentorApp;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.HistoryAdapter;
import com.studymentor.app.util.BottomNavHelper;

import java.util.List;

/**
 * UC5 — History list with stats + filter chips + empty state.
 * Filter chip ids:
 *   chip_all                → all questions
 *   chip_bookmarks          → bookmarked only
 *   chip_subj_math/...      → by subject substring match
 */
public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter adapter;
    private RecyclerView rv;
    private View emptyState;
    private ChipGroup chips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rv         = findViewById(R.id.rv_history);
        emptyState = findViewById(R.id.empty_state);
        chips      = findViewById(R.id.chips_filter);

        bindStats();
        bindFilter();
        bindList();
        bindEmptyStateCta();
        bindMiloNoticed();
        BottomNavHelper.setup(this, R.id.nav_history);
    }

    private void bindStats() {
        int count     = StudyMentorApp.get().db().questionDao().count();
        int bookmarks = StudyMentorApp.get().db().questionDao().bookmarkedCount();
        setStat(R.id.stat_questions, String.valueOf(count),     getString(R.string.stat_questions));
        setStat(R.id.stat_bookmarks, String.valueOf(bookmarks), getString(R.string.stat_bookmarks));
        setStat(R.id.stat_accuracy,  "—",                       getString(R.string.stat_accuracy));
    }

    private void setStat(int id, String value, String label) {
        View root = findViewById(id);
        ((TextView) root.findViewById(R.id.text_value)).setText(value);
        ((TextView) root.findViewById(R.id.text_label)).setText(label);
    }

    private void bindFilter() {
        chips.setOnCheckedStateChangeListener((group, ids) -> reload());
    }

    private void bindList() {
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(StudyMentorApp.get().db().questionDao().all(), q -> {
            Intent i = new Intent(this, AnswerActivity.class);
            i.putExtra(AnswerActivity.EXTRA_QUESTION_ID, q.id);
            startActivity(i);
        });
        rv.setAdapter(adapter);
        reload();
    }

    private void bindEmptyStateCta() {
        View cta = findViewById(R.id.empty_cta);
        cta.setVisibility(View.VISIBLE);
        ((com.google.android.material.button.MaterialButton) cta).setText(R.string.empty_history_cta);
        cta.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
            finish();
        });
    }

    private void reload() {
        List<Question> items = applyFilter(StudyMentorApp.get().db().questionDao().all());
        adapter.setItems(items);
        toggleEmpty(items.isEmpty());
    }

    private List<Question> applyFilter(List<Question> all) {
        int checkedId = chips.getCheckedChipId();
        if (checkedId == View.NO_ID || checkedId == R.id.chip_all) return all;

        if (checkedId == R.id.chip_bookmarks) {
            java.util.List<Question> out = new java.util.ArrayList<>();
            for (Question q : all) if (q.bookmarked) out.add(q);
            return out;
        }
        // Subject chip — match the chip's text against question.subject (case-insensitive)
        Chip chip = findViewById(checkedId);
        if (chip == null) return all;
        String label = chip.getText().toString().toLowerCase();
        java.util.List<Question> out = new java.util.ArrayList<>();
        for (Question q : all) {
            if (q.subject != null && q.subject.toLowerCase().contains(label)) out.add(q);
        }
        return out;
    }

    private void bindMiloNoticed() {
        View card = findViewById(R.id.card_milo_noticed);
        if (card == null) return;
        int count = StudyMentorApp.get().db().questionDao().count();
        if (count >= 5) {
            card.setVisibility(View.VISIBLE);
            TextView tv = card.findViewById(R.id.text_milo_noticed);
            tv.setText("You've asked " + count + " questions. Want a quick review quiz?");
        }
        card.findViewById(R.id.btn_milo_review).setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class)));
    }

    private void toggleEmpty(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);

        // Tailor the empty state copy by current filter
        TextView title = findViewById(R.id.empty_title);
        TextView body  = findViewById(R.id.empty_body);
        if (chips.getCheckedChipId() == R.id.chip_bookmarks) {
            title.setText(R.string.empty_bookmarks_title);
            body.setText(R.string.empty_bookmarks_body);
        } else {
            title.setText(R.string.empty_history_title);
            body.setText(R.string.empty_history_body);
        }
    }
}
