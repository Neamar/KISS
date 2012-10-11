package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;

public class AppProvider extends Provider {
	private ArrayList<AppHolder> apps = new ArrayList<AppHolder>();

	public AppProvider(Context context) {
		super(context);

		Thread thread = new Thread(null, initAppsList);
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.start();
	}

	protected Runnable initAppsList = new Runnable() {
		public void run() {
			long start = System.nanoTime();

			PackageManager manager = context.getPackageManager();

			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			final List<ResolveInfo> appsInfo = manager.queryIntentActivities(
					mainIntent, 0);
			Collections.sort(appsInfo, new ResolveInfo.DisplayNameComparator(
					manager));

			for (int i = 0; i < appsInfo.size(); i++) {
				AppHolder app = new AppHolder();
				ResolveInfo info = appsInfo.get(i);

				app.id = "app://"
						+ info.activityInfo.applicationInfo.packageName + "/"
						+ info.activityInfo.name;
				app.name = info.loadLabel(manager).toString();
				app.nameLowerCased = app.name.toLowerCase();

				app.packageName = info.activityInfo.applicationInfo.packageName;
				app.activityName = info.activityInfo.name;

				apps.add(app);
			}

			long end = System.nanoTime();
			Log.i("time", Long.toString((end - start) / 1000000)
					+ " milliseconds to list apps");
		}
	};

	public ArrayList<Holder> getResults(String query) {
		query = query.toLowerCase();

		ArrayList<Holder> records = new ArrayList<Holder>();

		int relevance;
		String appNameLowerCased;
		for (int i = 0; i < apps.size(); i++) {
			relevance = 0;
			appNameLowerCased = apps.get(i).nameLowerCased;
			if (appNameLowerCased.startsWith(query))
				relevance = 100;
			else if (appNameLowerCased.contains(" " + query))
				relevance = 50;
			else if (appNameLowerCased.contains(query))
				relevance = 1;

			if (relevance > 0) {
				apps.get(i).displayName = apps.get(i).name.replaceFirst("(?i)("
						+ Pattern.quote(query) + ")", "{$1}");
				apps.get(i).relevance = relevance;
				records.add(apps.get(i));
			}
		}

		return records;
	}

	public Record findById(String id) {
		for (int i = 0; i < apps.size(); i++) {
			if (apps.get(i).id.equals(id)) {
				apps.get(i).displayName = apps.get(i).name;
				return new AppRecord(apps.get(i));
			}

		}

		return null;
	}
}
