package in.kmods.penguin;

import android.util.Log;

import com.android.internal.os.RuntimeInit;
import com.android.internal.os.ZygoteInit;

public class PenguinInit {
    private static final String TAG = "Penguin-Xposed";
    static boolean isZygote = true;

    public static void main(String[] args) {
        Log.i("Penguin", "PenguinInit.main Called");
        try {
            if(isZygote) {
                PenguinHelper.initForZygote();
            }
            PenguinHelper.loadModules();
        } catch (Exception t) {
            Log.e(TAG, "Errors during Penguin-Xposed initialization", t);
        }
        if(isZygote){
            ZygoteInit.main(args);
        } else {
            RuntimeInit.main(args);
        }
    }

    protected static final class NoZygote {
        protected static void main(String[] args) {
            isZygote = false;
            PenguinInit.main(args);
        }
    }
}
