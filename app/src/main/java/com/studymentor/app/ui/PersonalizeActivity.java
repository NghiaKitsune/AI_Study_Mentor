package com.studymentor.app.ui;

import com.studymentor.app.databinding.ActivityPersonalizeBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;
import com.studymentor.app.util.Session;
import com.studymentor.app.viewmodel.AuthViewModel;

import java.util.Locale;

public class PersonalizeActivity extends AppCompatActivity {
    private ActivityPersonalizeBinding binding;
    private ChipGroup levels;
    private ChipGroup subjects;
    private AuthViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalizeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Session.userId(this) <= 0) { routeToLogin(); return; }
        levels = binding.chipsLevel;
        subjects = binding.chipsSubjects;
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSkip.setOnClickListener(v ->
                save("", "", "detailed", Session.language(this)));
        binding.btnContinue.setOnClickListener(v -> saveSelections());
        viewModel.state().observe(this, state -> {
            if (state.status == AuthViewModel.AuthState.Status.ERROR) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show();
                viewModel.clearTransientState();
            } else if (state.status == AuthViewModel.AuthState.Status.PERSONALIZATION_SAVED) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void saveSelections() {
        String level = levelId();
        if (level.isEmpty()) {
            Toast.makeText(this, R.string.personalize_choose_level, Toast.LENGTH_SHORT).show();
            return;
        }
        save(level, subjectIds(), "detailed", Session.language(this));
    }

    private void save(String level, String subjectsCsv, String style, String language) {
        viewModel.savePersonalization(Session.userId(this), level, subjectsCsv, style, language);
    }

    private String subjectIds() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < subjects.getChildCount(); i++) {
            View view = subjects.getChildAt(i);
            if (view instanceof Chip && ((Chip) view).isChecked()) {
                String id = ((Chip) view).getText().toString().trim().toLowerCase(Locale.ROOT)
                        .replace("programming", "code")
                        .replace("languages", "language")
                        .replace(" ", "_");
                if (result.length() > 0) result.append(',');
                result.append(id);
            }
        }
        return result.toString();
    }

    private String levelId() {
        int id = levels.getCheckedChipId();
        if (id == R.id.chip_level_middle) return "middle_school";
        if (id == R.id.chip_level_high) return "high_school";
        if (id == R.id.chip_level_college) return "university";
        if (id == R.id.chip_level_self) return "self_study";
        return "";
    }

    private void routeToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
