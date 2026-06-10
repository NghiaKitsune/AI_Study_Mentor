package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * UC1 — Personalize step. Optional. Skip just bumps the user to Home with empty prefs.
 */
public class PersonalizeActivity extends AppCompatActivity {

    private ChipGroup chipsLevel, chipsSubjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalize);

        chipsLevel    = findViewById(R.id.chips_level);
        chipsSubjects = findViewById(R.id.chips_subjects);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_skip).setOnClickListener(v -> finishOnboarding(""));
        findViewById(R.id.btn_continue).setOnClickListener(this::handleContinue);
    }

    private void handleContinue(View v) {
        finishOnboarding(buildSubjectsCsv());
    }

    private String buildSubjectsCsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chipsSubjects.getChildCount(); i++) {
            Chip c = (Chip) chipsSubjects.getChildAt(i);
            if (c.isChecked()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(c.getText());
            }
        }
        return sb.toString();
    }

    private void finishOnboarding(String subjectsCsv) {
        String level = "";
        int checkedId = chipsLevel.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip c = findViewById(checkedId);
            level = c.getText().toString();
        }
        Session.savePersonalization(this, level, subjectsCsv);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
