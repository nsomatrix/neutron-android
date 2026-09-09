/*
 * Copyright 2026 Neutron Emulator Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nsomatrix.neutron;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nsomatrix.neutron.base.BaseActivity;
import com.nsomatrix.neutron.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

	private static final long SPLASH_DURATION = 1800L;

	private ActivitySplashBinding binding;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private boolean navigated = false;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		binding = ActivitySplashBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.tvSplashVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);

		ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot, (v, insets) -> {
			Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.tvSplashVersion.getLayoutParams();
			int baseBottom = (int) (24 * getResources().getDisplayMetrics().density);
			lp.bottomMargin = baseBottom + navInsets.bottom;
			binding.tvSplashVersion.setLayoutParams(lp);
			return insets;
		});

		// Dynamic subtle entrance animation
		binding.layoutCenterContent.setAlpha(0f);
		binding.layoutCenterContent.setScaleX(0.92f);
		binding.layoutCenterContent.setScaleY(0.92f);
		binding.layoutCenterContent.animate()
				.alpha(1f)
				.scaleX(1f)
				.scaleY(1f)
				.setDuration(400)
				.setInterpolator(new DecelerateInterpolator())
				.start();

		handler.postDelayed(this::navigateToMain, SPLASH_DURATION);
		binding.splashRoot.setOnClickListener(v -> navigateToMain());
	}

	private void navigateToMain() {
		if (navigated || isFinishing()) return;
		navigated = true;
		handler.removeCallbacksAndMessages(null);

		Intent intent = new Intent(this, MainActivity.class);
		if (getIntent() != null) {
			if (getIntent().getData() != null) {
				intent.setData(getIntent().getData());
			}
			if (getIntent().getAction() != null) {
				intent.setAction(getIntent().getAction());
			}
			intent.setFlags(getIntent().getFlags());
		}
		startActivity(intent);
		finish();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	@Override
	protected void onDestroy() {
		handler.removeCallbacksAndMessages(null);
		binding = null;
		super.onDestroy();
	}
}
