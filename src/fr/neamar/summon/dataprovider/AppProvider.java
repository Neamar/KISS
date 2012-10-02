package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;

public class AppProvider extends Provider {
	private ArrayList<AppHolder> apps = new ArrayList<AppHolder>();

	public AppProvider(Context context) {
		super(context);

		PackageManager manager = context.getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> appsInfo = manager.queryIntentActivities(
				mainIntent, 0);
		Collections.sort(appsInfo, new ResolveInfo.DisplayNameComparator(manager));

		for (int i = 0; i < appsInfo.size(); i++) {
			AppHolder app = new AppHolder();
			ResolveInfo info = appsInfo.get(i);

			app.appName = info.loadLabel(manager).toString();
			app.icon = info.activityInfo.loadIcon(manager);
			app.setActivity(new ComponentName(
					info.activityInfo.applicationInfo.packageName,
					info.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			apps.add(app);
		}
	}

	public ArrayList<Record> getRecords(String query) {
		ArrayList<Record> records = new ArrayList<Record>();

		for (int i = 0; i < apps.size(); i++) {
			if (apps.get(i).appName.startsWith(query)) {
				Record r = new AppRecord(apps.get(i));
				records.add(r);
			}
		}

		return records;
	}
}
