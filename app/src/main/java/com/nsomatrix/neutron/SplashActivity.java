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

    private static final long MIN_SPLASH_DURATION_MS = 1600L;
    private final ExecutorService initExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LottieAnimationView lottieLoader;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Install AndroidX Splash Screen engine before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

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

        if (lottieLoader != null) {
            lottieLoader.pauseAnimation();
        }

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        // Forward launch intent data (e.g. JAR/JAD file opens) if present
        if (getIntent() != null && getIntent().getData() != null) {
            intent.setData(getIntent().getData());
            intent.setAction(getIntent().getAction());
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        initExecutor.shutdown();
    }
}
