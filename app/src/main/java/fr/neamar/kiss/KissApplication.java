package fr.neamar.kiss;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private RootHandler rootHandler;
    private IconsHandler iconsPackHandler;

    // Weak reference to the main activity, this is sadly required for permissions to work correctly.
    public WeakReference<MainActivity> currentMainActivity;

    /**
     * Sometimes, we need to wait for the user to give us permission before we can start an intent.
     * Store the intent here for later use.
     * Ideally, we'd want to use MainActivty to store this, but MainActivity has stateNotNeeded=true
     * which means it's always rebuild from scratch, we can't store any state in it.
     */
    public Intent pendingIntent = null;

    public static KissApplication getApplication(Context context) {
        return (KissApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // When opening the app for any reason, start loading the data handler
        initDataHandler();
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    public void setDataHandler(DataHandler newDataHandler) {
        dataHandler = newDataHandler;
    }

    public RootHandler getRootHandler() {
        if (rootHandler == null) {
            rootHandler = new RootHandler(this);
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    private void initDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        else if(dataHandler.allProvidersHaveLoaded) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
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
