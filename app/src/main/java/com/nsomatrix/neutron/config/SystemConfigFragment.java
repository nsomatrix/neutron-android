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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.FragmentConfigSystemBinding;

import java.nio.charset.Charset;
import javax.microedition.util.ContextHolder;

public class SystemConfigFragment extends Fragment implements View.OnClickListener {

	private FragmentConfigSystemBinding binding;
	private ConfigViewModel viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentConfigSystemBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(ConfigViewModel.class);

		binding.updateEncoding.setOnClickListener(this);

		viewModel.getParamsLiveData().observe(getViewLifecycleOwner(), params -> {
			if (params != null) {
				loadParams(params);
			}
		});

		if (viewModel.getParams() != null) {
			loadParams(viewModel.getParams());
		}
	}

	public void loadParams(ProfileModel params) {
		if (binding == null) return;
		String systemProperties = params.systemProperties;
		if (systemProperties == null) {
			systemProperties = ContextHolder.getAssetAsString("defaults/system.props");
		}
		binding.systemProperties.setText(systemProperties);
	}

	public void saveParams(ProfileModel params) {
		if (binding == null || params == null) return;
		params.systemProperties = getSystemProperties();
	}

	@NonNull
	public String getSystemProperties() {
		if (binding == null) return "";
		String s = binding.systemProperties.getText().toString();
		String[] lines = s.split("\\n");
		StringBuilder sb = new StringBuilder(s.length());
		boolean validCharset = false;
		for (int i = lines.length - 1; i >= 0; i--) {
			String line = lines[i];
			if (line.trim().isEmpty()) continue;
			if (line.startsWith("microedition.encoding:")) {
				if (validCharset) continue;
				try {
					Charset.forName(line.substring(22).trim());
					validCharset = true;
				} catch (Exception ignored) {
					continue;
				}
			}
			sb.append(line).append('\n');
		}
		return sb.toString();
	}

	private void showCharsetPicker(View v) {
		String[] charsets = Charset.availableCharsets().keySet().toArray(new String[0]);
		new AlertDialog.Builder(requireContext()).setItems(charsets, (d, w) -> {
			String enc = "microedition.encoding: " + charsets[w];
			String[] props = binding.systemProperties.getText().toString().split("[\\n\\r]+");
			int propsLength = props.length;
			if (propsLength == 0) {
				binding.systemProperties.setText(enc);
				return;
			}
			int i = propsLength - 1;
			while (i >= 0) {
				if (props[i].startsWith("microedition.encoding")) {
					props[i] = enc;
					break;
				}
				i--;
			}
			if (i < 0) {
				binding.systemProperties.append("\n" + enc);
				return;
			}
			binding.systemProperties.setText(TextUtils.join("\n", props));
		}).setTitle(R.string.pref_encoding_title).show();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.update_encoding) {
			showCharsetPicker(v);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
