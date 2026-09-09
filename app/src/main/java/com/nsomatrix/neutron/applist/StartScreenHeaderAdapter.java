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
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.nsomatrix.neutron.R;
import com.nsomatrix.neutron.databinding.ItemStartScreenHeaderBinding;

public class StartScreenHeaderAdapter extends RecyclerView.Adapter<StartScreenHeaderAdapter.StartHeaderViewHolder> {

	public interface OnStartScreenActionListener {
		void onHeroPlay(AppItem item);
		void onQuickActionAdd();
		void onQuickActionStorage();
		void onQuickActionKeyMapper();
		void onQuickActionSettings();
	}

	private AppItem recentItem = null;
	private boolean visible = true;
	private final OnStartScreenActionListener listener;
	private final LruCache<String, Drawable> iconCache = new LruCache<>(20);

	public StartScreenHeaderAdapter(OnStartScreenActionListener listener) {
		this.listener = listener;
	}

	public void setVisible(boolean visible) {
		if (this.visible == visible) return;
		this.visible = visible;
		if (visible) {
			notifyItemInserted(0);
		} else {
			notifyItemRemoved(0);
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void setRecentItem(@Nullable AppItem item) {
		this.recentItem = item;
		if (visible) {
			notifyItemChanged(0);
		}
	}

	public AppItem getRecentItem() {
		return recentItem;
	}

	@Override
	public int getItemCount() {
		return visible ? 1 : 0;
	}

	@NonNull
	@Override
	public StartHeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemStartScreenHeaderBinding binding = ItemStartScreenHeaderBinding.inflate(
				LayoutInflater.from(parent.getContext()), parent, false);
		return new StartHeaderViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull StartHeaderViewHolder holder, int position) {
		holder.bind(recentItem);
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

	class StartHeaderViewHolder extends RecyclerView.ViewHolder {
		private final ItemStartScreenHeaderBinding binding;

		StartHeaderViewHolder(ItemStartScreenHeaderBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(AppItem item) {
			if (item != null) {
				binding.tvHeroBadge.setText(R.string.hero_continue_playing);
				binding.tvHeroTitle.setText(item.getTitle());
				binding.tvHeroSubtitle.setText(item.getAuthor());
				binding.btnHeroPlay.setText(R.string.play_now);
				binding.btnHeroPlay.setIconResource(R.drawable.ic_play_arrow);

				Drawable icon = loadIcon(item);
				if (icon != null) {
					binding.ivHeroIcon.setImageDrawable(icon);
				} else {
					binding.ivHeroIcon.setImageResource(R.mipmap.ic_launcher);
				}

				binding.btnHeroPlay.setOnClickListener(v -> {
					if (listener != null) listener.onHeroPlay(item);
				});

				binding.cardHero.setOnClickListener(v -> {
					if (listener != null) listener.onHeroPlay(item);
				});
			} else {
				binding.tvHeroBadge.setText(R.string.start_screen_welcome_badge);
				binding.tvHeroTitle.setText(R.string.start_screen_welcome_title);
				binding.tvHeroSubtitle.setText(R.string.start_screen_welcome_desc);
				binding.btnHeroPlay.setText(R.string.action_import_game);
				binding.btnHeroPlay.setIconResource(R.drawable.ic_add_white);
				binding.ivHeroIcon.setImageResource(R.mipmap.ic_launcher);

				binding.btnHeroPlay.setOnClickListener(v -> {
					if (listener != null) listener.onQuickActionAdd();
				});

				binding.cardHero.setOnClickListener(v -> {
					if (listener != null) listener.onQuickActionAdd();
				});
			}

			binding.cardActionAdd.setOnClickListener(v -> {
				if (listener != null) listener.onQuickActionAdd();
			});

			binding.cardActionStorage.setOnClickListener(v -> {
				if (listener != null) listener.onQuickActionStorage();
			});

			binding.cardActionKeymapper.setOnClickListener(v -> {
				if (listener != null) listener.onQuickActionKeyMapper();
			});

			binding.cardActionSettings.setOnClickListener(v -> {
				if (listener != null) listener.onQuickActionSettings();
			});
		}
	}
}
