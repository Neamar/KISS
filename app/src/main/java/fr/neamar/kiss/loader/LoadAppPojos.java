package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private static String KISS_PACKAGE_NAME;

    public LoadAppPojos(Context context) {
        super(context, "app://");

        KISS_PACKAGE_NAME = context.getPackageName();
    }

    @Override
    protected ArrayList<AppPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        PackageManager manager = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> appsInfo = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(appsInfo, new ResolveInfo.DisplayNameComparator(manager));

        ArrayList<AppPojo> apps = new ArrayList<>();
        for (ResolveInfo info : appsInfo) {
            if (!KISS_PACKAGE_NAME.equals(info.activityInfo.applicationInfo.packageName)) {
                AppPojo app = new AppPojo();

                app.id = pojoScheme + info.activityInfo.applicationInfo.packageName + "/"
                        + info.activityInfo.name;
                app.name = info.loadLabel(manager).toString();

                //Ugly hack to remove accented characters.
                //Note Java 5 provides a Normalizer method, unavailable for Android :\
                app.nameLowerCased = StringNormalizer.normalize(app.name);

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
