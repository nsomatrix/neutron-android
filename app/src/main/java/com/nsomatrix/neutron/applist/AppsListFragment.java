/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2020 Nikita Shakarun
 * Copyright 2018-2022 Yury Kharchenko
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

import static com.nsomatrix.neutron.util.Constants.KEY_APP_URI;
import static com.nsomatrix.neutron.util.Constants.KEY_MIDLET_NAME;
import static com.nsomatrix.neutron.util.Constants.PREF_APP_SORT;
import static com.nsomatrix.neutron.util.Constants.PREF_FAVORITES;
import static com.nsomatrix.neutron.util.Constants.PREF_LAST_PATH;
import static com.nsomatrix.neutron.util.Constants.PREF_LIBRARY_VIEW_MODE;
import static com.nsomatrix.neutron.util.Constants.PREF_RECENTS;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.appsdb.AppRepository;
import com.nsomatrix.neutron.config.Config;
import com.nsomatrix.neutron.config.ConfigActivity;
import com.nsomatrix.neutron.config.ProfilesActivity;
import com.nsomatrix.neutron.databinding.FragmentAppsListBinding;
import com.nsomatrix.neutron.filepicker.FilteredFilePickerFragment;
import com.nsomatrix.neutron.info.AboutDialogFragment;
import com.nsomatrix.neutron.info.HelpDialogFragment;
import com.nsomatrix.neutron.installer.InstallerDialog;
import com.nsomatrix.neutron.settings.SettingsActivity;
import com.nsomatrix.neutron.util.AppUtils;
import com.nsomatrix.neutron.util.Constants;
import com.nsomatrix.neutron.util.FileUtils;
import com.nsomatrix.neutron.util.LogUtils;

public class AppsListFragment extends Fragment implements GameAdapter.OnGameActionListener,
		GameOptionsBottomSheet.GameOptionsListener {

	private static final String TAG = AppsListFragment.class.getSimpleName();

	private HeroHeaderAdapter heroAdapter;
	private GameAdapter gameAdapter;
	private ConcatAdapter concatAdapter;
	private AppItem currentHeroItem = null;
	private List<AppItem> currentMasterList = new ArrayList<>();
	private Uri appUri;
	private SharedPreferences preferences;
	private AppRepository appRepository;
	private Disposable searchViewDisposable;

	private FragmentAppsListBinding binding;

	private final ActivityResultLauncher<String> openFileLauncher = registerForActivityResult(
			FileUtils.getFilePicker(),
			this::onPickFileResult);

	public static AppsListFragment newInstance(Uri data) {
		AppsListFragment fragment = new AppsListFragment();
		Bundle args = new Bundle();
		args.putParcelable(KEY_APP_URI, data);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			appUri = args.getParcelable(KEY_APP_URI);
			args.remove(KEY_APP_URI);
		}
		preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
		AppListModel appListModel = new ViewModelProvider(requireActivity()).get(AppListModel.class);
		appRepository = appListModel.getAppRepository();
		appRepository.observeErrors(this, this::alertDbError);
		appRepository.observeApps(this, this::onDbUpdated);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentAppsListBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setupWindowInsets();
		setupToolbar();
		setupRecyclerView();
		setupFilterChips();
		setupEmptyStateAndFab();
	}

	private void setupWindowInsets() {
		ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
			Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
			v.setPadding(0, statusBarInsets.top, 0, 0);
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, (v, insets) -> {
			Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			int baseBottom = (int) (96 * getResources().getDisplayMetrics().density);
			v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), baseBottom + navBarInsets.bottom);
			return insets;
		});

		ViewCompat.setOnApplyWindowInsetsListener(binding.floatingActionButton, (v, insets) -> {
			Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			int baseMargin = (int) (20 * getResources().getDisplayMetrics().density);
			lp.bottomMargin = baseMargin + navBarInsets.bottom;
			lp.rightMargin = baseMargin + navBarInsets.right;
			v.setLayoutParams(lp);
			return insets;
		});
	}

	private void setupToolbar() {
		binding.toolbar.inflateMenu(R.menu.main);
		MenuItem viewModeItem = binding.toolbar.getMenu().findItem(R.id.action_view_mode);
		updateViewModeMenuIcon(viewModeItem);

		binding.toolbar.setOnMenuItemClickListener(this::handleToolbarMenuItemClick);

		// Setup Search
		MenuItem searchItem = binding.toolbar.getMenu().findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchItem.getActionView();
		if (searchView != null) {
			searchView.setQueryHint(getString(R.string.search));
			searchViewDisposable = Observable.create((ObservableOnSubscribe<String>) emitter ->
					searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
						@Override
						public boolean onQueryTextSubmit(String query) {
							emitter.onNext(query);
							return true;
						}

						@Override
						public boolean onQueryTextChange(String newText) {
							emitter.onNext(newText);
							return true;
						}
					})).debounce(200, TimeUnit.MILLISECONDS)
					.distinctUntilChanged()
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(query -> {
						if (gameAdapter != null) {
							gameAdapter.setSearchQuery(query);
							updateHeroVisibility();
							updateEmptyStateVisibility();
						}
					});
		}
	}

	private boolean handleToolbarMenuItemClick(MenuItem item) {
		FragmentActivity activity = requireActivity();
		int itemId = item.getItemId();
		if (itemId == R.id.action_view_mode) {
			toggleViewMode();
			updateViewModeMenuIcon(item);
			return true;
		} else if (itemId == R.id.action_sort) {
			showSortDialog();
			return true;
		} else if (itemId == R.id.action_about) {
			new AboutDialogFragment().show(getChildFragmentManager(), "about");
			return true;
		} else if (itemId == R.id.action_profiles) {
			startActivity(new Intent(activity, ProfilesActivity.class));
			return true;
		} else if (itemId == R.id.action_settings) {
			startActivity(new Intent(activity, SettingsActivity.class));
			return true;
		} else if (itemId == R.id.action_help) {
			new HelpDialogFragment().show(getChildFragmentManager(), "help");
			return true;
		} else if (itemId == R.id.action_save_log) {
			try {
				LogUtils.writeLog();
				Toast.makeText(activity, R.string.log_saved, Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
			}
			return true;
		} else if (itemId == R.id.action_exit_app) {
			activity.finish();
			return true;
		}
		return false;
	}

	private void setupRecyclerView() {
		heroAdapter = new HeroHeaderAdapter(this::onGameClick);
		gameAdapter = new GameAdapter(this);

		int savedViewMode = preferences.getInt(PREF_LIBRARY_VIEW_MODE, GameAdapter.MODE_GRID);
		gameAdapter.setViewMode(savedViewMode);
		gameAdapter.setFavorites(getFavorites());
		gameAdapter.setRecentPaths(getRecents());

		concatAdapter = new ConcatAdapter(heroAdapter, gameAdapter);

		int gridColumns = getResources().getInteger(R.integer.grid_columns);
		GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), gridColumns);
		layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				if (heroAdapter.getItemCount() > 0 && position == 0) {
					return gridColumns;
				}
				if (gameAdapter.getViewMode() == GameAdapter.MODE_LIST) {
					return gridColumns;
				}
				return 1;
			}
		});
		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(concatAdapter);

		// Shrink / Extend FAB on scroll
		binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				if (dy > 8 && binding.floatingActionButton.isExtended()) {
					binding.floatingActionButton.shrink();
				} else if (dy < -8 && !binding.floatingActionButton.isExtended()) {
					binding.floatingActionButton.extend();
				}
			}
		});

		if (!currentMasterList.isEmpty()) {
			gameAdapter.setMasterList(currentMasterList);
			updateHeroItem();
			updateEmptyStateVisibility();
		}
	}

	private void setupFilterChips() {
		binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
			if (checkedIds.isEmpty()) return;
			int checkedId = checkedIds.get(0);
			if (checkedId == R.id.chip_all) {
				gameAdapter.setActiveFilter(GameAdapter.FILTER_ALL);
			} else if (checkedId == R.id.chip_favorites) {
				gameAdapter.setActiveFilter(GameAdapter.FILTER_FAVORITES);
			} else if (checkedId == R.id.chip_recent) {
				gameAdapter.setActiveFilter(GameAdapter.FILTER_RECENT);
			}
			updateHeroVisibility();
			updateEmptyStateVisibility();
		});
	}

	private void updateHeroItem() {
		currentHeroItem = null;
		List<String> recents = getRecents();
		if (!recents.isEmpty() && !currentMasterList.isEmpty()) {
			String lastPath = recents.get(0);
			for (AppItem item : currentMasterList) {
				if (item.getPath().equals(lastPath)) {
					currentHeroItem = item;
					break;
				}
			}
		}
		updateHeroVisibility();
	}

	private void updateHeroVisibility() {
		if (heroAdapter == null) return;
		boolean shouldShow = currentHeroItem != null
				&& (gameAdapter == null || gameAdapter.getActiveFilter() == GameAdapter.FILTER_ALL)
				&& (gameAdapter == null || TextUtils.isEmpty(gameAdapter.getSearchQuery()));
		heroAdapter.setHeroItem(shouldShow ? currentHeroItem : null);
	}

	private void setupEmptyStateAndFab() {
		binding.floatingActionButton.setOnClickListener(v -> launchFilePicker());
		binding.btnEmptyImport.setOnClickListener(v -> launchFilePicker());
		binding.btnEmptyFolder.setOnClickListener(v -> {
			startActivity(new Intent(requireActivity(), SettingsActivity.class));
		});
	}

	private void launchFilePicker() {
		String path = preferences.getString(PREF_LAST_PATH, null);
		if (path == null) {
			File dir = Environment.getExternalStorageDirectory();
			if (dir.canRead()) {
				path = dir.getAbsolutePath();
			}
		}
		try {
			openFileLauncher.launch(path);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getContext(), R.string.error_no_picker, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void toggleViewMode() {
		if (gameAdapter == null) return;
		int currentMode = gameAdapter.getViewMode();
		int nextMode = (currentMode == GameAdapter.MODE_GRID) ? GameAdapter.MODE_LIST : GameAdapter.MODE_GRID;
		gameAdapter.setViewMode(nextMode);
		preferences.edit().putInt(PREF_LIBRARY_VIEW_MODE, nextMode).apply();
	}

	private void updateViewModeMenuIcon(MenuItem item) {
		if (item == null || gameAdapter == null) return;
		if (gameAdapter.getViewMode() == GameAdapter.MODE_GRID) {
			item.setIcon(R.drawable.ic_list_view);
		} else {
			item.setIcon(R.drawable.ic_grid_view);
		}
	}

	private Set<String> getFavorites() {
		return new HashSet<>(preferences.getStringSet(PREF_FAVORITES, Collections.emptySet()));
	}

	private List<String> getRecents() {
		String recentsStr = preferences.getString(PREF_RECENTS, "");
		if (TextUtils.isEmpty(recentsStr)) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(recentsStr.split(",")));
	}

	private void recordRecent(AppItem item) {
		if (item == null) return;
		List<String> recents = getRecents();
		recents.remove(item.getPath());
		recents.add(0, item.getPath());
		if (recents.size() > 10) {
			recents = recents.subList(0, 10);
		}
		preferences.edit().putString(PREF_RECENTS, TextUtils.join(",", recents)).apply();
		if (gameAdapter != null) {
			gameAdapter.setRecentPaths(recents);
			updateHeroItem();
		}
	}

	private void updateEmptyStateVisibility() {
		if (binding == null || gameAdapter == null) return;
		boolean isEmpty = gameAdapter.getItemCount() == 0 && (heroAdapter == null || heroAdapter.getItemCount() == 0);
		binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
		binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

		if (isEmpty) {
			if (gameAdapter.getMasterList().isEmpty()) {
				binding.tvEmptyTitle.setText(R.string.empty_library_title);
				binding.tvEmptyDesc.setText(R.string.empty_library_desc);
				binding.btnEmptyImport.setVisibility(View.VISIBLE);
				binding.btnEmptyFolder.setVisibility(View.VISIBLE);
			} else if (!TextUtils.isEmpty(gameAdapter.getSearchQuery())) {
				binding.tvEmptyTitle.setText(R.string.empty_search_title);
				binding.tvEmptyDesc.setText(R.string.empty_search_desc);
				binding.btnEmptyImport.setVisibility(View.GONE);
				binding.btnEmptyFolder.setVisibility(View.GONE);
			} else if (gameAdapter.getActiveFilter() == GameAdapter.FILTER_FAVORITES) {
				binding.tvEmptyTitle.setText(R.string.empty_favorites_title);
				binding.tvEmptyDesc.setText(R.string.empty_favorites_desc);
				binding.btnEmptyImport.setVisibility(View.GONE);
				binding.btnEmptyFolder.setVisibility(View.GONE);
			} else if (gameAdapter.getActiveFilter() == GameAdapter.FILTER_RECENT) {
				binding.tvEmptyTitle.setText(R.string.empty_recent_title);
				binding.tvEmptyDesc.setText(R.string.empty_recent_desc);
				binding.btnEmptyImport.setVisibility(View.GONE);
				binding.btnEmptyFolder.setVisibility(View.GONE);
			}
		}
	}

	// GameAdapter.OnGameActionListener Callbacks
	@Override
	public void onGameClick(AppItem item) {
		recordRecent(item);
		Config.startApp(requireActivity(), item.getTitle(), item.getPathExt(), false);
	}

	@Override
	public void onGameMoreClick(AppItem item, View anchor) {
		boolean isFavorite = gameAdapter.isFavorite(item);
		GameOptionsBottomSheet bottomSheet = GameOptionsBottomSheet.newInstance(item, isFavorite);
		bottomSheet.setListener(this);
		bottomSheet.show(getParentFragmentManager(), "game_options");
	}

	@Override
	public void onGameFavoriteToggle(AppItem item) {
		onToggleFavorite(item);
	}

	// GameOptionsBottomSheet.GameOptionsListener Callbacks
	@Override
	public void onPlay(AppItem item) {
		onGameClick(item);
	}

	@Override
	public void onToggleFavorite(AppItem item) {
		if (item == null) return;
		Set<String> favorites = getFavorites();
		boolean willBeFavorite = !favorites.contains(item.getPath());
		if (willBeFavorite) {
			favorites.add(item.getPath());
		} else {
			favorites.remove(item.getPath());
		}
		preferences.edit().putStringSet(PREF_FAVORITES, favorites).apply();
		if (gameAdapter != null) {
			gameAdapter.notifyFavoriteToggled(item, willBeFavorite);
			updateEmptyStateVisibility();
		}
	}

	@Override
	public void onOpenSettings(AppItem item) {
		Config.startApp(requireActivity(), item.getTitle(), item.getPathExt(), true);
	}

	@Override
	public void onAddShortcut(AppItem item) {
		requestAddShortcut(item);
	}

	@Override
	public void onRename(AppItem item) {
		alertRename(item);
	}

	@Override
	public void onReinstall(AppItem item) {
		InstallerDialog.newInstance(item.getId()).show(getParentFragmentManager(), "installer");
	}

	@Override
	public void onDelete(AppItem item) {
		alertDelete(item);
	}

	private void alertRename(AppItem item) {
		FragmentActivity activity = requireActivity();
		EditText editText = new EditText(activity);
		editText.setText(item.getTitle());
		float density = getResources().getDisplayMetrics().density;
		LinearLayout linearLayout = new LinearLayout(activity);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) (density * 20);
		params.setMargins(margin, 0, margin, 0);
		linearLayout.addView(editText, params);
		int paddingVertical = (int) (density * 16);
		int paddingHorizontal = (int) (density * 8);
		editText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

		new AlertDialog.Builder(activity)
				.setTitle(R.string.action_context_rename)
				.setView(linearLayout)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					String title = editText.getText().toString().trim();
					if (title.isEmpty()) {
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						item.setTitle(title);
						appRepository.update(item);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void alertDelete(AppItem item) {
		new AlertDialog.Builder(requireActivity())
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_delete)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					AppUtils.deleteApp(item);
					appRepository.delete(item);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void requestAddShortcut(AppItem appItem) {
		FragmentActivity activity = requireActivity();
		Bitmap bitmap = AppUtils.getIconBitmap(appItem);
		IconCompat icon;
		if (bitmap == null) {
			icon = IconCompat.createWithResource(activity, R.mipmap.ic_launcher);
		} else {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
			int iconSize = am.getLauncherLargeIconSize();
			Rect src;
			if (width > height) {
				int left = (width - height) / 2;
				src = new Rect(left, 0, left + height, height);
			} else if (width < height) {
				int top = (height - width) / 2;
				src = new Rect(0, top, width, top + width);
			} else {
				src = null;
			}
			Bitmap scaled = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(scaled);
			canvas.drawBitmap(bitmap, src, new RectF(0, 0, iconSize, iconSize), null);
			icon = IconCompat.createWithBitmap(scaled);
		}
		String title = appItem.getTitle();
		Intent launchIntent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPathExt()),
				activity, ConfigActivity.class);
		launchIntent.putExtra(KEY_MIDLET_NAME, title);
		ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(activity, title)
				.setIntent(launchIntent)
				.setShortLabel(title)
				.setIcon(icon)
				.build();
		ShortcutManagerCompat.requestPinShortcut(activity, shortcut, null);
	}

	private void showSortDialog() {
		int variant = appRepository.getSort();
		SortAdapter sortAdapter = new SortAdapter(requireActivity(), variant);
		new AlertDialog.Builder(requireActivity())
				.setTitle(R.string.pref_app_sort_title)
				.setAdapter(sortAdapter, (d, v) -> {
					sortAdapter.setVariant(v);
					setSort(v);
					d.dismiss();
				})
				.show();
	}

	private void setSort(int sortVariant) {
		if (appRepository.getSort() == sortVariant) {
			sortVariant |= 0x80000000;
		}
		preferences.edit().putInt(PREF_APP_SORT, sortVariant).apply();
	}

	private void alertDbError(Throwable throwable) {
		Activity activity = getActivity();
		if (activity == null) {
			Log.e(TAG, "Db error detected", throwable);
			return;
		}
		if (throwable instanceof SQLiteDiskIOException) {
			Toast.makeText(activity, R.string.error_disk_io, Toast.LENGTH_SHORT).show();
		} else {
			String msg = activity.getString(R.string.error) + ": " + throwable.getMessage();
			Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
		}
	}

	private void onPickFileResult(Uri uri) {
		if (uri == null) {
			return;
		}
		preferences.edit()
				.putString(Constants.PREF_LAST_PATH, FilteredFilePickerFragment.getLastPath())
				.apply();
		InstallerDialog.newInstance(uri).show(getParentFragmentManager(), "installer");
	}

	private void onDbUpdated(List<AppItem> items) {
		currentMasterList = items != null ? new ArrayList<>(items) : new ArrayList<>();
		if (gameAdapter != null) {
			gameAdapter.setMasterList(currentMasterList);
			updateHeroItem();
			updateEmptyStateVisibility();
		}
		if (appUri != null) {
			InstallerDialog.newInstance(appUri).show(getParentFragmentManager(), "installer");
			appUri = null;
		}
	}

	private static class SortAdapter extends ArrayAdapter<String> {
		private int variant;
		private final Drawable drawableArrowDown;
		private final Drawable drawableArrowUp;

		public SortAdapter(FragmentActivity activity, int variant) {
			super(activity,
					android.R.layout.simple_list_item_1,
					activity.getResources().getStringArray(R.array.pref_app_sort_entries));
			this.variant = variant;
			drawableArrowDown = AppCompatResources.getDrawable(activity, R.drawable.ic_arrow_down);
			drawableArrowUp = AppCompatResources.getDrawable(activity, R.drawable.ic_arrow_up);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			TextView tv = (TextView) super.getView(position, convertView, parent);
			if ((variant & 0x7FFFFFFF) == position) {
				TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(tv, null, null,
						variant >= 0 ? drawableArrowDown : drawableArrowUp, null);
			} else {
				tv.setCompoundDrawables(null, null, null, null);
			}
			return tv;
		}

		public void setVariant(int variant) {
			if (variant == this.variant) {
				variant |= 0x80000000;
			}
			this.variant = variant;
			notifyDataSetChanged();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onDestroy() {
		if (searchViewDisposable != null) {
			searchViewDisposable.dispose();
		}
		super.onDestroy();
	}
}
