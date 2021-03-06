package com.mohammadag.colouredstatusbar;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import de.robv.android.xposed.XposedHelpers;

public class Utils {
	private static Object mUserHandle;
	private static Boolean mHasGeminiSupport = null;
	private static final int mPlayMusicOrangeColor = Color.parseColor("#f4842d");

	public static ActivityInfo[] getActivityList(Context context, String packageName) throws NameNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
		ActivityInfo[] list = info.activities;
		return list;
	}

	public static String removePackageName(String string, String packageName) {
		return string.replace(packageName + ".", "");
	}

	public static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}
		Bitmap bitmap;

		try {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap); 
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			throw e;
		}

		return bitmap;
	}

	public static int getMainColorFromActionBarDrawable(Drawable drawable) throws IllegalArgumentException {
		/* This should fix the bug where a huge part of the ActionBar background is drawn white. */
		Drawable copyDrawable = drawable.getConstantState().newDrawable();

		if (copyDrawable instanceof ColorDrawable) {
			return ((ColorDrawable) drawable).getColor();
		}

		Bitmap bitmap = drawableToBitmap(copyDrawable);
		int pixel = bitmap.getPixel(0, 5);
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		int alpha = Color.alpha(pixel);
		return Color.argb(alpha, red, green, blue);
	}

	// Thanks to GermainZ for the suggestion
	// http://forum.xda-developers.com/showpost.php?p=46102053&postcount=153
	public static int getIconColorForColor(int color, int defaultNormal, int defaultInverted, float hsvMaxValue) {
		/* Take away things people complain about :P */
		if (color == mPlayMusicOrangeColor)
			return defaultNormal;

		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		float value = hsv[2];
		if (value > hsvMaxValue) {
			return defaultInverted;
		} else {
			return defaultNormal;
		}
	}

	/* Helper method, on API 17 this method uses sendBroadcastAsUser to prevent
	 * system warnings in logcat.
	 * 
	 * Changed UserHandle to android.os.UserHandle, thanks to zst123 for the patch! :)
	 * https://github.com/zst123/Xposed-Tinted-Status-Bar/commit/fb57b46cf057f12acf78ce9918750dae301c508a
	 */
	@SuppressWarnings("unused")
	public static void sendOrderedBroadcast(Context context, Intent intent) {
		/* TODO: Find a way to check if the caller is a system app */
		final boolean disableBroadcastAsUser = true;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && !disableBroadcastAsUser) {
			if (mUserHandle == null) {
				try {
					mUserHandle = XposedHelpers.getStaticObjectField(android.os.UserHandle.class, "CURRENT");
				} catch (Throwable t) {
					context.sendOrderedBroadcast(intent, null);
					return;
				}
			}
			context.sendOrderedBroadcastAsUser(intent, (android.os.UserHandle) mUserHandle, null, null, null, 0, null, null);
		} else {
			context.sendOrderedBroadcast(intent, null);
		}
	}

	public static String convertToARGB(int color) {
		String alpha = Integer.toHexString(Color.alpha(color));
		String red = Integer.toHexString(Color.red(color));
		String green = Integer.toHexString(Color.green(color));
		String blue = Integer.toHexString(Color.blue(color));

		if (alpha.length() == 1) {
			alpha = "0" + alpha;
		}

		if (red.length() == 1) {
			red = "0" + red;
		}

		if (green.length() == 1) {
			green = "0" + green;
		}

		if (blue.length() == 1) {
			blue = "0" + blue;
		}

		return "#" + alpha + red + green + blue;
	}

	public static String addHashIfNeeded(String string) {
		if (string.startsWith("#"))
			return string;
		else
			return "#" + string;
	}

	public static String removeHashIfNeeded(String string) {
		return string.replace("#", "");
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public static final SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);
	}

	public static boolean isPackageInstalled(Context context, String targetPackage) {
		PackageManager pm = context.getPackageManager();
		try {
			pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}  
		return true;
	}

	public static boolean isDonateVersionInstalled(Context context) {
		return isPackageInstalled(context, Common.PACKAGE_NAME_DONATE);
	}

	public static boolean hasGeminiSupport() {
		if (mHasGeminiSupport != null) return mHasGeminiSupport;

		mHasGeminiSupport = getSystemPropBoolean("ro.mediatek.gemini_support", false);
		return mHasGeminiSupport;
	}

	public static String porterDuffModeToString(PorterDuff.Mode mode) {
		switch (mode) {
		case MULTIPLY:
			return "MULTIPLY";
		case SRC_ATOP:
			return "SRC_ATOP";
		case ADD:
			return "ADD";
		case CLEAR:
			return "CLEAR";
		case DARKEN:
			return "DARKEN";
		default:
			return "";
		}
	}

	public static PorterDuff.Mode stringToPorterDuffMode(String string) {
		if ("MULTIPLY".equals(string)) {
			return Mode.MULTIPLY;
		} else if ("SRC_ATOP".equals(string)) {
			return Mode.SRC_ATOP;
		}

		return null;
	}

	/* Also from GravityBox */
	public static Boolean getSystemPropBoolean(String key, boolean def) {
		Boolean ret = def;

		try {
			Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
			ret = (Boolean) callStaticMethod(classSystemProperties, "getBoolean", key, def);
		} catch (Throwable t) {
			ret = def;
		}
		return ret;
	}

	@SuppressWarnings("deprecation")
	public static void setImageViewBackground(ImageView view, Drawable drawable) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}
}
