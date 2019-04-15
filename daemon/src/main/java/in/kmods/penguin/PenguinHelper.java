package in.kmods.penguin;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.Application;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.TAG;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.closeSilently;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticIntField;
import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

public class PenguinHelper {
    private static final String INSTALLER_PACKAGE_NAME = "in.kmods.penguin.manager";

    private static final String BASE_DIR = Build.VERSION.SDK_INT >= 24
            ? "/data/user_de/0/" + INSTALLER_PACKAGE_NAME + "/"
            : "/data/data/" + INSTALLER_PACKAGE_NAME + "/";

    private static final String INSTANT_RUN_CLASS = "com.android.tools.fd.runtime.BootstrapApplication";

    static void initForZygote() throws Exception {
        XC_MethodHook callback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                closeFilesBeforeForkNative();
            }

            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                reopenFilesAfterForkNative();
            }
        };

        Class<?> zygote = findClass("com.android.internal.os.Zygote", null);
        hookAllMethods(zygote, "nativeForkAndSpecialize", callback);
        hookAllMethods(zygote, "nativeForkSystemServer", callback);

        // when a package is loaded for an existing process, trigger the callbacks as well
        hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(param.thisObject != null) {
                    Application app = (Application) param.thisObject;
                    String packageName = app.getPackageName();
                    ApplicationInfo appInfo = app.getApplicationInfo();

                    Log.e(TAG, "Hooked into " + packageName);

                    if(packageName.equals(INSTALLER_PACKAGE_NAME)){
                        try {
                            Class<?> penguinApp = findClass(INSTALLER_PACKAGE_NAME + ".PenguinApp", app.getClassLoader());
                            setStaticObjectField(penguinApp, "version", "v1.0");
                        } catch (Exception ignored){}
                    }

                    XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam();
                    lpparam.packageName = packageName;
                    lpparam.processName = appInfo.processName;
                    lpparam.classLoader = app.getClassLoader();
                    lpparam.appInfo = appInfo;
                    lpparam.isFirstApplication = false;
                    XC_LoadPackage.callAll(lpparam);
                }
            }
        });
        /*hookAllMethods(ActivityThread.class, "createBaseContextForActivity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(param.thisObject != null) {
                    Object appContext = param.thisObject;
                    LoadedApk lddapk = (LoadedApk) getObjectField(appContext, "mPackageInfo");
                    if(lddapk != null) {
                        if(!getBooleanField(lddapk, "mIncludeCode")){
                            return;
                        }

                        String packageName = (String) getObjectField(lddapk, "mPackageName");
                        ApplicationInfo appInfo = (ApplicationInfo) getObjectField(lddapk, "mApplicationInfo");
                        ClassLoader appCl = (ClassLoader) getObjectField(lddapk, "mClassLoader");

                        Log.e(TAG, "Hooked into " + packageName);

                        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam();
                        lpparam.packageName = packageName;
                        lpparam.processName = appInfo.processName;
                        lpparam.classLoader = appCl;
                        lpparam.appInfo = appInfo;
                        lpparam.isFirstApplication = false;
                        XC_LoadPackage.callAll(lpparam);
                    }
                }
            }
        });*/

        // Samsung
        if (Build.VERSION.SDK_INT >= 24) {
            zygote = findClass("com.android.internal.os.Zygote", null);
            try {
                setStaticBooleanField(zygote, "isEnhancedZygoteASLREnabled", false);
            } catch (NoSuchFieldError ignored) {
            }
        }
    }

    static native void closeFilesBeforeForkNative();
    static native void reopenFilesAfterForkNative();
    static native String getFileData(String path);
    static native String getVersion();

    /**
     * Try to load all modules defined in <code>BASE_DIR/conf/modules.list</code>
     */
    static void loadModules() throws IOException {
        final String filename = BASE_DIR + "app_config/modules.list";
        String data = getFileData(filename);
        if(data == null || data.isEmpty()) {
            Log.i(TAG, "No ModulesList found!");
            return;
        }

        ClassLoader topClassLoader = XposedBridge.BOOTCLASSLOADER;
        ClassLoader parent;
        while ((parent = topClassLoader.getParent()) != null) {
            topClassLoader = parent;
        }

        InputStream is = new ByteArrayInputStream(data.getBytes());
        BufferedReader apks = new BufferedReader(new InputStreamReader(is));
        String apk;
        while ((apk = apks.readLine()) != null) {
            loadModule(apk, topClassLoader);
        }
        apks.close();
    }

    /**
     * Load a module from an APK by calling the init(String) method for all classes defined
     * in <code>assets/xposed_init</code>.
     */
    @SuppressLint("PrivateApi")
    private static void loadModule(String apk, ClassLoader topClassLoader) {
        Log.i(TAG, "Loading modules from " + apk);

        if (!new File(apk).exists()) {
            Log.e(TAG, "File does not exist");
            return;
        }

        DexFile dexFile;
        try {
            dexFile = new DexFile(apk);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load module", e);
            return;
        }

        if (dexFile.loadClass(INSTANT_RUN_CLASS, topClassLoader) != null) {
            Log.e(TAG, "Cannot load module, please disable \"Instant Run\" in Android Studio.");
            closeSilently(dexFile);
            return;
        }

        if (dexFile.loadClass(XposedBridge.class.getName(), topClassLoader) != null) {
            Log.e(TAG, "Cannot load module:");
            Log.e(TAG, "The Xposed API classes are compiled into the module's APK.");
            Log.e(TAG, "This may cause strange issues and must be fixed by the module developer.");
            Log.e(TAG, "For details, see: http://api.xposed.info/using.html");
            closeSilently(dexFile);
            return;
        }

        closeSilently(dexFile);

        ZipFile zipFile = null;
        InputStream is;
        try {
            zipFile = new ZipFile(apk);
            ZipEntry zipEntry = zipFile.getEntry("assets/xposed_init");
            if (zipEntry == null) {
                Log.e(TAG, "assets/xposed_init not found in the APK");
                closeSilently(zipFile);
                return;
            }
            is = zipFile.getInputStream(zipEntry);
        } catch (IOException e) {
            Log.e(TAG, "Cannot read assets/xposed_init in the APK", e);
            closeSilently(zipFile);
            return;
        }

        ClassLoader mcl = new PathClassLoader(apk, XposedBridge.BOOTCLASSLOADER);
        BufferedReader moduleClassesReader = new BufferedReader(new InputStreamReader(is));
        try {
            String moduleClassName;
            while ((moduleClassName = moduleClassesReader.readLine()) != null) {
                moduleClassName = moduleClassName.trim();
                if (moduleClassName.isEmpty() || moduleClassName.startsWith("#"))
                    continue;

                try {
                    Log.i(TAG, "Loading class " + moduleClassName);
                    Class<?> moduleClass = mcl.loadClass(moduleClassName);

                    if (IXposedHookLoadPackage.class.isAssignableFrom(moduleClass)) {
                        final Object moduleInstance = moduleClass.newInstance();
                        if (PenguinInit.isZygote) {
                            if(moduleInstance instanceof IXposedHookLoadPackage) {
                                XposedBridge.addLoadPackage(moduleInstance);
                            }
                        }
                    } else {
                        Log.e(TAG, "This class doesn't implement any sub-interface of IXposedHookLoadPackage, skipping it");
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to load class " + moduleClassName, t);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load module from " + apk, e);
        } finally {
            closeSilently(is);
            closeSilently(zipFile);
        }
    }
}
