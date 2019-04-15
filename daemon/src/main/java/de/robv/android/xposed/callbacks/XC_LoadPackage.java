package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;

public abstract class XC_LoadPackage extends XCallback {

	/**
	 * Wraps information about the app being loaded.
	 */
	public static final class LoadPackageParam {
		/** The name of the package being loaded. */
		public String packageName;

		/** The process in which the package is executed. */
		public String processName;

		/** The ClassLoader used for this package. */
		public ClassLoader classLoader;

		/** More information about the application being loaded. */
		public ApplicationInfo appInfo;

		/** Set to {@code true} if this is the first (and main) application for this process. */
		public boolean isFirstApplication;
	}

	/**
	 * Call all Callbacks when an app being load.
	 */
	public static void callAll(LoadPackageParam lpparam) throws Throwable {
		if (lpparam == null)
			throw new IllegalStateException("This object is null!");

		for(Object callback : XposedBridge.getLoadedCallbacks()){
			((IXposedHookLoadPackage) callback).handleLoadPackage(lpparam);
		}
	}
}
