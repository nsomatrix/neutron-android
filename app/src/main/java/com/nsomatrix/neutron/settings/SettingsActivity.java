/*
 * Copyright 2017 Nikita Shakarun
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

package com.nsomatrix.neutron.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.base.BaseActivity;

public class SettingsActivity extends BaseActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		setContentView(R.layout.activity_settings);

		AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
		MaterialToolbar toolbar = findViewById(R.id.toolbar);
		if (toolbar != null) {
			toolbar.setNavigationOnClickListener(v -> finish());
		}

		if (appBarLayout != null) {
			ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
				Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
				v.setPadding(0, statusBarInsets.top, 0, 0);
				return insets;
			});
		}
	}
}
