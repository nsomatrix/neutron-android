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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.FragmentConfigDisplayBinding;
import com.nsomatrix.neutron.util.FileUtils;
import com.nsomatrix.neutron.util.MenuFontHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DisplayConfigFragment extends Fragment implements View.OnClickListener {
	private static final String TAG = DisplayConfigFragment.class.getSimpleName();

	private FragmentConfigDisplayBinding binding;
	private ConfigViewModel viewModel;
	private ArrayAdapter<ShaderInfo> spShaderAdapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentConfigDisplayBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

		binding.keepAspectRatioToggle.setOnCheckedChangeListener(this::onLockAspectChanged);
		binding.showScreenSizePresets.setOnClickListener(this::showScreenPresets);
		binding.swapScreenSides.setOnClickListener(this);
		binding.addScreenSizeToPresets.setOnClickListener(v -> addResolutionToPresets());
		binding.tuneSelectedShader.setOnClickListener(this::showShaderSettings);

		binding.scaleRatio.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				int length = s.length();
				if (length > 4) {
					if (start >= 4) {
						binding.scaleRatio.getText().delete(4, length);
					} else {
						int st = start + count;
						int end = st + (before == 0 ? count : before);
						binding.scaleRatio.getText().delete(st, Math.min(end, length));
					}
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0) return;
				try {
					int progress = Integer.parseInt(s.toString());
					if (progress > 1000) {
						s.replace(0, s.length(), "1000");
					}
				} catch (NumberFormatException e) {
					s.clear();
				}
			}
		});

		binding.graphicalModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
					case 0:
					case 3:
						binding.parallelScreenRedrawingToggle.setVisibility(View.VISIBLE);
						binding.shaderRoot.setVisibility(View.GONE);
						break;
					case 1:
						binding.parallelScreenRedrawingToggle.setVisibility(View.GONE);
						initShaderSpinner();
						break;
					case 2:
						binding.parallelScreenRedrawingToggle.setVisibility(View.GONE);
						binding.shaderRoot.setVisibility(View.GONE);
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		viewModel.getParamsLiveData().observe(getViewLifecycleOwner(), params -> {
			if (params != null) {
				loadParams(params);
			}
		});

		if (viewModel.getParams() != null) {
			loadParams(viewModel.getParams());
		}
	}

	private void onLockAspectChanged(CompoundButton cb, boolean isChecked) {
		if (isChecked) {
			float w;
			try {
				w = Integer.parseInt(binding.screenWidth.getText().toString());
			} catch (Exception ignored) {
				w = 0;
			}
			if (w <= 0) {
				cb.setChecked(false);
				return;
			}
			float h;
			try {
				h = Integer.parseInt(binding.screenHeight.getText().toString());
			} catch (Exception ignored) {
				h = 0;
			}
			if (h <= 0) {
				cb.setChecked(false);
				return;
			}
			float finalW = w;
			float finalH = h;
			binding.screenWidth.setOnFocusChangeListener(new ResolutionAutoFill(
					binding.screenWidth, binding.screenHeight, finalH / finalW));
			binding.screenHeight.setOnFocusChangeListener(new ResolutionAutoFill(
					binding.screenHeight, binding.screenWidth, finalW / finalH));
		} else {
			View.OnFocusChangeListener listener = binding.screenWidth.getOnFocusChangeListener();
			if (listener != null) {
				listener.onFocusChange(binding.screenWidth, false);
				binding.screenWidth.setOnFocusChangeListener(null);
			}
			listener = binding.screenHeight.getOnFocusChangeListener();
			if (listener != null) {
				listener.onFocusChange(binding.screenHeight, false);
				binding.screenHeight.setOnFocusChangeListener(null);
			}
		}
	}

	@SuppressLint("SetTextI18n")
	public void loadParams(ProfileModel params) {
		if (binding == null) return;
		int screenWidth = params.screenWidth;
		if (screenWidth != 0) {
			binding.screenWidth.setText(Integer.toString(screenWidth));
		}
		int screenHeight = params.screenHeight;
		if (screenHeight != 0) {
			binding.screenHeight.setText(Integer.toString(screenHeight));
		}
		binding.scaleRatio.setText(Integer.toString(params.screenScaleRatio));
		binding.screenOrientationSelector.setSelection(params.orientation);
		binding.scaleTypeSelector.setSelection(params.screenScaleType);
		binding.screenGravitySelector.setSelection(params.screenGravity);
		binding.filteringToggle.setChecked(params.screenFilter);
		binding.immediateProcessingToggle.setChecked(params.immediateMode);
		binding.parallelScreenRedrawingToggle.setChecked(params.parallelRedrawScreen);
		binding.forceFullscreenToggle.setChecked(params.forceFullscreen);
		binding.graphicalModeSelector.setSelection(params.graphicsMode);
		binding.showFpsToggle.setChecked(params.showFps);
		binding.shaderSelector.setSelection(0);
		if (spShaderAdapter != null) {
			ShaderInfo shader = params.shader;
			int position = shader == null ? -1 : spShaderAdapter.getPosition(shader);
			if (position > 0) {
				spShaderAdapter.getItem(position).values = shader.values;
				binding.shaderSelector.setSelection(position);
			}
		}
		int fpsLimit = params.fpsLimit;
		binding.fpsLimit.setText(fpsLimit > 0 ? Integer.toString(fpsLimit) : "");
	}

	public void saveParams(ProfileModel params) {
		if (binding == null || params == null) return;
		params.screenWidth = parseInt(binding.screenWidth.getText().toString());
		params.screenHeight = parseInt(binding.screenHeight.getText().toString());
		try {
			params.screenScaleRatio = Integer.parseInt(binding.scaleRatio.getText().toString());
		} catch (NumberFormatException e) {
			params.screenScaleRatio = 100;
		}
		params.orientation = binding.screenOrientationSelector.getSelectedItemPosition();
		params.screenGravity = binding.screenGravitySelector.getSelectedItemPosition();
		params.screenScaleType = binding.scaleTypeSelector.getSelectedItemPosition();
		params.screenFilter = binding.filteringToggle.isChecked();
		params.immediateMode = binding.immediateProcessingToggle.isChecked();
		int mode = binding.graphicalModeSelector.getSelectedItemPosition();
		params.graphicsMode = mode;
		if (mode == 1) {
			if (binding.shaderSelector.getSelectedItemPosition() == 0)
				params.shader = null;
			else
				params.shader = (ShaderInfo) binding.shaderSelector.getSelectedItem();
		}
		params.parallelRedrawScreen = binding.parallelScreenRedrawingToggle.isChecked();
		params.forceFullscreen = binding.forceFullscreenToggle.isChecked();
		params.showFps = binding.showFpsToggle.isChecked();
		params.fpsLimit = parseInt(binding.fpsLimit.getText().toString());
	}

	private int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void showShaderSettings(View v) {
		ProfileModel params = viewModel.getParams();
		if (params == null) return;
		ShaderInfo shader = (ShaderInfo) binding.shaderSelector.getSelectedItem();
		params.shader = shader;
		ShaderTuneDialog.newInstance(shader).show(getParentFragmentManager(), "ShaderTuning");
	}

	private void initShaderSpinner() {
		if (spShaderAdapter != null) {
			binding.shaderRoot.setVisibility(View.VISIBLE);
			return;
		}
		String workDir = viewModel.getWorkDir();
		if (workDir == null) return;
		File dir = new File(workDir + Config.SHADERS_DIR);
		if (!dir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dir.mkdirs();
		}
		ArrayList<ShaderInfo> infos = new ArrayList<>();
		spShaderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, infos);
		spShaderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.shaderSelector.setAdapter(spShaderAdapter);
		File[] files = dir.listFiles((f) -> f.isFile() && f.getName().toLowerCase().endsWith(".ini"));
		if (files != null) {
			for (File file : files) {
				String text = FileUtils.getText(file.getAbsolutePath());
				String[] split = text.split("[\\n\\r]+");
				ShaderInfo info = null;
				for (String line : split) {
					if (line.startsWith("[")) {
						if (info != null && info.fragment != null && info.vertex != null) {
							infos.add(info);
						}
						info = new ShaderInfo(line.replaceAll("[\\[\\]]", ""), "unknown");
					} else if (info != null) {
						try {
							info.set(line);
						} catch (Exception e) {
							Log.e(TAG, "initShaderSpinner: ", e);
						}
					}
				}
				if (info != null && info.fragment != null && info.vertex != null) {
					infos.add(info);
				}
			}
			Collections.sort(infos);
		}
		infos.add(0, new ShaderInfo(getString(R.string.identity_filter), "woesss"));
		spShaderAdapter.notifyDataSetChanged();
		ProfileModel params = viewModel.getParams();
		ShaderInfo selected = params != null ? params.shader : null;
		if (selected != null) {
			int position = infos.indexOf(selected);
			if (position > 0) {
				infos.get(position).values = selected.values;
				binding.shaderSelector.setSelection(position);
			}
		}
		binding.shaderRoot.setVisibility(View.VISIBLE);
		binding.shaderSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				ShaderInfo item = (ShaderInfo) parent.getItemAtPosition(position);
				ShaderInfo.Setting[] settings = item.settings;
				float[] values = item.values;
				if (values == null) {
					for (int i = 0; i < 4; i++) {
						if (settings[i] != null) {
							if (values == null) {
								values = new float[4];
							}
							values[i] = settings[i].def;
						}
					}
				}
				if (values == null) {
					binding.tuneSelectedShader.setVisibility(View.GONE);
				} else {
					item.values = values;
					binding.tuneSelectedShader.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	}

	private void showScreenPresets(View v) {
		PopupMenu popup = new PopupMenu(requireContext(), v);
		Menu menu = popup.getMenu();
		for (String preset : viewModel.getScreenPresets()) {
			menu.add(preset);
		}
		MenuFontHelper.applyGoogleSans(menu);
		popup.setOnMenuItemClickListener(item -> {
			String string = item.getTitle().toString();
			int separator = string.indexOf(" x ");
			binding.screenWidth.setText(string.substring(0, separator));
			binding.screenHeight.setText(string.substring(separator + 3));
			return true;
		});
		popup.show();
	}

	private void addResolutionToPresets() {
		String width = binding.screenWidth.getText().toString();
		String height = binding.screenHeight.getText().toString();
		if (width.isEmpty()) width = "-1";
		if (height.isEmpty()) height = "-1";
		int w = parseInt(width);
		int h = parseInt(height);
		if (w <= 0 || h <= 0) {
			Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
			return;
		}
		String preset = width + " x " + height;
		ArrayList<String> screenPresets = viewModel.getScreenPresets();
		if (screenPresets.contains(preset)) {
			Toast.makeText(requireContext(), R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
			return;
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
		Set<String> set = preferences.getStringSet("ResolutionsPreset", null);
		if (set == null) {
			set = new HashSet<>(1);
		}
		if (set.add(preset)) {
			preferences.edit().putStringSet("ResolutionsPreset", set).apply();
			screenPresets.add(preset);
			Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(requireContext(), R.string.not_saved_exists, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.swap_screen_sides) {
			String tmp = binding.screenWidth.getText().toString();
			binding.screenWidth.setText(binding.screenHeight.getText().toString());
			binding.screenHeight.setText(tmp);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
