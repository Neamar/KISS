package fr.neamar.kiss;

import android.content.Context;

public class KissApplication {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private static DataHandler dataHandler;
    private static CameraHandler cameraHandler;
    private static RootHandler rootHandler;
    private static IconHandler iconHandler;
    private static IconPack iconPack;

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

    public static IconHandler getIconsHandler(Context ctx) {
        if (iconHandler == null) {
            iconHandler = new IconHandler(ctx);
        }

        return iconHandler;
    }

    public static void resetIconsHandler() {
        iconHandler.reset();
    }

    public static IconPack getIconPack() {
        return iconPack;
    }

    public static void setIconPack(IconPack pack) {
        iconPack = pack;
        iconHandler.reset();
    }
}
