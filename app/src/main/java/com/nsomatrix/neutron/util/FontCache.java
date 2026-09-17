package com.nsomatrix.neutron.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.util.ContextHolder;

/**
 * High-performance thread-safe Typeface cache to eliminate duplicate font file loads.
 */
public class FontCache {
	private static final String TAG = "FontCache";
	private static final ConcurrentHashMap<String, Typeface> cache = new ConcurrentHashMap<>();

	/**
	 * Retrieves a Typeface from assets, caching it in memory.
	 */
	public static Typeface get(Context context, String fontPath) {
		if (context == null) {
			return Typeface.DEFAULT;
		}
		Typeface tf = cache.get(fontPath);
		if (tf == null) {
			try {
				tf = Typeface.createFromAsset(context.getAssets(), fontPath);
				if (tf != null) {
					cache.put(fontPath, tf);
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to load typeface from asset: " + fontPath, e);
				return Typeface.DEFAULT;
			}
		}
		return tf != null ? tf : Typeface.DEFAULT;
	}

	/**
	 * Retrieves Google Sans Typeface variant for the given style using application context.
	 */
	public static Typeface getGoogleSans(int style) {
		Context context = ContextHolder.getAppContext();
		if (context == null) {
			return Typeface.defaultFromStyle(style);
		}
		String fontPath;
		switch (style & Typeface.BOLD_ITALIC) {
			case Typeface.BOLD_ITALIC:
				fontPath = "GoogleSans-BoldItalic.ttf";
				break;
			case Typeface.BOLD:
				fontPath = "GoogleSans-Bold.ttf";
				break;
			case Typeface.ITALIC:
				fontPath = "GoogleSans-Italic.ttf";
				break;
			case Typeface.NORMAL:
			default:
				fontPath = "GoogleSans-Regular.ttf";
				break;
		}
		return get(context, fontPath);
	}
}
