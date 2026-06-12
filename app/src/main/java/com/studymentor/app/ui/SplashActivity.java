package com.studymentor.app.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.studymentor.app.R;
import com.studymentor.app.util.Session;

/**
 * Launcher Activity. Shows Milo for ~1.5s with a gentle scale-pulse, then routes:
 *   - not logged in     → SignUp
 *   - logged, no onboard → Personalize
 *   - logged + onboarded → Home
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Scale-up entrance on the app icon container
        android.view.View mascot = findViewById(R.id.container_app_icon);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mascot, "scaleX", 0.85f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mascot, "scaleY", 0.85f, 1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();

        new Handler(Looper.getMainLooper()).postDelayed(this::route, SPLASH_MS);
    }

    private void route() {
        Intent next;
        if (!Session.isLoggedIn(this)) {
            // Show 3-step onboarding carousel on very first launch
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
