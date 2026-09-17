package com.nsomatrix.neutron.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * Utility to apply Google Sans font to all items in Android Menu, ContextMenu, and PopupMenu objects.
 */
public class MenuFontHelper {
	public static void applyGoogleSans(Menu menu) {
		if (menu == null) return;
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			applyToMenuItem(item);
			if (item.hasSubMenu()) {
				SubMenu subMenu = item.getSubMenu();
				applyGoogleSans(subMenu);
			}
		}
	}

	public static void applyToMenuItem(MenuItem item) {
		if (item == null) return;
		CharSequence title = item.getTitle();
		if (title != null && title.length() > 0) {
			Typeface tf = FontCache.getGoogleSans(Typeface.NORMAL);
			SpannableString s = new SpannableString(title);
			s.setSpan(new CustomTypefaceSpan("google_sans", tf), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			item.setTitle(s);
		}
	}
}
