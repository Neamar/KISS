package fr.neamar.kiss;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

public class KissApplication {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private static DataHandler dataHandler;
    private static CameraHandler cameraHandler;
    private static RootHandler rootHandler;
    private static IconsHandler iconsPackHandler;

    private KissApplication() {
    }

    public static DataHandler getDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
        return dataHandler;
    }

    public static void setDataHandler(DataHandler newDataHandler) {
        dataHandler = newDataHandler;
    }

    public static CameraHandler getCameraHandler() {
        if (cameraHandler == null) {
            cameraHandler = new CameraHandler();
        }
        return cameraHandler;
    }

    public static RootHandler getRootHandler(Context ctx) {
        if (rootHandler == null) {
            rootHandler = new RootHandler(ctx);
        }
        return rootHandler;
    }

    public static void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public static void initDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
    }
    
    public static IconsHandler getIconsHandler(Context ctx) {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(ctx);
        }    
        
        return iconsPackHandler;
    }
    
    public static void resetIconsHandler(Context ctx) {
        iconsPackHandler = new IconsHandler(ctx);
    }

    public static void applyColorOverride(Activity activity, SharedPreferences prefs) {
        String primaryColorOverride = prefs.getString("primary-color", "");
        if(primaryColorOverride.length() > 0) {
            int mainColor = Color.parseColor(primaryColorOverride);
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(mainColor);
    }
}
