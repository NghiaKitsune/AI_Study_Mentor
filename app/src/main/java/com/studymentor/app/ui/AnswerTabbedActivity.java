package com.studymentor.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.studymentor.app.R;

/**
 * UC4 — Tabbed answer variant.
 * Dark question header | Solution / Concept / Practice / Pitfalls tabs | Floating follow-up composer
 */
public class AnswerTabbedActivity extends AppCompatActivity {

    private static final int TAB_SOLUTION  = 0;
    private static final int TAB_CONCEPT   = 1;
    private static final int TAB_PRACTICE  = 2;
    private static final int TAB_PITFALLS  = 3;

    private int activeTab = TAB_SOLUTION;
    private TextView[] tabs;

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

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTab(idx));
        }

        switchTab(TAB_SOLUTION);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, R.string.toast_coming_soon, android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, R.string.action_bookmark, android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void switchTab(int idx) {
        activeTab = idx;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == idx);
            tabs[i].setTextColor(getColor(active ? R.color.text_primary : R.color.text_tertiary));
        }
        renderContent(idx);
    }

    private void renderContent(int idx) {
        LinearLayout container = findViewById(R.id.content_container);
        container.removeAllViews();

        String[] titles = {
            "Solution", "Concept", "Practice", "Pitfalls"
        };
        String[] bodies = {
            "Sunlight scatters off air molecules. Blue light scatters more than red, so the sky looks blue. " +
            "At sunset, light travels through more atmosphere, blue gets scattered away, and what reaches you is red.\n\n" +
            "Step 1 — White light is a rainbow\nSunlight is made of all visible colors (red → violet).\n\n" +
            "Step 2 — Rayleigh scattering\nAir molecules scatter short wavelengths much more than long ones. Blue gets bounced around the sky.\n\n" +
            "Step 3 — Why red at sunset\nNear the horizon, light passes through ~12× more atmosphere. By the time it reaches you, most blue is gone.",

            "Rayleigh Scattering\n\nI ∝ 1 / λ⁴\n\nWhen light hits particles much smaller than its wavelength, scattering intensity is inversely proportional to the 4th power of wavelength.\n\nShorter wavelength (blue) = much more scattering.\n\nFun fact: Why not violet? Violet scatters even more — but our eyes are less sensitive to it.",

            "Quick check — earn +15 XP for getting them right:\n\nQ1: Which color of visible light has the shortest wavelength?\n  A. Red\n  B. Green\n  C. Violet ✓\n  D. Yellow\n\nQ2: Why is the sunset red?\n  A. Light loses energy  \n  B. Blue is scattered away ✓\n  C. Clouds absorb blue\n  D. Sun emits more red",

            "Common Pitfalls:\n\n✗ Sky is blue because of water\n→ It's air molecules (mostly N₂ and O₂), not water vapor, that cause Rayleigh scattering.\n\n✗ Sunset is red because light loses energy\n→ Light doesn't lose energy. Blue has already been scattered away before reaching you.\n\n✗ More scattering = brighter sky\n→ Bright is about total light. The sky is bright AND blue because scattered blue reaches you from every direction."
        };

        TextView tv = new TextView(this);
        tv.setText(bodies[idx]);
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setTextSize(14);
        tv.setLineSpacing(4f, 1.4f);
        container.addView(tv);
    }
}
