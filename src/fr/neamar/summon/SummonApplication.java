package fr.neamar.summon;

import android.app.Application;
import android.content.Context;

public class SummonApplication extends Application {

	private static DataHandler dataHandler;
    private static DataHandler loadingDataHandler;

	public static DataHandler getDataHandler(Context ctx) {
		if (dataHandler == null) {
			initDataHandler(ctx);
		}
		return dataHandler;
	}

	public static void initDataHandler(Context ctx) {
		if (dataHandler == null) {
            dataHandler = DataHandler.fromDB(ctx);
			loadingDataHandler = new DataHandler(ctx);
		}
	}

	public static void resetDataHandler(Context ctx) {
        loadingDataHandler = new DataHandler(ctx);
	}

    public static void loadingOver(){
        dataHandler = loadingDataHandler;
        loadingDataHandler = null;
    }
}
