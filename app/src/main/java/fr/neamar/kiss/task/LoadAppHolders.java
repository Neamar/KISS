package fr.neamar.kiss.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import fr.neamar.kiss.pojo.AppHolder;

public class LoadAppHolders extends LoadHolders<AppHolder> {

	public LoadAppHolders(Context context) {
		super(context, "app://");
	}

	@Override
	protected ArrayList<AppHolder> doInBackground(Void... params) {
		long start = System.nanoTime();

		PackageManager manager = context.getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> appsInfo = manager.queryIntentActivities(mainIntent, 0);
		Collections.sort(appsInfo, new ResolveInfo.DisplayNameComparator(manager));

		ArrayList<AppHolder> apps = new ArrayList<AppHolder>();
		for (int i = 0; i < appsInfo.size(); i++) {
			AppHolder app = new AppHolder();
			ResolveInfo info = appsInfo.get(i);

			app.id = holderScheme + info.activityInfo.applicationInfo.packageName + "/"
					+ info.activityInfo.name;
			app.name = info.loadLabel(manager).toString();
			
			//Ugly hack to remove accented characters.
			//Note Java 5 provides a Normalizer method, unavailable for Android :\
			app.nameLowerCased = app.name.toLowerCase().replaceAll("[èéêë]", "e")
					.replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
					.replaceAll("[àâ]", "a").replaceAll("ô", "o").replaceAll("[ÈÉÊË]", "E");;

			app.packageName = info.activityInfo.applicationInfo.packageName;
			app.activityName = info.activityInfo.name;

			apps.add(app);
		}
		long end = System.nanoTime();
		Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");
		return apps;
	}
}
