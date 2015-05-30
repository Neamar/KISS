package fr.neamar.kiss;

import android.app.Application;
import android.content.Context;

public class KissApplication extends Application {
	private static DataHandler dataHandler;

	/**
	 * Number of ms to wait, after a click occurred, to record a launch
	 * Setting this value to 0 removes all animations
	 */
	public static final int TOUCH_DELAY = 100;

	public static DataHandler getDataHandler(Context ctx) {
		if (dataHandler == null) {
			dataHandler = new DataHandler(ctx);
		}
		return dataHandler;
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
