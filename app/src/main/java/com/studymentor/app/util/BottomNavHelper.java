package com.studymentor.app.util;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studymentor.app.R;
import com.studymentor.app.ui.HistoryActivity;
import com.studymentor.app.ui.HomeActivity;
import com.studymentor.app.ui.ProfileActivity;
import com.studymentor.app.ui.QuizActivity;

public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(AppCompatActivity activity, int currentTabId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(currentTabId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentTabId) return true;

            Class<?> target = null;
            if (id == R.id.nav_home)     target = HomeActivity.class;
            if (id == R.id.nav_history)  target = HistoryActivity.class;
            if (id == R.id.nav_practice) target = QuizActivity.class;
            if (id == R.id.nav_profile)  target = ProfileActivity.class;

            if (target != null) {
                Intent i = new Intent(activity, target);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(i);
            }
            return true;
        });
    }
}
