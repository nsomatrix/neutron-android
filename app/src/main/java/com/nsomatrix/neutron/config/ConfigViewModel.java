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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;

public class ConfigViewModel extends ViewModel {

	private ProfileModel params;
	private File configDir;
	private File keylayoutFile;
	private File dataDir;
	private String defProfile;
	private String workDir;
	private boolean isProfile;
	private boolean needShow;
	private String midletName;
	private String startArguments;
	private String path;

	private final ArrayList<String> screenPresets = new ArrayList<>();
	private final ArrayList<int[]> fontPresetValues = new ArrayList<>();
	private final ArrayList<String> fontPresetTitles = new ArrayList<>();

	private final MutableLiveData<ProfileModel> paramsLiveData = new MutableLiveData<>();
	private final MutableLiveData<Boolean> configLoadedEvent = new MutableLiveData<>();

	public ProfileModel getParams() {
		return params;
	}

	public void setParams(ProfileModel params) {
		this.params = params;
		paramsLiveData.setValue(params);
	}

	public MutableLiveData<ProfileModel> getParamsLiveData() {
		return paramsLiveData;
	}

	public File getConfigDir() {
		return configDir;
	}

	public void setConfigDir(File configDir) {
		this.configDir = configDir;
	}

	public File getKeylayoutFile() {
		return keylayoutFile;
	}

	public void setKeylayoutFile(File keylayoutFile) {
		this.keylayoutFile = keylayoutFile;
	}

	public File getDataDir() {
		return dataDir;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public String getDefProfile() {
		return defProfile;
	}

	public void setDefProfile(String defProfile) {
		this.defProfile = defProfile;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public boolean isProfile() {
		return isProfile;
	}

	public void setProfile(boolean profile) {
		isProfile = profile;
	}

	public boolean isNeedShow() {
		return needShow;
	}

	public void setNeedShow(boolean needShow) {
		this.needShow = needShow;
	}

	public String getMidletName() {
		return midletName;
	}

	public void setMidletName(String midletName) {
		this.midletName = midletName;
	}

	public String getStartArguments() {
		return startArguments;
	}

	public void setStartArguments(String startArguments) {
		this.startArguments = startArguments;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ArrayList<String> getScreenPresets() {
		return screenPresets;
	}

	public ArrayList<int[]> getFontPresetValues() {
		return fontPresetValues;
	}

	public ArrayList<String> getFontPresetTitles() {
		return fontPresetTitles;
	}

	public void addFontSizePreset(String title, int small, int medium, int large) {
		fontPresetValues.add(new int[]{small, medium, large});
		fontPresetTitles.add(title);
	}

	public MutableLiveData<Boolean> getConfigLoadedEvent() {
		return configLoadedEvent;
	}

	public void notifyConfigLoaded() {
		configLoadedEvent.setValue(true);
	}
}
