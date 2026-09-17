/*
 * Copyright 2026 NSO Matrix / Neutron Team
 * Enterprise Boot Splash Implementation
 */

package com.nsomatrix.neutron;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.RenderMode;
import com.nsomatrix.neutron.config.Config;
import com.nsomatrix.neutron.util.FileUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 2400L;
    private final ExecutorService initExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LottieAnimationView lottieLoader;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Install AndroidX Splash Screen engine before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenViewProvider -> {
            // Immediately remove OS window splash so activity_splash layout & Lottie loader are visible right away
            splashScreenViewProvider.remove();
        });

        super.onCreate(savedInstanceState);
        
        // Setup transparent edge-to-edge system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Dynamic system status/nav bar icon appearance for Light vs Dark themes
        boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(!isNightMode);
            insetsController.setAppearanceLightNavigationBars(!isNightMode);
        }

        setContentView(R.layout.activity_splash);

        lottieLoader = findViewById(R.id.lottie_loader);
        if (lottieLoader != null) {
            lottieLoader.setRenderMode(RenderMode.HARDWARE);
            // Async composition pre-parsing to eliminate UI thread frame drops
            LottieCompositionFactory.fromRawRes(this, R.raw.splash_loader)
                    .addListener(composition -> {
                        if (lottieLoader != null && !isFinishing() && !isDestroyed()) {
                            lottieLoader.setComposition(composition);
                            lottieLoader.playAnimation();
                        }
                    });
        }

        startTime = System.currentTimeMillis();

        // 2. Perform non-blocking async pre-warming
        performAsyncAppPrewarm();
    }

    private void performAsyncAppPrewarm() {
        initExecutor.execute(() -> {
            try {
                // Pre-warm storage directory check off main thread
                String emulatorDir = Config.getEmulatorDir();
                if (emulatorDir != null) {
                    File dir = new File(emulatorDir);
                    if (dir.isDirectory() && dir.canWrite()) {
                        FileUtils.initWorkDir(dir);
                    }
                }
            } catch (Exception ignored) {
                // Non-critical background task safe catch
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingDelay = Math.max(0, MIN_SPLASH_DURATION_MS - elapsedTime);

            mainHandler.postDelayed(this::navigateToMain, remainingDelay);
        });
    }

    private void navigateToMain() {
        if (isFinishing() || isDestroyed()) return;

        View splashRoot = findViewById(R.id.splash_root);
        if (splashRoot != null) {
            splashRoot.animate()
                    .alpha(0f)
                    .setDuration(260L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        if (getIntent() != null && getIntent().getData() != null) {
                            intent.setData(getIntent().getData());
                            intent.setAction(getIntent().getAction());
                        }
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    })
                    .start();
        } else {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (getIntent() != null && getIntent().getData() != null) {
                intent.setData(getIntent().getData());
                intent.setAction(getIntent().getAction());
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        initExecutor.shutdown();
    }
}
