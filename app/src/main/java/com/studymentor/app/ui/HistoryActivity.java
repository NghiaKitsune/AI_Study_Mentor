package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityHistoryBinding;
import com.studymentor.app.databinding.ItemStatPillBinding;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;
import com.studymentor.app.data.Question;
import com.studymentor.app.ui.adapter.HistoryAdapter;
import com.studymentor.app.util.BottomNavHelper;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.HistoryViewModel;
import com.studymentor.app.viewmodel.ProgressViewModel;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;
    private ProgressViewModel progressViewModel;
    private HistoryAdapter adapter;
    private RecyclerView recycler;
    private View emptyState;
    private ChipGroup filters;
    private String search = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { routeLogin(); return; }
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recycler = binding.rvHistory;
        emptyState = binding.emptyStateInclude.getRoot();
        filters = binding.chipsFilter;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(new ArrayList<>(), this::openAnswer);
        adapter.setOnLongClick(this::confirmDelete);
        recycler.setAdapter(adapter);
        bindSearch();
        filters.setOnCheckedStateChangeListener((group, ids) -> applyFilter());
        binding.emptyStateInclude.emptyCta.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        binding.btnMiloReview.setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class)));
        BottomNavHelper.setup(this, R.id.nav_history);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.state().observe(this, state -> {
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_LONG).show();
                return;
            }
            if (state.data == null) return;
            adapter.setItems(state.data.items);
            setStat(binding.statQuestions, String.valueOf(state.data.total),
                    getString(R.string.stat_questions));
            setStat(binding.statBookmarks, String.valueOf(state.data.bookmarks),
                    getString(R.string.stat_bookmarks));
            toggleEmpty(state.data.items.isEmpty());
            View card = binding.cardMiloNoticed;
            card.setVisibility(state.data.total >= 3 ? View.VISIBLE : View.GONE);
            if (state.data.total >= 3) {
                ((TextView) binding.textMiloNoticed).setText(getResources().getQuantityString(
                        R.plurals.history_review_suggestion, state.data.total, state.data.total));
            }
        });
        progressViewModel.snapshot().observe(this, snapshot ->
                setStat(binding.statAccuracy,
                        getString(R.string.percent_value, snapshot.accuracy),
                        getString(R.string.stat_accuracy)));
        long userId = Session.userId(this);
        viewModel.initialize(userId);
        progressViewModel.initialize(userId);
    }

    @Override protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
            progressViewModel.refresh();
        }
    }

    private void bindSearch() {
        EditText input = binding.inputSearch;
        binding.btnSearch.setOnClickListener(v -> input.setText(""));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) {
                search = editable.toString();
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        int id = filters.getCheckedChipId();
        boolean bookmarked = id == R.id.chip_bookmarks;
        String subject = "";
        if (id == R.id.chip_subj_math) subject = "math";
        else if (id == R.id.chip_subj_science) subject = "science";
        else if (id == R.id.chip_subj_code) subject = "code";
        viewModel.filter(search, subject, bookmarked);
    }

    private void openAnswer(Question question) {
        Intent intent = new Intent(this, AnswerActivity.class);
        intent.putExtra(AnswerActivity.EXTRA_QUESTION_ID, question.id);
        startActivity(intent);
    }

    private void confirmDelete(Question question) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_q_title)
                .setMessage(question.prompt)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.dialog_delete_confirm,
                        (dialog, which) -> viewModel.delete(question.id))
                .show();
    }

    private void setStat(ItemStatPillBinding stat, String value, String label) {
        stat.textValue.setText(value);
        stat.textLabel.setText(label);
    }

    private void toggleEmpty(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void routeLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
