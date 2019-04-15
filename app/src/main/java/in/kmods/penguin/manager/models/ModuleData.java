package in.kmods.penguin.manager.models;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class ModuleData implements Serializable {
    private static final long serialVersionUID = 1337L;

    public String appName;
    public String packageName;
    public String versionName;
    public int versionCode;
    public String sourceDir;
    public Drawable icon;

    public int xposedminVersion;
    public int penguinminVersion;
    public String description;
    public boolean isForXposed;
    public boolean isForPenguin;
    public boolean isEnabled;
}
