/*
 *  Copyright 2020-2022 Yury Kharchenko
 *  Copyright 2026 Neutron Emulator Project
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

package com.nsomatrix.neutron.installer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.applist.AppItem;
import com.nsomatrix.neutron.applist.AppListModel;
import com.nsomatrix.neutron.appsdb.AppRepository;
import com.nsomatrix.neutron.config.Config;
import com.nsomatrix.neutron.databinding.DialogInstallerBinding;
import com.nsomatrix.neutron.jar.Descriptor;
import com.nsomatrix.neutron.util.FileUtils;

public class InstallerDialog extends BottomSheetDialogFragment {
	private static final String TAG = InstallerDialog.class.getSimpleName();
	private static final String ARG_URI = "InstallerDialog.uri";
	private static final String ARG_ID = "InstallerDialog.id";

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	private AppRepository appRepository;
	private AppInstaller installer;
	private DialogInstallerBinding binding;

	private final ActivityResultLauncher<String> openFileLauncher = registerForActivityResult(
			FileUtils.getFilePicker(),
			this::onPickFileResult);

	/**
	 * @param uri original uri from intent.
	 * @return A new instance of fragment InstallerDialog.
	 */
	public static InstallerDialog newInstance(Uri uri) {
		InstallerDialog fragment = new InstallerDialog();
		Bundle args = new Bundle();
		args.putParcelable(ARG_URI, uri);
		fragment.setArguments(args);
		fragment.setCancelable(false);
		return fragment;
	}

	public static InstallerDialog newInstance(int id) {
		InstallerDialog fragment = new InstallerDialog();
		Bundle args = new Bundle();
		args.putInt(ARG_ID, id);
		fragment.setArguments(args);
		fragment.setCancelable(false);
		return fragment;
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		AppListModel appListModel = new ViewModelProvider(requireActivity()).get(AppListModel.class);
		appRepository = appListModel.getAppRepository();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			dismissAllowingStateLoss();
		}
	}

	@Override
	public int getTheme() {
		return R.style.AppBottomSheetDialogTheme;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = DialogInstallerBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Dynamic window insets for edge-to-edge navigation bar
		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
			Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
			int baseBottom = (int) (24 * getResources().getDisplayMetrics().density);
			binding.sheetContentContainer.setPadding(
					binding.sheetContentContainer.getPaddingLeft(),
					binding.sheetContentContainer.getPaddingTop(),
					binding.sheetContentContainer.getPaddingRight(),
					baseBottom + navInsets.bottom
			);
			return windowInsets;
		});

		binding.btnCancel.setOnClickListener(v -> cleanupAndDismiss());
		showInspecting();
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
		if (installer != null) {
			return;
		}
		startInitialLoad();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onDestroy() {
		compositeDisposable.dispose();
		super.onDestroy();
	}

	private void cleanupAndDismiss() {
		if (installer != null) {
			installer.deleteTemp();
			installer.clearCache();
		}
		dismiss();
	}

	private void startInitialLoad() {
		Bundle args = requireArguments();
		Uri uri = args.getParcelable(ARG_URI);
		if (uri != null) {
			installApp(null, uri);
			return;
		}
		int id = args.getInt(ARG_ID);
		reinstallApp(id);
	}

	private void installApp(String path, Uri uri) {
		showInspecting();
		installer = new AppInstaller(path, uri, requireActivity().getApplication(), appRepository);
		Disposable disposable = Single.create(installer::loadInfo)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void reinstallApp(int id) {
		showInspecting();
		installer = new AppInstaller(id, requireActivity().getApplication(), appRepository);
		Disposable disposable = Single.create(installer::loadInfo)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	@SuppressLint("CheckResult")
	private void onPickFileResult(Uri uri) {
		if (uri == null) {
			return;
		}
		showInspecting();
		Disposable disposable = installer.updateInfo(uri)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void showInspecting() {
		if (binding == null) return;
		binding.layoutProgress.setVisibility(View.VISIBLE);
		binding.installationProgress.setVisibility(View.VISIBLE);
		binding.installationStatus.setText(R.string.installer_inspecting);
		binding.layoutDiff.setVisibility(View.GONE);
		binding.layoutPrompt.setVisibility(View.GONE);
		binding.layoutSuccess.setVisibility(View.GONE);
		binding.layoutError.setVisibility(View.GONE);
		binding.btnPrimary.setVisibility(View.GONE);
		binding.btnSecondary.setVisibility(View.GONE);
		binding.btnCancel.setText(R.string.CANCEL_CMD);
		binding.btnCancel.setOnClickListener(v -> cleanupAndDismiss());
	}

	private void convert() {
		if (binding == null) return;
		binding.layoutProgress.setVisibility(View.VISIBLE);
		binding.installationProgress.setVisibility(View.VISIBLE);
		binding.installationStatus.setText(R.string.installer_converting);
		binding.layoutDiff.setVisibility(View.GONE);
		binding.layoutPrompt.setVisibility(View.GONE);
		binding.layoutSuccess.setVisibility(View.GONE);
		binding.layoutError.setVisibility(View.GONE);
		binding.btnPrimary.setVisibility(View.GONE);
		binding.btnSecondary.setVisibility(View.GONE);
		binding.btnCancel.setText(R.string.CANCEL_CMD);
		binding.btnCancel.setOnClickListener(v -> cleanupAndDismiss());

		Disposable disposable = Single.create(installer::install)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void populateMetadata(Descriptor nd) {
		if (nd == null || binding == null) return;
		String name = nd.getName();
		if (!TextUtils.isEmpty(name)) {
			binding.sheetInstallerTitle.setText(name);
		}
		String vendor = nd.getVendor();
		if (!TextUtils.isEmpty(vendor)) {
			binding.sheetInstallerVendor.setText(vendor);
		}
		String version = nd.getVersion();
		if (!TextUtils.isEmpty(version)) {
			binding.badgeVersion.setText("v" + version);
			binding.badgeVersion.setVisibility(View.VISIBLE);
		}

		String jarPath = installer.getJar();
		if (jarPath != null) {
			File jarFile = new File(jarPath);
			if (jarFile.exists() && jarFile.length() > 0) {
				binding.badgeSize.setText(formatFileSize(jarFile.length()));
				binding.badgeSize.setVisibility(View.VISIBLE);
			}
		}

		String iconPath = installer.getIconPath();
		if (iconPath != null) {
			Drawable drawable = Drawable.createFromPath(iconPath);
			if (drawable != null) {
				drawable.setFilterBitmap(false);
				binding.sheetInstallerIcon.setImageDrawable(drawable);
			}
		}
	}

	private String formatFileSize(long bytes) {
		if (bytes <= 0) return "";
		if (bytes >= 1024 * 1024) {
			return String.format(Locale.getDefault(), "%.1f MB", (float) bytes / (1024 * 1024));
		} else if (bytes >= 1024) {
			return String.format(Locale.getDefault(), "%.0f KB", (float) bytes / 1024);
		} else {
			return bytes + " B";
		}
	}

	private void onProgress(@NonNull Integer status) {
		if (!isAdded() || binding == null) {
			return;
		}

		if (status == AppInstaller.STATUS_SUCCESS) {
			binding.layoutProgress.setVisibility(View.GONE);
			binding.layoutDiff.setVisibility(View.GONE);
			binding.layoutPrompt.setVisibility(View.GONE);
			binding.layoutError.setVisibility(View.GONE);
			binding.layoutSuccess.setVisibility(View.VISIBLE);

			AppItem app = installer.getExistsApp();
			if (app != null) {
				binding.sheetInstallerTitle.setText(app.getTitle());
				binding.sheetInstallerVendor.setText(app.getAuthor());
				if (!TextUtils.isEmpty(app.getVersion())) {
					binding.badgeVersion.setText("v" + app.getVersion());
					binding.badgeVersion.setVisibility(View.VISIBLE);
				}
				String iconPath = app.getImagePathExt();
				if (iconPath != null) {
					Drawable drawable = Drawable.createFromPath(iconPath);
					if (drawable != null) {
						drawable.setFilterBitmap(false);
						binding.sheetInstallerIcon.setImageDrawable(drawable);
					}
				}
				binding.btnPrimary.setText(R.string.installer_btn_play);
				binding.btnPrimary.setIconResource(R.drawable.ic_play_arrow);
				binding.btnPrimary.setVisibility(View.VISIBLE);
				binding.btnPrimary.setOnClickListener(v -> {
					Config.startApp(requireContext(), app.getTitle(), app.getPathExt(), false);
					dismiss();
				});

				binding.btnSecondary.setText(R.string.action_settings);
				binding.btnSecondary.setIconResource(R.drawable.ic_settings);
				binding.btnSecondary.setVisibility(View.VISIBLE);
				binding.btnSecondary.setOnClickListener(v -> {
					Config.startApp(requireContext(), app.getTitle(), app.getPathExt(), true);
					dismiss();
				});
			}
			binding.btnCancel.setText(R.string.close);
			binding.btnCancel.setOnClickListener(v -> dismiss());
			return;
		}

		Descriptor nd = installer.getNewDescriptor();
		populateMetadata(nd);

		switch (status) {
			case AppInstaller.STATUS_NEW:
				if (installer.getJar() != null) {
					convert();
					return;
				}
				binding.layoutProgress.setVisibility(View.GONE);
				binding.layoutPrompt.setVisibility(View.VISIBLE);
				binding.tvPromptMessage.setText(R.string.warn_install_from_net);
				binding.btnPrimary.setText(R.string.installer_btn_install);
				binding.btnPrimary.setIconResource(0);
				binding.btnPrimary.setVisibility(View.VISIBLE);
				binding.btnPrimary.setOnClickListener(v -> convert());
				break;

			case AppInstaller.STATUS_NEWEST:
			case AppInstaller.STATUS_EQUAL:
			case AppInstaller.STATUS_OLDEST:
				String currentVersion = installer.getCurrentVersion();
				String newVersion = nd != null ? nd.getVersion() : null;

				binding.layoutProgress.setVisibility(View.GONE);
				binding.layoutDiff.setVisibility(View.VISIBLE);
				binding.tvDiffOldVersion.setText(getString(R.string.installer_installed_version, currentVersion != null ? currentVersion : "—"));
				binding.tvDiffNewVersion.setText(getString(R.string.installer_new_version, newVersion != null ? newVersion : "—"));

				if (status == AppInstaller.STATUS_NEWEST) {
					binding.tvDiffTitle.setText(R.string.installer_update_available);
					binding.tvDiffDesc.setText(getString(R.string.reinstall_newest, newVersion, currentVersion));
					binding.btnPrimary.setText(R.string.installer_btn_update);
				} else if (status == AppInstaller.STATUS_OLDEST) {
					binding.tvDiffTitle.setText(R.string.warning);
					binding.tvDiffDesc.setText(getString(R.string.reinstall_older, newVersion, currentVersion));
					binding.btnPrimary.setText(R.string.action_reinstall);
				} else {
					binding.tvDiffTitle.setText(R.string.action_reinstall);
					binding.tvDiffDesc.setText(R.string.reinstall);
					binding.btnPrimary.setText(R.string.action_reinstall);

					AppItem existsApp = installer.getExistsApp();
					if (existsApp != null) {
						binding.btnSecondary.setText(R.string.installer_btn_launch_current);
						binding.btnSecondary.setIconResource(R.drawable.ic_play_arrow);
						binding.btnSecondary.setVisibility(View.VISIBLE);
						binding.btnSecondary.setOnClickListener(v -> {
							cleanupAndDismiss();
							Config.startApp(requireContext(), existsApp.getTitle(), existsApp.getPathExt(), false);
						});
					}
				}
				binding.btnPrimary.setIconResource(0);
				binding.btnPrimary.setVisibility(View.VISIBLE);
				binding.btnPrimary.setOnClickListener(v -> convert());
				break;

			case AppInstaller.STATUS_UNMATCHED:
				binding.layoutProgress.setVisibility(View.GONE);
				binding.layoutPrompt.setVisibility(View.VISIBLE);
				binding.tvPromptMessage.setText(R.string.install_jar_non_matched_jad);
				binding.btnPrimary.setText(R.string.installer_btn_install);
				binding.btnPrimary.setIconResource(0);
				binding.btnPrimary.setVisibility(View.VISIBLE);
				binding.btnPrimary.setOnClickListener(v -> installApp(installer.getJar(), null));
				break;

			case AppInstaller.STATUS_NEED_JAD:
				binding.layoutProgress.setVisibility(View.GONE);
				binding.layoutPrompt.setVisibility(View.VISIBLE);
				binding.tvPromptMessage.setText(R.string.installer_need_jar_desc);
				binding.btnPrimary.setText(R.string.installer_btn_select_jar);
				binding.btnPrimary.setIconResource(R.drawable.ic_folder);
				binding.btnPrimary.setVisibility(View.VISIBLE);
				binding.btnPrimary.setOnClickListener(v -> openFileLauncher.launch(null));
				break;

			default:
				throw new IllegalStateException("Unexpected status value: " + status);
		}
	}

	private void onError(Throwable e) {
		Log.e(TAG, "Installer error", e);
		if (installer != null) {
			installer.clearCache();
			installer.deleteTemp();
		}
		if (!isAdded() || binding == null) return;
		binding.layoutProgress.setVisibility(View.GONE);
		binding.layoutDiff.setVisibility(View.GONE);
		binding.layoutPrompt.setVisibility(View.GONE);
		binding.layoutSuccess.setVisibility(View.GONE);
		binding.layoutError.setVisibility(View.VISIBLE);
		binding.tvErrorDetails.setText(e != null && e.getLocalizedMessage() != null
				? e.getLocalizedMessage() : getString(R.string.error));
		binding.btnPrimary.setVisibility(View.GONE);
		binding.btnSecondary.setVisibility(View.GONE);
		binding.btnCancel.setText(R.string.dismiss);
		binding.btnCancel.setOnClickListener(v -> dismiss());
	}
}
