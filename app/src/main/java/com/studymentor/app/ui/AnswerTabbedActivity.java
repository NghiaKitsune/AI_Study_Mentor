package com.studymentor.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.studymentor.app.R;

public class AnswerTabbedActivity extends AppCompatActivity {

    private static final int TAB_SOLUTION = 0;
    private static final int TAB_CONCEPT  = 1;
    private static final int TAB_PRACTICE = 2;
    private static final int TAB_PITFALLS = 3;

    private int activeTab = TAB_SOLUTION;
    private TextView[] tabs;
    private View[]     indicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_tabbed);

        String question = getIntent().getStringExtra("extra_question");
        if (question == null) question = "Why does the sky appear blue during the day but red at sunset?";
        ((TextView) findViewById(R.id.text_question)).setText(question);

        tabs = new TextView[]{
            findViewById(R.id.tab_solution),
            findViewById(R.id.tab_concept),
            findViewById(R.id.tab_practice),
            findViewById(R.id.tab_pitfalls),
        };
        indicators = new View[]{
            findViewById(R.id.ind_solution),
            findViewById(R.id.ind_concept),
            findViewById(R.id.ind_practice),
            findViewById(R.id.ind_pitfalls),
        };

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTab(idx));
        }

        switchTab(TAB_SOLUTION);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v ->
            android.widget.Toast.makeText(this, R.string.toast_coming_soon, android.widget.Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_bookmark).setOnClickListener(v ->
            android.widget.Toast.makeText(this, R.string.action_bookmark, android.widget.Toast.LENGTH_SHORT).show());
    }

    private void switchTab(int idx) {
        activeTab = idx;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == idx);
            tabs[i].setTextColor(getColor(active ? R.color.text_primary : R.color.text_tertiary));
            indicators[i].setVisibility(active ? View.VISIBLE : View.GONE);
        }
        renderContent(idx);
    }

    private void renderContent(int idx) {
        LinearLayout container = findViewById(R.id.content_container);
        container.removeAllViews();

        switch (idx) {
            case TAB_SOLUTION:
                addBody(container,
                    "Sunlight scatters off air molecules. Blue light scatters more than red, so the sky looks blue. " +
                    "At sunset, light travels through more atmosphere, blue gets scattered away, and what reaches you is red.");
                addSection(container, "Step 1 — White light is a rainbow",
                    "Sunlight is made of all visible colors (red → violet).");
                addSection(container, "Step 2 — Rayleigh scattering",
                    "Air molecules scatter short wavelengths much more than long ones. Blue gets bounced around the sky.");
                addSection(container, "Step 3 — Why red at sunset",
                    "Near the horizon, light passes through ~12× more atmosphere. By the time it reaches you, most blue is gone.");
                break;

            case TAB_CONCEPT:
                addSection(container, "Rayleigh Scattering",
                    "I ∝ 1 / λ⁴\n\nWhen light hits particles much smaller than its wavelength, scattering intensity is " +
                    "inversely proportional to the 4th power of wavelength. Shorter wavelength (blue) = much more scattering.");
                addSection(container, "Fun fact",
                    "Why not violet? Violet scatters even more — but our eyes are less sensitive to it, so we perceive the sky as blue.");
                break;

            case TAB_PRACTICE:
                addBody(container, "Quick check — earn +15 XP for getting them right:");
                addSection(container, "Q1: Which color has the shortest wavelength?",
                    "A. Red\nB. Green\nC. Violet ✓\nD. Yellow");
                addSection(container, "Q2: Why is the sunset red?",
                    "A. Light loses energy\nB. Blue is scattered away ✓\nC. Clouds absorb blue\nD. Sun emits more red");
                break;

            case TAB_PITFALLS:
                addSection(container, "✗ Sky is blue because of water",
                    "It's air molecules (mostly N₂ and O₂), not water vapor, that cause Rayleigh scattering.");
                addSection(container, "✗ Sunset is red because light loses energy",
                    "Light doesn't lose energy. Blue has already been scattered away before reaching you.");
                addSection(container, "✗ More scattering = brighter sky",
                    "Bright is about total light. The sky is bright AND blue because scattered blue reaches you from every direction.");
                break;
        }
    }

    private void addSection(LinearLayout container, String title, String body) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        TextView tvTitle = item.findViewById(R.id.text_section_title);
        tvTitle.setText(title);
        tvTitle.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
        lp.topMargin = dpToPx(14);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(body);
        container.addView(item);
    }

    private void addBody(LinearLayout container, String text) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_answer_section, container, false);
        ((TextView) item.findViewById(R.id.text_section_body)).setText(text);
        container.addView(item);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
