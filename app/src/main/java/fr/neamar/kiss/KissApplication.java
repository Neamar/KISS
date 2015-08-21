package fr.neamar.kiss;

import android.app.Application;
import android.content.Context;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private static DataHandler dataHandler;
    private static RootHandler rootHandler;

    public static DataHandler getDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
        return dataHandler;
    }
    
    public static RootHandler getRootHander(Context ctx) {
    	if (rootHandler == null) {
    		rootHandler = new RootHandler(ctx);
    	}
    	return rootHandler;
    }
    
    public static void resetRootHander(Context ctx) {
    	rootHandler.resetRootHander(ctx);    	
    }

    public static void initDataHandler(Context ctx) {
        if (dataHandler == null) {
            dataHandler = new DataHandler(ctx);
        }
    }

    public static void resetDataHandler(Context ctx) {
        dataHandler = new DataHandler(ctx);
    }
}
