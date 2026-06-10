package com.studymentor.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.studymentor.app.util.Session;

/**
 * Launcher / router. Forwards to the right screen based on session state, then finishes.
 *
 * Flow:
 *   1) not logged in     → SignUpActivity
 *   2) logged in, no onboarding → PersonalizeActivity
 *   3) logged in, onboarded     → HomeActivity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent next;
        if (!Session.isLoggedIn(this)) {
            // Show onboarding carousel on very first launch
            if (!Session.hasSeenOnboarding(this)) {
                next = new Intent(this, OnboardingActivity.class);
            } else {
                next = new Intent(this, SignUpActivity.class);
            }
        } else if (!Session.isOnboarded(this)) {
            next = new Intent(this, PersonalizeActivity.class);
        } else {
            next = new Intent(this, HomeActivity.class);
        }
        startActivity(next);
        finish();
    }
}
