package fr.neamar.summon.task;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import fr.neamar.summon.db.DBHelper;
import fr.neamar.summon.holder.AppHolder;

public class LoadAppHoldersFromDB extends LoadHoldersFromDB<AppHolder> {

	public LoadAppHoldersFromDB(Context context) {
		super(context, "app://");
	}

	@Override
	protected ArrayList<AppHolder> doInBackground(Void... params) {
		long start = System.nanoTime();

		ArrayList<AppHolder> apps = DBHelper.getAppHolders(context, holderScheme);

		long end = System.nanoTime();
		Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list " +apps.size()+ "  apps from DB");
		return apps;
	}
}
