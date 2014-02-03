package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;

import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class SignalClusterHook {
	private ColourChangerMod mInstance;
	public SignalClusterHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		doHooks(classLoader);
	}

	private void doHooks(ClassLoader classLoader) {
		try { 
			Class<?> SignalClusterView = XposedHelpers.findClass("com.android.systemui.statusbar.SignalClusterViewGemini", classLoader);
			findAndHookMethod(SignalClusterView, "onAttachedToWindow", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try { 
					XposedBridge.log("TSB: found SimIndicator");
					findImageViews((ViewGroup)param.thisObject);
				}catch (Throwable t){
					XposedBridge.log("TSB: Not a view group");
				} 
			} 
		});
		
		} catch (Throwable e) {
			XposedBridge.log("TSB: cannot find SimIndicator");
		}
	}
	
	private void findImageViews(ViewGroup statusIcons) {
		for (int i = 0; i < statusIcons.getChildCount(); i++) {
			View view = statusIcons.getChildAt(i);
			if (view == null) {
				continue;
			} else if (view instanceof ImageView) {
				mInstance.addSystemIconView((ImageView) view);
			} else if (view instanceof ViewGroup){
				findImageViews((ViewGroup) view);
			}
		}
	}
}
