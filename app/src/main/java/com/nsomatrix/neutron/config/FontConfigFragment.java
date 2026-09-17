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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.FragmentConfigFontBinding;

public class FontConfigFragment extends Fragment implements View.OnClickListener {

	private FragmentConfigFontBinding binding;
	private ConfigViewModel viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentConfigFontBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

		binding.showFontSizePresets.setOnClickListener(this);

		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				updateLivePreview();
			}
		};

		binding.fontSizeSmall.addTextChangedListener(textWatcher);
		binding.fontSizeMedium.addTextChangedListener(textWatcher);
		binding.fontSizeLarge.addTextChangedListener(textWatcher);

		binding.showFontSizesInScaledPixelsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview());
		binding.enableAntiAliasingToggle.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview());

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
		binding.fontSizeSmall.setText(Integer.toString(params.fontSizeSmall));
		binding.fontSizeMedium.setText(Integer.toString(params.fontSizeMedium));
		binding.fontSizeLarge.setText(Integer.toString(params.fontSizeLarge));
		binding.showFontSizesInScaledPixelsToggle.setChecked(params.fontApplyDimensions);
		binding.enableAntiAliasingToggle.setChecked(params.fontAA);
		updateLivePreview();
	}

	public void saveParams(ProfileModel params) {
		if (binding == null || params == null) return;
		try {
			params.fontSizeSmall = Integer.parseInt(binding.fontSizeSmall.getText().toString());
		} catch (NumberFormatException e) {
			params.fontSizeSmall = 0;
		}
		try {
			params.fontSizeMedium = Integer.parseInt(binding.fontSizeMedium.getText().toString());
		} catch (NumberFormatException e) {
			params.fontSizeMedium = 0;
		}
		try {
			params.fontSizeLarge = Integer.parseInt(binding.fontSizeLarge.getText().toString());
		} catch (NumberFormatException e) {
			params.fontSizeLarge = 0;
		}
		params.fontApplyDimensions = binding.showFontSizesInScaledPixelsToggle.isChecked();
		params.fontAA = binding.enableAntiAliasingToggle.isChecked();
	}

	private void updateLivePreview() {
		if (binding == null) return;
		int smallSize = parseInt(binding.fontSizeSmall.getText().toString(), 14);
		int mediumSize = parseInt(binding.fontSizeMedium.getText().toString(), 18);
		int largeSize = parseInt(binding.fontSizeLarge.getText().toString(), 22);

		boolean useSp = binding.showFontSizesInScaledPixelsToggle.isChecked();
		int unit = useSp ? TypedValue.COMPLEX_UNIT_SP : TypedValue.COMPLEX_UNIT_PX;

		binding.tvPreviewSmall.setTextSize(unit, Math.max(8, smallSize));
		binding.tvPreviewMedium.setTextSize(unit, Math.max(10, mediumSize));
		binding.tvPreviewLarge.setTextSize(unit, Math.max(12, largeSize));
	}

	private int parseInt(String s, int def) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.show_font_size_presets) {
			new AlertDialog.Builder(requireContext())
					.setTitle(getString(R.string.SIZE_PRESETS))
					.setItems(viewModel.getFontPresetTitles().toArray(new String[0]),
							(dialog, which) -> {
								int[] values = viewModel.getFontPresetValues().get(which);
								binding.fontSizeSmall.setText(Integer.toString(values[0]));
								binding.fontSizeMedium.setText(Integer.toString(values[1]));
								binding.fontSizeLarge.setText(Integer.toString(values[2]));
							})
					.show();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
