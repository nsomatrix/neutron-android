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

package com.nsomatrix.neutron.applist;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.FragmentGameOptionsBottomSheetBinding;

public class GameOptionsBottomSheet extends BottomSheetDialogFragment {

	public interface GameOptionsListener {
		void onPlay(AppItem item);
		void onToggleFavorite(AppItem item);
		void onOpenSettings(AppItem item);
		void onAddShortcut(AppItem item);
		void onRename(AppItem item);
		void onReinstall(AppItem item);
		void onDelete(AppItem item);
	}

	private AppItem appItem;
	private boolean isFavorite;
	private GameOptionsListener listener;
	private FragmentGameOptionsBottomSheetBinding binding;

	public static GameOptionsBottomSheet newInstance(AppItem item, boolean isFavorite) {
		GameOptionsBottomSheet fragment = new GameOptionsBottomSheet();
		fragment.appItem = item;
		fragment.isFavorite = isFavorite;
		return fragment;
	}

	public void setListener(GameOptionsListener listener) {
		this.listener = listener;
	}

	@Override
	public int getTheme() {
		return R.style.AppBottomSheetDialogTheme;
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog instanceof BottomSheetDialog) {
			BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
			FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (bottomSheet != null) {
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentGameOptionsBottomSheetBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (appItem == null) {
			dismiss();
			return;
		}

		// Dynamic navigation bar window insets for edge-to-edge
		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
			Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
			int baseBottomPadding = (int) (24 * getResources().getDisplayMetrics().density);
			binding.sheetContentContainer.setPadding(
					binding.sheetContentContainer.getPaddingLeft(),
					binding.sheetContentContainer.getPaddingTop(),
					binding.sheetContentContainer.getPaddingRight(),
					baseBottomPadding + navInsets.bottom
			);
			return windowInsets;
		});

		// Setup icon
		String iconPath = appItem.getImagePathExt();
		if (iconPath != null) {
			Drawable icon = Drawable.createFromPath(iconPath);
			if (icon != null) {
				icon.setFilterBitmap(false);
				binding.sheetGameIcon.setImageDrawable(icon);
			} else {
				binding.sheetGameIcon.setImageResource(R.mipmap.ic_launcher);
			}
		} else {
			binding.sheetGameIcon.setImageResource(R.mipmap.ic_launcher);
		}

		// Setup title & metadata
		binding.sheetGameTitle.setText(appItem.getTitle());
		binding.sheetGameVendor.setText(appItem.getAuthor());
		binding.sheetGameVersion.setText(getString(R.string.version) + appItem.getVersion());

		updateFavoriteUi();

		// Actions
		binding.sheetBtnPlay.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onPlay(appItem);
		});

		binding.sheetActionFavorite.setOnClickListener(v -> {
			isFavorite = !isFavorite;
			updateFavoriteUi();
			if (listener != null) listener.onToggleFavorite(appItem);
		});

		binding.sheetActionSettings.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onOpenSettings(appItem);
		});

		binding.sheetActionShortcut.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onAddShortcut(appItem);
		});

		binding.sheetActionRename.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onRename(appItem);
		});

		binding.sheetActionReinstall.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onReinstall(appItem);
		});

		binding.sheetActionDelete.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onDelete(appItem);
		});
	}

	private void updateFavoriteUi() {
		if (isFavorite) {
			binding.sheetIconFavorite.setImageResource(R.drawable.ic_star_filled);
			binding.sheetTextFavorite.setText(R.string.action_unfavorite);
		} else {
			binding.sheetIconFavorite.setImageResource(R.drawable.ic_star_outline);
			binding.sheetTextFavorite.setText(R.string.action_favorite);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
