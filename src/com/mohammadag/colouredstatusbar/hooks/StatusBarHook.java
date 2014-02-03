package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.lang.reflect.Method;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class StatusBarHook {
	private ColourChangerMod mInstance;

	public StatusBarHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;

		Class<?> PhoneStatusBar = findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
		try {
			findAndHookMethod(PhoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					LinearLayout mStatusIcons = (LinearLayout) getObjectField(param.thisObject, "mStatusIcons");
					mInstance.setStatusIcons(mStatusIcons);
					try {
						View mNavigationBarView = (View) getObjectField(param.thisObject, "mNavigationBarView");
						mNavigationBarView.setBackgroundColor(Color.TRANSPARENT);
						mInstance.setNavigationBarView(mNavigationBarView);
					} catch (NoSuchFieldError e) {}
				}
			});
		} catch (NoSuchMethodError e) {}

		try {
			Method swapViews = XposedHelpers.findMethodBestMatch(PhoneStatusBar, "swapViews", View.class, int.class,
					int.class, long.class);

			XperiaTransparencyHook xperiaHook = new XperiaTransparencyHook();
			XposedBridge.hookMethod(swapViews, xperiaHook);
		} catch (NoSuchMethodError e) {
			// Not an Xperia
		} catch (NoSuchFieldError e) {
			// Whaaaa?
		}

		try {
			findAndHookMethod(PhoneStatusBar, "transparentizeStatusBar", int.class, new TouchWizTransparencyHook(mInstance));
		} catch (NoSuchMethodError e) {
			// Not an S4
		}

		XC_MethodHook addRemoveIconHook = new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mInstance.setStatusIcons((LinearLayout) getObjectField(param.thisObject, "mStatusIcons"));

				if (param.method.getName().equals("addIcon"))
					mInstance.refreshStatusIconColors();
			};
		};

		Class<?> StatusBarIcon = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIcon", null);
		findAndHookMethod(PhoneStatusBar, "addIcon", String.class, int.class, int.class, StatusBarIcon, addRemoveIconHook);
		findAndHookMethod(PhoneStatusBar, "removeIcon", String.class, int.class, int.class, addRemoveIconHook);

		Class<?> StatusBarIconView = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", classLoader);
		XposedBridge.hookAllConstructors(StatusBarIconView, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ImageView view = (ImageView) param.thisObject;
				mInstance.addSystemIconView(view, true);
			}
		});
	}
}
