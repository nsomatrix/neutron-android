/*
 * Copyright 2018 Nikita Shakarun
 * Copyright 2020 Yury Kharchenko
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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.microedition.shell.MicroActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.base.BaseActivity;
import com.nsomatrix.neutron.databinding.ActivityConfigBinding;
import com.nsomatrix.neutron.util.FileUtils;

import static com.nsomatrix.neutron.util.Constants.*;

public class ConfigActivity extends BaseActivity implements ShaderTuneDialog.Callback {

	private static final String TAG = ConfigActivity.class.getSimpleName();

	private ConfigViewModel viewModel;
	private ConfigPagerAdapter pagerAdapter;
	private FragmentManager fragmentManager;
	private Display display;
	private ActivityConfigBinding binding;

	@SuppressLint({"StringFormatMatches", "StringFormatInvalid"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);

		Intent intent = getIntent();
		String action = intent.getAction();
		boolean isProfile = ACTION_EDIT_PROFILE.equals(action);
		boolean needShow = isProfile || ACTION_EDIT.equals(action);
		viewModel.setProfile(isProfile);
		viewModel.setNeedShow(needShow);

		String path = intent.getDataString();
		if (path == null) {
			viewModel.setNeedShow(false);
			finish();
			return;
		}
		viewModel.setPath(path);

		File configDir;
		String workDir;
		if (isProfile) {
			setResult(RESULT_OK, new Intent().setData(intent.getData()));
			configDir = new File(Config.getProfilesDir(), path);
			workDir = Config.getEmulatorDir();
			setTitle(path);
		} else {
			String name = intent.getStringExtra(KEY_MIDLET_NAME);
			viewModel.setMidletName(name);
			viewModel.setStartArguments(intent.getStringExtra(KEY_START_ARGUMENTS));
			setTitle(name);
			File appDir = new File(path);
			File convertedDir = appDir.getParentFile();
			if (!appDir.isDirectory() || convertedDir == null
					|| (workDir = convertedDir.getParent()) == null) {
				viewModel.setNeedShow(false);
				String storageName = "";
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
					if (sm != null) {
						StorageVolume storageVolume = sm.getStorageVolume(appDir);
						if (storageVolume != null) {
							String desc = storageVolume.getDescription(this);
							if (desc != null) {
								storageName = "\"" + desc + "\" ";
							}
						}
					}
				}
				new AlertDialog.Builder(this)
						.setTitle(R.string.error)
						.setMessage(getString(R.string.err_missing_app, storageName))
						.setPositiveButton(R.string.exit, (d, w) -> finish())
						.setCancelable(false)
						.show();
				return;
			}
			File dataDir = new File(workDir + Config.MIDLET_DATA_DIR + appDir.getName());
			dataDir.mkdirs();
			viewModel.setDataDir(dataDir);
			configDir = new File(workDir + Config.MIDLET_CONFIGS_DIR + appDir.getName());
		}
		configDir.mkdirs();
		viewModel.setConfigDir(configDir);
		viewModel.setWorkDir(workDir);

		String defProfile = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.getString(PREF_DEFAULT_PROFILE, null);
		viewModel.setDefProfile(defProfile);

		loadConfig();
		if (!viewModel.getParams().isNew && !needShow) {
			startMIDlet();
			return;
		}
		loadKeyLayout();

		binding = ActivityConfigBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		display = getWindowManager().getDefaultDisplay();
		fragmentManager = getSupportFragmentManager();

		fillScreenSizePresets(display.getWidth(), display.getHeight());

		viewModel.addFontSizePreset("128 x 128", 9, 13, 15);
		viewModel.addFontSizePreset("128 x 160", 13, 15, 20);
		viewModel.addFontSizePreset("176 x 220", 15, 18, 22);
		viewModel.addFontSizePreset("240 x 320", 18, 22, 26);
		viewModel.addFontSizePreset("360 x 640", 22, 26, 30);

		pagerAdapter = new ConfigPagerAdapter(this);
		binding.viewPager.setAdapter(pagerAdapter);
		binding.viewPager.setOffscreenPageLimit(3);

		new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
			switch (position) {
				case 0:
					tab.setText(R.string.PREF_SCREEN_OPTIONS);
					break;
				case 1:
					tab.setText(R.string.PREF_FONT_OPTIONS);
					break;
				case 2:
					tab.setText(R.string.pref_input_devices_title);
					break;
				case 3:
					tab.setText(R.string.PREF_SYS_PROPS);
					break;
			}
		}).attach();
	}

	void loadConfig() {
		File configDir = viewModel.getConfigDir();
		String defProfile = viewModel.getDefProfile();
		ProfileModel params = ProfilesManager.loadConfig(configDir);
		if (params == null && defProfile != null) {
			FileUtils.copyFiles(new File(Config.getProfilesDir(), defProfile), configDir, null);
			params = ProfilesManager.loadConfig(configDir);
		}
		if (params == null) {
			params = new ProfileModel(configDir);
		}
		viewModel.setParams(params);
	}

	private void loadKeyLayout() {
		File configDir = viewModel.getConfigDir();
		File file = new File(configDir, Config.MIDLET_KEY_LAYOUT_FILE);
		viewModel.setKeylayoutFile(file);
		if (viewModel.isProfile() || file.exists()) {
			return;
		}
		String defProfile = viewModel.getDefProfile();
		if (defProfile == null) {
			return;
		}
		File defaultKeyLayoutFile = new File(Config.getProfilesDir() + defProfile, Config.MIDLET_KEY_LAYOUT_FILE);
		if (!defaultKeyLayoutFile.exists()) {
			return;
		}
		try {
			FileUtils.copyFileUsingChannel(defaultKeyLayoutFile, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPause() {
		if (viewModel.isNeedShow() && viewModel.getConfigDir() != null) {
			saveParams();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (viewModel.isNeedShow()) {
			loadParams(true);
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		fillScreenSizePresets(display.getWidth(), display.getHeight());
	}

	private void fillScreenSizePresets(int w, int h) {
		ArrayList<String> screenPresets = viewModel.getScreenPresets();
		screenPresets.clear();

		screenPresets.add("128 x 128");
		screenPresets.add("128 x 160");
		screenPresets.add("132 x 176");
		screenPresets.add("176 x 220");
		screenPresets.add("240 x 320");
		screenPresets.add("352 x 416");
		screenPresets.add("640 x 360");
		screenPresets.add("800 x 480");

		if (w > h) {
			screenPresets.add(h * 3 / 4 + " x " + h);
			screenPresets.add(h * 4 / 3 + " x " + h);
		} else {
			screenPresets.add(w + " x " + w * 4 / 3);
			screenPresets.add(w + " x " + w * 3 / 4);
		}

		screenPresets.add(w + " x " + h);
		Set<String> preset = PreferenceManager.getDefaultSharedPreferences(this)
				.getStringSet("ResolutionsPreset", null);
		if (preset != null) {
			screenPresets.addAll(preset);
		}
		Collections.sort(screenPresets, (o1, o2) -> {
			int sep1 = o1.indexOf(" x ");
			int sep2 = o2.indexOf(" x ");
			if (sep1 == -1) {
				if (sep2 != -1) return -1;
				else return 0;
			} else if (sep2 == -1) return 1;
			int r = Integer.decode(o1.substring(0, sep1)).compareTo(Integer.decode(o2.substring(0, sep2)));
			if (r != 0) return r;
			return Integer.decode(o1.substring(sep1 + 3)).compareTo(Integer.decode(o2.substring(sep2 + 3)));
		});
		String prev = null;
		for (Iterator<String> iterator = screenPresets.iterator(); iterator.hasNext(); ) {
			String next = iterator.next();
			if (next.equals(prev)) iterator.remove();
			else prev = next;
		}
	}

	public void loadParams(boolean reloadFromFile) {
		if (reloadFromFile) {
			loadConfig();
		} else {
			viewModel.setParams(viewModel.getParams());
		}
	}

	private void saveParams() {
		try {
			ProfileModel params = viewModel.getParams();
			if (params == null) return;

			if (pagerAdapter != null) {
				if (pagerAdapter.getDisplayFragment() != null) {
					pagerAdapter.getDisplayFragment().saveParams(params);
				}
				if (pagerAdapter.getFontFragment() != null) {
					pagerAdapter.getFontFragment().saveParams(params);
				}
				if (pagerAdapter.getInputFragment() != null) {
					pagerAdapter.getInputFragment().saveParams(params);
				}
				if (pagerAdapter.getSystemFragment() != null) {
					pagerAdapter.getSystemFragment().saveParams(params);
				}
			}

			ProfilesManager.saveConfig(params);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config, menu);
		if (viewModel.isProfile()) {
			menu.findItem(R.id.action_start).setVisible(false);
			menu.findItem(R.id.action_clear_data).setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_start) {
			startMIDlet();
		} else if (itemId == R.id.action_clear_data) {
			showClearDataDialog();
		} else if (itemId == R.id.action_reset_settings) {
			ProfileModel newParams = new ProfileModel(viewModel.getConfigDir());
			viewModel.setParams(newParams);
		} else if (itemId == R.id.action_reset_layout) {
			File keylayoutFile = viewModel.getKeylayoutFile();
			if (keylayoutFile != null) {
				//noinspection ResultOfMethodCallIgnored
				keylayoutFile.delete();
			}
			loadKeyLayout();
		} else if (itemId == R.id.action_load_profile) {
			File keylayoutFile = viewModel.getKeylayoutFile();
			if (keylayoutFile != null) {
				LoadProfileDialog.newInstance(keylayoutFile.getParent())
						.show(fragmentManager, "load_profile");
			}
		} else if (itemId == R.id.action_save_profile) {
			saveParams();
			File keylayoutFile = viewModel.getKeylayoutFile();
			if (keylayoutFile != null) {
				SaveProfileDialog.getInstance(keylayoutFile.getParent())
						.show(fragmentManager, "save_profile");
			}
		} else if (itemId == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	private void showClearDataDialog() {
		File dataDir = viewModel.getDataDir();
		if (dataDir == null) return;
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_clear_data)
				.setPositiveButton(android.R.string.ok, (d, w) -> FileUtils.clearDirectory(dataDir))
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void startMIDlet() {
		saveParams();
		Intent i = new Intent(this, MicroActivity.class);
		i.setData(getIntent().getData());
		i.putExtra(KEY_MIDLET_NAME, getIntent().getStringExtra(KEY_MIDLET_NAME));
		i.putExtra(KEY_START_ARGUMENTS, viewModel.getStartArguments());
		startActivity(i);
		finish();
	}

	@Override
	public void onTuneComplete(float[] values) {
		ProfileModel params = viewModel.getParams();
		if (params != null && params.shader != null) {
			params.shader.values = values;
			viewModel.setParams(params);
		}
	}
}
