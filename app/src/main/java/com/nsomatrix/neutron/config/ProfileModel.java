/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.nsomatrix.neutron.config;

import android.util.SparseIntArray;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.io.File;

import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.util.ContextHolder;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.util.SparseIntArrayAdapter;

public class ProfileModel {
	public static final int VERSION = 4;
	/** True if this is a new profile (not yet saved to file) */
	public final transient boolean isNew;

	public transient File dir;

	@SerializedName("Version")
	public int version;

	@SerializedName("ScreenWidth")
	public int screenWidth;

	@SerializedName("ScreenHeight")
	public int screenHeight;

	@SerializedName("ScreenBackgroundColor")
	public int screenBackgroundColor;

	@SerializedName("ScreenScaleRatio")
	public int screenScaleRatio;

	@SerializedName("Orientation")
	public int orientation;

	@SerializedName("ScreenScaleToFit")
	public boolean screenScaleToFit;

	@SerializedName("ScreenKeepAspectRatio")
	public boolean screenKeepAspectRatio;

	@SerializedName("ScreenScaleType")
	public int screenScaleType;

	@SerializedName("ScreenGravity")
	public int screenGravity;

	@SerializedName("ScreenFilter")
	public boolean screenFilter;

	@SerializedName("ImmediateMode")
	public boolean immediateMode;

	@SerializedName("HwAcceleration")
	public boolean hwAcceleration;

	@SerializedName("GraphicsMode")
	public int graphicsMode;

	@SerializedName("Shader")
	public ShaderInfo shader;

	@SerializedName("ParallelRedrawScreen")
	public boolean parallelRedrawScreen;

	@SerializedName("ShowFps")
	public boolean showFps;

	@SerializedName("FpsLimit")
	public int fpsLimit;

	@SerializedName("ForceFullscreen")
	public boolean forceFullscreen;

	@SerializedName("FontSizeSmall")
	public int fontSizeSmall;

	@SerializedName("FontSizeMedium")
	public int fontSizeMedium;

	@SerializedName("FontSizeLarge")
	public int fontSizeLarge;

	@SerializedName("FontApplyDimensions")
	public boolean fontApplyDimensions;

	@SerializedName("FontAntiAlias")
	public boolean fontAA;

	@SerializedName("TouchInput")
	public boolean touchInput;

	@SerializedName("ShowKeyboard")
	public boolean showKeyboard;

	@SerializedName("VirtualKeyboardType")
	public int vkType;

	@SerializedName("ButtonShape")
	public int vkButtonShape;

	@SerializedName("VirtualKeyboardAlpha")
	public int vkAlpha;

	@SerializedName("VirtualKeyboardForceOpacity")
	public boolean vkForceOpacity;

	@SerializedName("VirtualKeyboardFeedback")
	public boolean vkFeedback;

	@SerializedName("VirtualKeyboardDelay")
	public int vkHideDelay;

	@SerializedName("UseThemeColors")
	public boolean useThemeColors = true;

	@SerializedName("VirtualKeyboardColorBackground")
	public int vkBgColor;

	@SerializedName("VirtualKeyboardColorBackgroundSelected")
	public int vkBgColorSelected;

	@SerializedName("VirtualKeyboardColorForeground")
	public int vkFgColor;

	@SerializedName("VirtualKeyboardColorForegroundSelected")
	public int vkFgColorSelected;

	@SerializedName("VirtualKeyboardColorOutline")
	public int vkOutlineColor;

	@SerializedName("Layout")
	public int keyCodesLayout;

	@JsonAdapter(SparseIntArrayAdapter.class)
	@SerializedName("KeyCodeMap")
	public SparseIntArray keyCodeMap;

	@JsonAdapter(SparseIntArrayAdapter.class)
	@SerializedName("KeyMappings")
	public SparseIntArray keyMappings;

	@SerializedName("SystemProperties")
	public String systemProperties;

	@SuppressWarnings("unused") // Gson uses default constructor if present
	public ProfileModel() {
		isNew = false;
		useThemeColors = true;
	}

	public ProfileModel(File dir) {
		this.dir = dir;
		isNew = true;
		version = VERSION;
		screenWidth = 240;
		screenHeight = 320;
		screenScaleType = 1;
		screenGravity = 1;
		screenScaleRatio = 100;
		screenScaleToFit = true;
		screenKeepAspectRatio = true;
		graphicsMode = 1;

		fontSizeSmall = 18;
		fontSizeMedium = 22;
		fontSizeLarge = 26;
		fontAA = true;

		showKeyboard = true;
		touchInput = true;

		vkButtonShape = VirtualKeyboard.ROUND_RECT_SHAPE;
		vkAlpha = 64;

		Context context = ContextHolder.getAppContext();
		if (context != null) {
			screenBackgroundColor = ContextCompat.getColor(context, R.color.background) & 0x00FFFFFF;
			vkBgColor = ContextCompat.getColor(context, R.color.vk_bg) & 0x00FFFFFF;
			vkFgColor = ContextCompat.getColor(context, R.color.vk_fg) & 0x00FFFFFF;
			vkBgColorSelected = ContextCompat.getColor(context, R.color.vk_bg_selected) & 0x00FFFFFF;
			vkFgColorSelected = ContextCompat.getColor(context, R.color.vk_fg_selected) & 0x00FFFFFF;
			vkOutlineColor = ContextCompat.getColor(context, R.color.vk_outline) & 0x00FFFFFF;
		} else {
			screenBackgroundColor = 0xF6F8FA;
			vkBgColor = 0xFFFFFF;
			vkFgColor = 0x1F2328;
			vkBgColorSelected = 0xE5E9EF;
			vkFgColorSelected = 0x1F2328;
			vkOutlineColor = 0xD0D7DE;
		}
		systemProperties = ContextHolder.getAssetAsString("defaults/system.props");
	}
}
