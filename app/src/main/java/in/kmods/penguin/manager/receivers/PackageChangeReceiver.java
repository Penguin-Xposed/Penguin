package in.kmods.penguin.manager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import in.kmods.penguin.manager.utils.ModuleUtils;

public class PackageChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || context == null)
            return;

        if(intent.getAction() == null)
            return;

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)
                && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            // Ignore existing packages being removed in order to be updated
            return;
        }

        String packageName = getPackageName(intent);
        if (packageName == null)
            return;

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
            // make sure that the change is for the complete package, not only a
            // component
            String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
            if (components != null) {
                boolean isForPackage = false;
                for (String component : components) {
                    if (packageName.equals(component)) {
                        isForPackage = true;
                        break;
                    }
                }
                if (!isForPackage)
                    return;
            }
        }

        switch (intent.getAction()) {
            case Intent.ACTION_PACKAGE_REMOVED:
                ModuleUtils.removeSpecificModule(packageName);
                break;
            case Intent.ACTION_PACKAGE_ADDED:
                ModuleUtils.newModuleInstall(packageName);
                break;
            default:
                ModuleUtils.updateModule(packageName);
                break;
        }
    }

    private static String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        return (uri != null) ? uri.getSchemeSpecificPart() : null;
    }
}
