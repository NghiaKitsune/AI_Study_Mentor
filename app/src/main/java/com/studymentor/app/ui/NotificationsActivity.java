package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityNotificationsBinding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;
import com.studymentor.app.data.NotificationEntity;
import com.studymentor.app.ui.adapter.NotificationAdapter;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.NotificationViewModel;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { finish(); return; }
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        RecyclerView recycler = binding.rvNotifications;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(item -> viewModel.markRead(item.id));
        recycler.setAdapter(adapter);
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        viewModel.data().observe(this, data -> {
            adapter.setItems(data.items);
            ((TextView) binding.textUnreadCount).setText(data.unread > 0
                    ? getString(R.string.notifications_new, data.unread)
                    : getString(R.string.notifications_caught_up));
        });
        viewModel.error().observe(this, error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show());
        ChipGroup chips = binding.chipsFilter;
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_all : ids.get(0);
            String type = "";
            if (id == R.id.chip_reminders) type = NotificationEntity.TYPE_REMINDER;
            else if (id == R.id.chip_wins) type = NotificationEntity.TYPE_PROGRESS;
            else if (id == R.id.chip_review) type = NotificationEntity.TYPE_REVIEW;
            viewModel.filter(type);
        });
        viewModel.initialize(Session.userId(this));
    }
}
