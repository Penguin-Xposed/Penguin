package in.kmods.penguin.manager;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

import in.kmods.penguin.manager.receivers.PackageChangeReceiver;

public class PenguinApp extends Application {
    private static String version = "";
    private static int status = -1;

    private static PenguinApp app;
    private static PackageChangeReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        if(findLocation() > 0){
            status++;
            if(version != null && version.startsWith("v")){
                status++;
            }
        }

        receiver = new PackageChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(receiver);
        super.onTerminate();
    }

    private static long findLocation() {
        long location = -1;
        Scanner scan;
        try {
            scan = new Scanner(new File("/proc/self/maps"));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] parts = line.split(" ");
                if (parts[parts.length - 1].contains("libmemtrack_real.so") && parts[1].contains("x")) {
                    location = Long.parseLong(parts[0].substring(0, parts[0].indexOf("-")), 16);
                    break;
                }
            }
            scan.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return location;
    }

    public static String getVersion(){
        return version;
    }

    public static int getStatus(){
        return status;
    }

    public static PenguinApp getApp(){
        return app;
    }
}
