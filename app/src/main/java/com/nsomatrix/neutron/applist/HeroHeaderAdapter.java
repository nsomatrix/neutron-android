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
import com.nsomatrix.neutron.databinding.ItemHeroContinuePlayingBinding;

public class HeroHeaderAdapter extends RecyclerView.Adapter<HeroHeaderAdapter.HeroViewHolder> {

	public interface OnHeroClickListener {
		void onHeroPlay(AppItem item);
	}

	private AppItem heroItem = null;
	private final OnHeroClickListener listener;
	private final LruCache<String, Drawable> iconCache = new LruCache<>(20);

	public HeroHeaderAdapter(OnHeroClickListener listener) {
		this.listener = listener;
	}

	public void setHeroItem(@Nullable AppItem item) {
		if (this.heroItem == item) {
			return;
		}
		AppItem oldItem = this.heroItem;
		this.heroItem = item;
		if (oldItem == null && item != null) {
			notifyItemInserted(0);
		} else if (oldItem != null && item == null) {
			notifyItemRemoved(0);
		} else if (oldItem != null && item != null) {
			notifyItemChanged(0);
		}
	}

	public AppItem getHeroItem() {
		return heroItem;
	}

	@Override
	public int getItemCount() {
		return heroItem != null ? 1 : 0;
	}

	@NonNull
	@Override
	public HeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ItemHeroContinuePlayingBinding binding = ItemHeroContinuePlayingBinding.inflate(
				LayoutInflater.from(parent.getContext()), parent, false);
		return new HeroViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull HeroViewHolder holder, int position) {
		if (heroItem != null) {
			holder.bind(heroItem);
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

	class HeroViewHolder extends RecyclerView.ViewHolder {
		private final ItemHeroContinuePlayingBinding binding;

		HeroViewHolder(ItemHeroContinuePlayingBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(AppItem item) {
			if (item == null) return;
			binding.tvHeroTitle.setText(item.getTitle());
			binding.tvHeroAuthor.setText(item.getAuthor());

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
		}
	}
}
