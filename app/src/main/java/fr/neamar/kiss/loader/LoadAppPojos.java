package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.pojo.AppPojo;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private static SharedPreferences prefs;

    public LoadAppPojos(Context context) {
        super(context, "app://");
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected ArrayList<AppPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        PackageManager manager = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> appsInfo = manager.queryIntentActivities(mainIntent, 0);
        if (prefs.getString("sort-apps", "alphabetical").equals("invertedAlphabetical")) {
            Collections.sort(appsInfo, Collections.reverseOrder(new ResolveInfo.DisplayNameComparator(manager)));
        }
        else {
            Collections.sort(appsInfo, new ResolveInfo.DisplayNameComparator(manager));
        }

        ArrayList<AppPojo> apps = new ArrayList<>();
        String excludedAppList = PreferenceManager.getDefaultSharedPreferences(context).
                getString("excluded-apps-list", context.getPackageName() + ";");
        List excludedApps = Arrays.asList(excludedAppList.split(";"));

        for (ResolveInfo info : appsInfo) {
            if (!excludedApps.contains(info.activityInfo.applicationInfo.packageName)) {
                AppPojo app = new AppPojo();

                app.id = pojoScheme + info.activityInfo.applicationInfo.packageName + "/"
                        + info.activityInfo.name;
                app.setName(info.loadLabel(manager).toString());

                app.packageName = info.activityInfo.applicationInfo.packageName;
                app.activityName = info.activityInfo.name;

                apps.add(app);
            }
        }
        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");
        return apps;
    }
}
