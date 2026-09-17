/*
 * Copyright 2023 Nikita Shakarun
 * Copyright 2023 Arman Jussupgaliyev
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

package com.nsomatrix.neutron.config;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ConfigPagerAdapter extends FragmentStateAdapter {

	private final DisplayConfigFragment displayFragment = new DisplayConfigFragment();
	private final FontConfigFragment fontFragment = new FontConfigFragment();
	private final InputConfigFragment inputFragment = new InputConfigFragment();
	private final SystemConfigFragment systemFragment = new SystemConfigFragment();

	public ConfigPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
		super(fragmentActivity);
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		switch (position) {
			case 0:
				return displayFragment;
			case 1:
				return fontFragment;
			case 2:
				return inputFragment;
			case 3:
				return systemFragment;
			default:
				throw new IllegalArgumentException("Invalid tab position: " + position);
		}
	}

	@Override
	public int getItemCount() {
		return 4;
	}

	public DisplayConfigFragment getDisplayFragment() {
		return displayFragment;
	}

	public FontConfigFragment getFontFragment() {
		return fontFragment;
	}

	public InputConfigFragment getInputFragment() {
		return inputFragment;
	}

	public SystemConfigFragment getSystemFragment() {
		return systemFragment;
	}
}
