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

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.ItemGameCardGridBinding;
import com.nsomatrix.neutron.databinding.ItemGameCardListBinding;

public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int MODE_GRID = 0;
	public static final int MODE_LIST = 1;

	public static final int FILTER_ALL = 0;
	public static final int FILTER_FAVORITES = 1;
	public static final int FILTER_RECENT = 2;
	public static final int FILTER_3D = 3;

	private static final int TYPE_GRID = 0;
	private static final int TYPE_LIST = 1;

	public interface OnGameActionListener {
		void onGameClick(AppItem item);
		void onGameMoreClick(AppItem item, View anchor);
		void onGameFavoriteToggle(AppItem item);
	}

	private int viewMode = MODE_GRID;
	private int activeFilter = FILTER_ALL;
	private String searchQuery = "";

	private List<AppItem> masterList = new ArrayList<>();
	private final List<AppItem> displayedList = new ArrayList<>();

	private final Set<String> favoritePaths = new HashSet<>();
	private final List<String> recentPaths = new ArrayList<>();
	private final Map<String, Boolean> is3dCache = new HashMap<>();

	private final LruCache<String, Drawable> iconCache = new LruCache<>(80);
	private final OnGameActionListener listener;

	public GameAdapter(OnGameActionListener listener) {
		this.listener = listener;
	}

	public void setFavorites(Set<String> favorites) {
		favoritePaths.clear();
		if (favorites != null) {
			favoritePaths.addAll(favorites);
		}
		if (activeFilter == FILTER_FAVORITES) {
			applyFilters(false);
		} else {
			notifyDataSetChanged();
		}
	}

	public void notifyFavoriteToggled(AppItem item, boolean isFavorite) {
		if (item == null) return;
		if (isFavorite) {
			favoritePaths.add(item.getPath());
		} else {
			favoritePaths.remove(item.getPath());
		}
		int pos = -1;
		for (int i = 0; i < displayedList.size(); i++) {
			if (displayedList.get(i).getId() == item.getId()) {
				pos = i;
				break;
			}
		}
		if (pos != -1) {
			if (activeFilter == FILTER_FAVORITES && !isFavorite) {
				displayedList.remove(pos);
				notifyItemRemoved(pos);
			} else {
				notifyItemChanged(pos);
			}
		}
	}

	public void setRecentPaths(List<String> recents) {
		recentPaths.clear();
		if (recents != null) {
			recentPaths.addAll(recents);
		}
		if (activeFilter == FILTER_RECENT) {
			applyFilters(false);
		}
	}

	public void setMasterList(List<AppItem> items) {
		this.masterList = items != null ? new ArrayList<>(items) : new ArrayList<>();
		applyFilters(false);
	}

	public List<AppItem> getMasterList() {
		return masterList;
	}

	public void setViewMode(int mode) {
		if (this.viewMode != mode) {
			this.viewMode = mode;
			notifyDataSetChanged();
		}
	}

	public int getViewMode() {
		return viewMode;
	}

	public void setActiveFilter(int filter) {
		if (this.activeFilter != filter) {
			this.activeFilter = filter;
			applyFilters(false);
		}
	}

	public int getActiveFilter() {
		return activeFilter;
	}

	public void setSearchQuery(String query) {
		String newQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
		if (!this.searchQuery.equals(newQuery)) {
			this.searchQuery = newQuery;
			applyFilters(true);
		}
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public boolean isFavorite(AppItem item) {
		return item != null && favoritePaths.contains(item.getPath());
	}

	private void applyFilters(boolean useDiff) {
		List<AppItem> filtered = new ArrayList<>();
		for (AppItem item : masterList) {
			if (activeFilter == FILTER_FAVORITES && !favoritePaths.contains(item.getPath())) {
				continue;
			}
			if (activeFilter == FILTER_RECENT && !recentPaths.contains(item.getPath())) {
				continue;
			}
			if (activeFilter == FILTER_3D && !checkIs3D(item)) {
				continue;
			}

			if (!TextUtils.isEmpty(searchQuery)) {
				String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.ROOT) : "";
				String author = item.getAuthor() != null ? item.getAuthor().toLowerCase(Locale.ROOT) : "";
				if (!title.contains(searchQuery) && !author.contains(searchQuery)) {
					continue;
				}
			}

			filtered.add(item);
		}

		if (!useDiff) {
			displayedList.clear();
			displayedList.addAll(filtered);
			notifyDataSetChanged();
			return;
		}

		final List<AppItem> oldList = new ArrayList<>(displayedList);
		final List<AppItem> newList = filtered;

		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
			@Override
			public int getOldListSize() {
				return oldList.size();
			}

			@Override
			public int getNewListSize() {
				return newList.size();
			}

			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
				return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
			}

			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
				AppItem o = oldList.get(oldItemPosition);
				AppItem n = newList.get(newItemPosition);
				return TextUtils.equals(o.getTitle(), n.getTitle()) &&
						TextUtils.equals(o.getAuthor(), n.getAuthor()) &&
						TextUtils.equals(o.getVersion(), n.getVersion()) &&
						favoritePaths.contains(o.getPath()) == favoritePaths.contains(n.getPath());
			}
		});

		displayedList.clear();
		displayedList.addAll(newList);
		try {
			diffResult.dispatchUpdatesTo(this);
		} catch (Exception e) {
			notifyDataSetChanged();
		}
	}

	public AppItem getItemAt(int position) {
		if (position >= 0 && position < displayedList.size()) {
			return displayedList.get(position);
		}
		return null;
	}

	@Override
	public int getItemCount() {
		return displayedList.size();
	}

	@Override
	public int getItemViewType(int position) {
		return viewMode == MODE_GRID ? TYPE_GRID : TYPE_LIST;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		if (viewType == TYPE_GRID) {
			ItemGameCardGridBinding binding = ItemGameCardGridBinding.inflate(inflater, parent, false);
			return new GridViewHolder(binding);
		} else {
			ItemGameCardListBinding binding = ItemGameCardListBinding.inflate(inflater, parent, false);
			return new ListViewHolder(binding);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		AppItem item = getItemAt(position);
		if (item == null) return;
		if (holder instanceof GridViewHolder) {
			((GridViewHolder) holder).bind(item);
		} else if (holder instanceof ListViewHolder) {
			((ListViewHolder) holder).bind(item);
		}
	}

	private Drawable loadIcon(AppItem item) {
		String iconPath = item.getImagePathExt();
		if (iconPath == null) return null;
		Drawable cached = iconCache.get(iconPath);
		if (cached != null) return cached;

		Drawable drawable = Drawable.createFromPath(iconPath);
		if (drawable != null) {
			drawable.setFilterBitmap(false);
			iconCache.put(iconPath, drawable);
		}
		return drawable;
	}

	private boolean checkIs3D(AppItem item) {
		if (item == null) return false;
		Boolean cached = is3dCache.get(item.getPath());
		if (cached != null) return cached;

		boolean is3d = false;
		try {
			File appDir = new File(item.getPathExt());
			File resDir = new File(appDir, "res");
			if (resDir.isDirectory()) {
				String[] list = resDir.list();
				if (list != null) {
					for (String f : list) {
						String lower = f.toLowerCase(Locale.ROOT);
						if (lower.endsWith(".m3g") || lower.endsWith(".mbac") || lower.endsWith(".tra")) {
							is3d = true;
							break;
						}
					}
				}
			}
		} catch (Exception ignored) {}

		is3dCache.put(item.getPath(), is3d);
		return is3d;
	}

	// Grid Card View Holder
	class GridViewHolder extends RecyclerView.ViewHolder {
		private final ItemGameCardGridBinding binding;

		GridViewHolder(ItemGameCardGridBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(AppItem item) {
			if (item == null) return;
			binding.tvTitle.setText(item.getTitle());
			binding.tvAuthor.setText(item.getAuthor());

			Drawable icon = loadIcon(item);
			if (icon != null) {
				binding.ivIcon.setImageDrawable(icon);
			} else {
				binding.ivIcon.setImageResource(R.mipmap.ic_launcher);
			}

			// Favorite Star
			boolean favorite = isFavorite(item);
			binding.ivFavorite.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

			binding.ivFavorite.setOnClickListener(v -> {
				if (listener != null) listener.onGameFavoriteToggle(item);
			});

			binding.btnOverflow.setOnClickListener(v -> {
				if (listener != null) listener.onGameMoreClick(item, v);
			});

			binding.cardGame.setOnClickListener(v -> {
				if (listener != null) listener.onGameClick(item);
			});

			binding.cardGame.setOnLongClickListener(v -> {
				if (listener != null) listener.onGameMoreClick(item, v);
				return true;
			});
		}
	}

	// List Card View Holder
	class ListViewHolder extends RecyclerView.ViewHolder {
		private final ItemGameCardListBinding binding;

		ListViewHolder(ItemGameCardListBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(AppItem item) {
			if (item == null) return;
			binding.tvTitle.setText(item.getTitle());
			binding.tvAuthor.setText(item.getAuthor());
			binding.tvVersion.setText("v" + item.getVersion());

			Drawable icon = loadIcon(item);
			if (icon != null) {
				binding.ivIcon.setImageDrawable(icon);
			} else {
				binding.ivIcon.setImageResource(R.mipmap.ic_launcher);
			}

			// Favorite Star
			boolean favorite = isFavorite(item);
			binding.ivFavorite.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

			binding.ivFavorite.setOnClickListener(v -> {
				if (listener != null) listener.onGameFavoriteToggle(item);
			});

			binding.btnOverflow.setOnClickListener(v -> {
				if (listener != null) listener.onGameMoreClick(item, v);
			});

			binding.cardGame.setOnClickListener(v -> {
				if (listener != null) listener.onGameClick(item);
			});

			binding.cardGame.setOnLongClickListener(v -> {
				if (listener != null) listener.onGameMoreClick(item, v);
				return true;
			});
		}
	}
}
