package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private final TagsHandler tagsHandler;

    public LoadAppPojos(Context context) {
        super(context, "app://");
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<AppPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<AppPojo> apps = new ArrayList<>();

        if(context.get() == null) {
            return apps;
        }

        String excludedAppList = PreferenceManager.getDefaultSharedPreferences(context.get()).
                getString("excluded-apps-list", context.get().getPackageName() + ";");
        List excludedApps = Arrays.asList(excludedAppList.split(";"));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) context.get().getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) context.get().getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                    String fullPackageName = user.addUserSuffixToString(appInfo.packageName, '#');
                    if (!excludedApps.contains(fullPackageName)) {
                        AppPojo app = new AppPojo();

                        app.id = user.addUserSuffixToString(pojoScheme + appInfo.packageName + "/" + activityInfo.getName(), '/');

                        app.setName(activityInfo.getLabel().toString());

                        app.packageName = appInfo.packageName;
                        app.activityName = activityInfo.getName();

                        // Wrap Android user handle in opaque container that will work across
                        // all Android versions
                        app.userHandle = user;

                        app.setTags(tagsHandler.getTags(app.id));
                        apps.add(app);
                    }
                }
            }
        } else {
            PackageManager manager = context.get().getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                if (!excludedApps.contains(appInfo.packageName)) {
                    AppPojo app = new AppPojo();

                    app.id = pojoScheme + appInfo.packageName + "/" + info.activityInfo.name;
                    app.setName(info.loadLabel(manager).toString());

                    app.packageName = appInfo.packageName;
                    app.activityName = info.activityInfo.name;

                    app.userHandle = new UserHandle();

                    app.setTags(tagsHandler.getTags(app.id));
                    apps.add(app);
                }
            }
        }

        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");
        return apps;
    }
}
