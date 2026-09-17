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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.FragmentConfigInputBinding;
import com.nsomatrix.neutron.settings.KeyMapperActivity;

import java.io.File;

public class InputConfigFragment extends Fragment implements View.OnClickListener {

	private FragmentConfigInputBinding binding;
	private ConfigViewModel viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentConfigInputBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

		binding.showKeyMappings.setOnClickListener(this);
		binding.showVirtualKeyboardToggle.setOnClickListener((b) -> {
			binding.virtualKeyboardConfigGroup.setVisibility(
					binding.showVirtualKeyboardToggle.isChecked() ? View.VISIBLE : View.GONE);
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

	@SuppressLint("SetTextI18n")
	public void loadParams(ProfileModel params) {
		if (binding == null) return;
		boolean showVk = params.showKeyboard;
		binding.showVirtualKeyboardToggle.setChecked(showVk);
		binding.virtualKeyboardConfigGroup.setVisibility(showVk ? View.VISIBLE : View.GONE);
		binding.enableHapticFeedbackToggle.setChecked(params.vkFeedback);
		binding.forceOpacityForOffscreenKeysToggle.setChecked(params.vkForceOpacity);
		binding.enableTouchInputToggle.setChecked(params.touchInput);

		binding.buttonsLayoutSelector.setSelection(params.keyCodesLayout);
		binding.buttonShapeSelector.setSelection(params.vkButtonShape);
		binding.changeOpacitySeekbar.setProgress(params.vkAlpha);
		int vkHideDelay = params.vkHideDelay;
		binding.virtualKeyboardHideDelay.setText(vkHideDelay > 0 ? Integer.toString(vkHideDelay) : "");
	}

	public void saveParams(ProfileModel params) {
		if (binding == null || params == null) return;
		params.showKeyboard = binding.showVirtualKeyboardToggle.isChecked();
		params.vkFeedback = binding.enableHapticFeedbackToggle.isChecked();
		params.vkForceOpacity = binding.forceOpacityForOffscreenKeysToggle.isChecked();
		params.touchInput = binding.enableTouchInputToggle.isChecked();

		params.keyCodesLayout = binding.buttonsLayoutSelector.getSelectedItemPosition();
		params.vkButtonShape = binding.buttonShapeSelector.getSelectedItemPosition();
		params.vkAlpha = binding.changeOpacitySeekbar.getProgress();
		params.vkHideDelay = parseInt(binding.virtualKeyboardHideDelay.getText().toString());
	}

	private int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.show_key_mappings) {
			File configDir = viewModel.getConfigDir();
			if (configDir == null) return;
			Intent i = new Intent(requireActivity().getIntent().getAction(),
					Uri.parse(configDir.getPath()),
					requireContext(), KeyMapperActivity.class);
			startActivity(i);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
