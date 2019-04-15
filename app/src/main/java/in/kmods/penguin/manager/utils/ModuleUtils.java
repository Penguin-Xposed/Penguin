package in.kmods.penguin.manager.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.kmods.penguin.manager.PenguinApp;
import in.kmods.penguin.manager.adapters.ModuleListAdapter;
import in.kmods.penguin.manager.interfaces.PackageCallBack;
import in.kmods.penguin.manager.models.ModuleData;

public class ModuleUtils {
    private static HashMap<String, ModuleData> installedmodules;
    private static PackageCallBack callBack;

    static {
        installedmodules = new HashMap<>();
    }

    public static void AddCallBack(PackageCallBack packageCallBack){
        callBack = packageCallBack;
    }

    public static void updateInstalledModules(String pkg){
        PackageManager pm = PenguinApp.getApp().getPackageManager();
        List<PackageInfo> pilist = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        for(PackageInfo pi : pilist){
            if(!installedmodules.containsKey((pkg != null ? pkg : pi.packageName))) {
                try {
                    ModuleData moduleData = new ModuleData();
                    ApplicationInfo ai = pm.getApplicationInfo(pi.packageName, PackageManager.GET_META_DATA);
                    Bundle bundle = ai.metaData;
                    if (bundle != null && (bundle.containsKey("xposedmodule") || bundle.containsKey("penguinmodule"))) {
                        moduleData.appName = (String) pm.getApplicationLabel(ai);
                        moduleData.packageName = pi.packageName;
                        moduleData.versionName = pi.versionName;
                        moduleData.versionCode = pi.versionCode;
                        moduleData.sourceDir = ai.publicSourceDir;
                        moduleData.icon = pm.getApplicationIcon(ai);

                        moduleData.isForXposed = bundle.containsKey("xposedmodule");
                        moduleData.isForPenguin = bundle.containsKey("penguinmodule");
                        moduleData.isEnabled = false;

                        moduleData.penguinminVersion = 0;
                        moduleData.xposedminVersion = 0;
                        Object minversion;
                        if (bundle.containsKey("penguinminversion")) {
                            minversion = bundle.get("penguinminversion");
                            if (minversion != null) {
                                if (minversion instanceof Integer) {
                                    moduleData.penguinminVersion = (Integer) minversion;
                                } else if (minversion instanceof String) {
                                    moduleData.penguinminVersion = Integer.parseInt((String) minversion);
                                }
                            }
                        } else if (bundle.containsKey("xposedminversion")) {
                            minversion = bundle.get("xposedminversion");
                            if (minversion != null) {
                                if (minversion instanceof Integer) {
                                    moduleData.xposedminVersion = (Integer) minversion;
                                } else if (minversion instanceof String) {
                                    moduleData.xposedminVersion = Integer.parseInt((String) minversion);
                                }
                            }
                        }

                        moduleData.description = null;
                        Object moduledescription;
                        if (bundle.containsKey("penguindescription")) {
                            moduledescription = bundle.get("penguindescription");
                            if (moduledescription != null) {
                                moduleData.description = (String) moduledescription;
                            }
                        } else if (bundle.containsKey("xposeddescription")) {
                            moduledescription = bundle.get("xposeddescription");
                            if (moduledescription != null) {
                                moduleData.description = (String) moduledescription;
                            }
                        }

                        installedmodules.put(moduleData.packageName, moduleData);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static ModuleData getInstalledModuleData(String pkg){
        if(installedmodules.containsKey(pkg)){
            return installedmodules.get(pkg);
        }
        return null;
    }

    public static void updateModule(String pkg){
        installedmodules.remove(pkg);
        newModuleInstall(pkg);
    }

    public static void newModuleInstall(String pkg){
        updateInstalledModules(pkg);
        ModuleListAdapter currAdapter = getAdapter(false);
        currAdapter.add(getInstalledModuleData(pkg));
        currAdapter.UpdateModuleList();
        callBack.onListUpdate();
    }

    public static void removeSpecificModule(String pkg){
        installedmodules.remove(pkg);
        ModuleListAdapter currAdapter = getAdapter(false);
        currAdapter.remove(pkg);
        currAdapter.UpdateModuleList();
        callBack.onListUpdate();
    }

    public static int getListSize(){
        return installedmodules.size();
    }

    public static ModuleListAdapter getAdapter(boolean forcenew){
        return ModuleListAdapter.getInstance(new ArrayList<>(installedmodules.values()), forcenew);
    }
}
