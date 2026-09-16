/*
 *  Copyright 2020-2022 Yury Kharchenko
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
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.DecimalFormat;
import java.util.Map;

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
	private static final String ARG_URI = "InstallerDialog.uri";
	private static final String ARG_ID = "InstallerDialog.id";
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	private AppRepository appRepository;
	private Button btnOk;
	private Button btnClose;
	private Button btnRun;
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
		setStyle(STYLE_NORMAL, R.style.EnterpriseBottomSheetDialogTheme);
		if (savedInstanceState != null) {
			dismissAllowingStateLoss();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = DialogInstallerBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		btnOk = binding.btnOk;
		btnClose = binding.btnClose;
		btnRun = binding.btnRun;
		showQueryingState();
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

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog instanceof BottomSheetDialog) {
			BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
			FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (bottomSheet != null) {
				bottomSheet.setBackgroundResource(android.R.color.transparent);
				BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
			}
		}
		if (installer != null) {
			return;
		}
		hideButtons();
		Bundle args = requireArguments();
		Uri uri = args.getParcelable(ARG_URI);
		if (uri != null) {
			installApp(null, uri);
			return;
		}
		int id = args.getInt(ARG_ID);
		reinstallApp(id);
	}

	private void showQueryingState() {
		if (binding == null) return;
		binding.tvAppName.setText(R.string.loading_info);
		binding.tvAppVendor.setText("");

		binding.progressVersion.setVisibility(View.VISIBLE);
		if (binding.progressVersion.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.progressVersion.getLayoutParams();
			params.setMarginEnd(0);
			binding.progressVersion.setLayoutParams(params);
		}
		binding.tvAppVersionBadge.setText("");
		binding.tvAppVersionBadge.setVisibility(View.GONE);
		binding.layoutVersionPill.setVisibility(View.VISIBLE);

		binding.progressDetailSize.setVisibility(View.VISIBLE);
		binding.tvDetailSize.setText("");

		binding.progressDetailProfile.setVisibility(View.VISIBLE);
		binding.tvDetailProfile.setText("");

		binding.installationStatus.setText(R.string.loading_info);
		showProgress();
		hideButtons();
	}

	private void hideSpinners() {
		if (binding == null) return;
		binding.progressVersion.setVisibility(View.GONE);
		binding.progressDetailSize.setVisibility(View.GONE);
		binding.progressDetailProfile.setVisibility(View.GONE);
	}

	private void installApp(String path, Uri uri) {
		showQueryingState();
		installer = new AppInstaller(path, uri, requireActivity().getApplication(), appRepository);
		btnClose.setOnClickListener(v -> {
			installer.deleteTemp();
			installer.clearCache();
			dismiss();
		});
		Disposable disposable = Single.create(installer::loadInfo)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void reinstallApp(int id) {
		showQueryingState();
		installer = new AppInstaller(id, requireActivity().getApplication(), appRepository);
		btnClose.setOnClickListener(v -> {
			installer.deleteTemp();
			installer.clearCache();
			dismiss();
		});
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
		showQueryingState();
		Disposable disposable = installer.updateInfo(uri)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void hideProgress() {
		binding.layoutProgress.setVisibility(View.GONE);
	}

	private void showProgress() {
		binding.layoutProgress.setVisibility(View.VISIBLE);
	}

	private void hideButtons() {
		btnOk.setVisibility(View.GONE);
		btnClose.setVisibility(View.GONE);
		btnRun.setVisibility(View.GONE);
	}

	private void showButtons() {
		btnOk.setVisibility(View.VISIBLE);
		btnClose.setVisibility(View.VISIBLE);
	}

	private void convert() {
		updateHeaderAndDetails();
		binding.installationStatus.setText(R.string.converting_wait);
		showProgress();
		hideButtons();
		Disposable disposable = Single.create(installer::install)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private String formatAppSize(long size) {
		if (size <= 0) return "N/A";
		DecimalFormat decimalFormat = new DecimalFormat("########.00");
		if (size >= 1024L) {
			float kb = (float) size / 1024F;
			if (kb >= 1024F) {
				float mb = kb / 1024F;
				return decimalFormat.format(mb) + " MB";
			}
			return decimalFormat.format(kb) + " KB";
		}
		return size + " B";
	}

	private void updateHeaderAndDetails() {
		if (installer == null || binding == null) return;
		hideSpinners();
		Descriptor nd = installer.getNewDescriptor();
		if (nd != null) {
			binding.tvAppName.setText(nd.getName());
			binding.tvAppVendor.setText(nd.getVendor() != null ? nd.getVendor() : getString(R.string.app_name));
			if (nd.getVersion() != null) {
				binding.tvAppVersionBadge.setText("v" + nd.getVersion());
				binding.layoutVersionPill.setVisibility(View.VISIBLE);
				binding.tvAppVersionBadge.setVisibility(View.VISIBLE);
			} else {
				binding.layoutVersionPill.setVisibility(View.GONE);
			}

			Map<String, String> attrs = nd.getAttrs();
			String profile = attrs.get("MicroEdition-Profile");
			if (profile == null) profile = attrs.get("MicroEdition-Configuration");
			if (profile == null) profile = "MIDP 2.0";
			binding.tvDetailProfile.setText(profile);

			String desc = attrs.get("MIDlet-Description");
			if (desc != null && !desc.trim().isEmpty()) {
				binding.tvDescription.setText(desc.trim());
				binding.tvDescription.setVisibility(View.VISIBLE);
			} else {
				binding.tvDescription.setVisibility(View.GONE);
			}
		}

		long size = installer.getJarSize();
		binding.tvDetailSize.setText(formatAppSize(size));

		Drawable drawable = Drawable.createFromPath(installer.getIconPath());
		if (drawable != null) {
			binding.ivAppIcon.setImageDrawable(drawable);
		} else {
			binding.ivAppIcon.setImageResource(R.mipmap.ic_launcher);
		}
	}

	private void showNotice(String text, int iconRes, int textColorRes, int bgDrawableRes) {
		binding.cardNotice.setVisibility(View.VISIBLE);
		binding.cardNotice.setBackgroundResource(bgDrawableRes);
		binding.ivNoticeIcon.setImageResource(iconRes);
		binding.tvNoticeText.setText(text);
		binding.tvNoticeText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), textColorRes));
	}

	private void onProgress(@NonNull Integer status) {
		if (!isAdded() || binding == null) {
			return;
		}
		hideSpinners();
		if (status == AppInstaller.STATUS_SUCCESS) {
			hideProgress();
			AppItem app = installer.getExistsApp();
			if (app != null) {
				binding.tvAppName.setText(app.getTitle());
				binding.tvAppVendor.setText(app.getAuthor());
				binding.tvAppVersionBadge.setText("Installed");
				binding.layoutVersionPill.setVisibility(View.VISIBLE);
				binding.tvAppVersionBadge.setVisibility(View.VISIBLE);
				Drawable drawable = Drawable.createFromPath(app.getImagePathExt());
				if (drawable != null) binding.ivAppIcon.setImageDrawable(drawable);
			}
			long size = installer.getJarSize();
			if (size > 0) {
				binding.tvDetailSize.setText(formatAppSize(size));
			}

			showNotice(getString(R.string.install_done), R.drawable.ic_installer_check, R.color.installer_info_text, R.drawable.bg_info_banner);

			btnOk.setText(R.string.START_CMD);
			btnOk.setOnClickListener(v -> {
				if (app != null) {
					Config.startApp(v.getContext(), app.getTitle(), app.getPathExt(), false);
				}
				dismiss();
			});
			btnClose.setText(R.string.close);
			showButtons();
			return;
		}

		updateHeaderAndDetails();

		Descriptor nd = installer.getNewDescriptor();
		switch (status) {
			case AppInstaller.STATUS_NEW:
				if (installer.getJar() != null) {
					convert();
					return;
				}
				if (installer.getJar() == null) {
					showNotice(getString(R.string.warn_install_from_net), R.drawable.ic_installer_warning, R.color.installer_warning_text, R.drawable.bg_warning_banner);
				} else {
					binding.cardNotice.setVisibility(View.GONE);
				}
				btnOk.setText(R.string.install);
				btnOk.setOnClickListener(v -> convert());
				break;

			case AppInstaller.STATUS_OLDEST:
				showNotice(getString(R.string.reinstall_older, nd.getVersion(), installer.getCurrentVersion()),
						R.drawable.ic_installer_warning, R.color.installer_warning_text, R.drawable.bg_warning_banner);
				btnOk.setText(R.string.install);
				btnOk.setOnClickListener(v -> convert());
				break;

			case AppInstaller.STATUS_EQUAL:
				showNotice(getString(R.string.reinstall), R.drawable.ic_installer_info, R.color.installer_info_text, R.drawable.bg_info_banner);
				AppItem app = installer.getExistsApp();
				btnOk.setText(R.string.action_reinstall);
				btnOk.setOnClickListener(v -> convert());
				btnRun.setVisibility(View.VISIBLE);
				btnRun.setText(R.string.START_CMD);
				btnRun.setOnClickListener(v -> {
					installer.clearCache();
					installer.deleteTemp();
					if (app != null) {
						Config.startApp(v.getContext(), app.getTitle(), app.getPathExt(), false);
					}
					dismiss();
				});
				break;

			case AppInstaller.STATUS_NEWEST:
				showNotice(getString(R.string.reinstall_newest, nd.getVersion(), installer.getCurrentVersion()),
						R.drawable.ic_installer_info, R.color.installer_info_text, R.drawable.bg_info_banner);
				btnOk.setText(R.string.install);
				btnOk.setOnClickListener(v -> convert());
				break;

			case AppInstaller.STATUS_UNMATCHED:
				SpannableStringBuilder info = installer.getManifest().getInfo(requireActivity());
				info.append(getString(R.string.install_jar_non_matched_jad));
				showNotice(info.toString(), R.drawable.ic_installer_warning, R.color.installer_warning_text, R.drawable.bg_warning_banner);
				btnOk.setText(R.string.install);
				btnOk.setOnClickListener(v -> installApp(installer.getJar(), null));
				break;

			case AppInstaller.STATUS_NEED_JAD:
				showNotice(getString(R.string.install_jar_needed), R.drawable.ic_installer_info, R.color.installer_info_text, R.drawable.bg_info_banner);
				btnOk.setText(R.string.choose);
				btnOk.setOnClickListener(v -> openFileLauncher.launch(null));
				break;

			default:
				throw new IllegalStateException("Unexpected value: " + status);
		}

		hideProgress();
		showButtons();
	}

	private void onError(Throwable e) {
		e.printStackTrace();
		installer.clearCache();
		installer.deleteTemp();
		if (!isAdded()) return;
		hideProgress();
		Toast.makeText(requireActivity(), getString(R.string.error) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		dismissAllowingStateLoss();
	}
}
