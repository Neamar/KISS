package fr.neamar.kiss;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.utils.IconPackCache;

public class KissApplication extends Application {

    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private volatile DataHandler dataHandler;
    private volatile RootHandler rootHandler;
    private volatile IconsHandler iconsPackHandler;
    private final IconPackCache mIconPackCache = new IconPackCache();

    public static KissApplication getApplication(Context context) {
        if (context instanceof KissApplication) {
            return (KissApplication) context;
        } else {
            return (KissApplication) context.getApplicationContext();
        }
    }

    public static IconPackCache iconPackCache(Context ctx) {
        return getApplication(ctx).mIconPackCache;
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            synchronized (this) {
                if (dataHandler == null) {
                    dataHandler = new DataHandler(this);
                }
            }
        }
        return dataHandler;
    }

    public RootHandler getRootHandler() {
        if (rootHandler == null) {
            synchronized (this) {
                if (rootHandler == null) {
                    rootHandler = new RootHandler(this);
                }
            }
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            synchronized (this) {
                if (iconsPackHandler == null) {
                    iconsPackHandler = new IconsHandler(this);
                }
            }
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * <p>
     * Icons are kept in memory when a game is simply started (UI hidden): coming back to the launcher must not
     * read them again from disk. They are only dropped when the system is really short of memory.
     *
     * @param level the memory-related event that was raised.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            SQLiteDatabase.releaseMemory();
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            mIconPackCache.clearCache(this);
            getIconsHandler().trimMemoryCache();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DBHelper.initDatabase(this);
        InterfaceTweaks.setDefaultNightMode(this);
    }
}
