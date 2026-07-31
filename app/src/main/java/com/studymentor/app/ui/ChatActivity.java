package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityChatBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.studymentor.app.R;
import com.studymentor.app.data.Question;
import com.studymentor.app.repository.ChatRepository;
import com.studymentor.app.ui.adapter.MessageAdapter;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    public static final String EXTRA_PROMPT = "extra_prompt";
    public static final String EXTRA_QUESTION_ID = "extra_question_id";

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private RecyclerView recycler;
    private TextInputEditText input;
    private TextView typing;
    private View suggestions;
    private FloatingActionButton send;
    private String lastStatus = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) {
            routeLogin();
            return;
        }
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> finish());
        recycler = binding.rvMessages;
        input = binding.inputMessage;
        typing = binding.textTyping;
        suggestions = binding.layoutSuggestions;
        send = binding.btnSend;
        adapter = new MessageAdapter(new ArrayList<>(), this::openDetail);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        long questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1L);
        viewModel.initialize(questionId, Session.userId(this));

        bindSuggestions();
        bindPrefill(getIntent());
        send.setOnClickListener(v -> sendCurrent());
        binding.btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_SOURCE, "chat");
            startActivity(intent);
        });
        binding.btnChatMore.setOnClickListener(v -> {
            if (viewModel.questionId() > 0) openDetail(viewModel.questionId());
        });

        viewModel.messages().observe(this, messages -> {
            adapter.setItems(messages);
            suggestions.setVisibility(messages == null || messages.isEmpty()
                    ? View.VISIBLE : View.GONE);
            scrollBottom();
        });
        viewModel.state().observe(this, state -> {
            boolean processing = Question.STATUS_PROCESSING.equals(state.status);
            typing.setVisibility(processing ? View.VISIBLE : View.GONE);
            send.setEnabled(!processing);
            if (state.message != null && !state.message.trim().isEmpty()
                    && !state.status.equals(lastStatus)) {
                Snackbar bar = Snackbar.make(recycler, state.message, Snackbar.LENGTH_LONG);
                if (Question.STATUS_FAILED.equals(state.status)
                        || Question.STATUS_PENDING.equals(state.status)) {
                    bar.setAction(R.string.action_retry, v -> viewModel.retry());
                }
                bar.show();
            }
            if (Question.STATUS_COMPLETED.equals(state.status)
                    && !Question.STATUS_COMPLETED.equals(lastStatus)
                    && state.questionId > 0) {
                Snackbar.make(recycler,
                                state.fromCache
                                        ? R.string.chat_loaded_from_history
                                        : R.string.chat_saved_offline,
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.answer_details,
                                v -> openDetail(state.questionId))
                        .show();
            }
            lastStatus = state.status;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindPrefill(intent);
    }

    private void bindPrefill(Intent intent) {
        String prefill = intent.getStringExtra(EXTRA_PROMPT);
        if (prefill != null && !prefill.trim().isEmpty()) {
            String clean = prefill.trim();
            input.setText(clean);
            input.setSelection(clean.length());
            suggestions.setVisibility(View.GONE);
        }
    }

    private void bindSuggestions() {
        for (com.google.android.material.chip.Chip chip :
                new com.google.android.material.chip.Chip[]{binding.chipSuggest1,
                        binding.chipSuggest2, binding.chipSuggest3, binding.chipSuggest4}) {
            chip.setOnClickListener(v -> {
                input.setText(chip.getText());
                sendCurrent();
            });
        }
    }

    private void sendCurrent() {
        String prompt = String.valueOf(input.getText()).trim();
        if (prompt.isEmpty()) {
            input.setError(getString(R.string.chat_error_empty));
            return;
        }
        if (prompt.length() > ChatRepository.MAX_PROMPT_CHARS) {
            input.setError(getResources().getQuantityString(R.plurals.chat_error_too_long,
                    ChatRepository.MAX_PROMPT_CHARS, ChatRepository.MAX_PROMPT_CHARS));
            return;
        }
        input.setText("");
        suggestions.setVisibility(View.GONE);
        viewModel.send(prompt, "");
    }

    private void openDetail(long questionId) {
        Intent intent = new Intent(this, AnswerActivity.class);
        intent.putExtra(AnswerActivity.EXTRA_QUESTION_ID, questionId);
        startActivity(intent);
    }

    private void scrollBottom() {
        recycler.post(() -> {
            int count = adapter.getItemCount();
            if (count > 0) recycler.scrollToPosition(count - 1);
        });
    }

    private void routeLogin() {
        Toast.makeText(this, R.string.auth_required_history, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
