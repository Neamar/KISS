package fr.neamar.kiss;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private CameraHandler cameraHandler;
    private RootHandler rootHandler;
    private IconsHandler iconsPackHandler;

    public static KissApplication getApplication(Context context) {
        return (KissApplication) context.getApplicationContext();
    }

    public DataHandler getDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
        return dataHandler;
    }

    public void setDataHandler(DataHandler newDataHandler) {
        dataHandler = newDataHandler;
    }

    public CameraHandler getCameraHandler() {
        if (cameraHandler == null) {
            cameraHandler = new CameraHandler();
        }
        return cameraHandler;
    }

    public RootHandler getRootHandler(Context ctx) {
        if (rootHandler == null) {
            rootHandler = new RootHandler(ctx);
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public void initDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
        else {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            ctx.sendBroadcast(i);
        }
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

}
