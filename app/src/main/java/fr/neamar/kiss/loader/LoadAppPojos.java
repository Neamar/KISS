package fr.neamar.kiss.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.cache.MemoryCacheHelper;
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

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = KissApplication.getApplication(ctx).getDataHandler().getExcluded();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                    String fullPackageName = user.addUserSuffixToString(appInfo.packageName, '#');
                    AppPojo app = new AppPojo();

                    app.id = user.addUserSuffixToString(pojoScheme + appInfo.packageName + "/" + activityInfo.getName(), '/');

                    app.setName(activityInfo.getLabel().toString());

                    app.packageName = appInfo.packageName;
                    app.activityName = activityInfo.getName();

                    // Wrap Android user handle in opaque container that will work across
                    // all Android versions
                    app.userHandle = user;

                    app.setTags(tagsHandler.getTags(app.id));

                    if (!excludedAppList.contains(app.getComponentName())) {
                        apps.add(app);
                    }
                }
            }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                AppPojo app = new AppPojo();

                app.id = pojoScheme + appInfo.packageName + "/" + info.activityInfo.name;
                app.setName(info.loadLabel(manager).toString());

                app.packageName = appInfo.packageName;
                app.activityName = info.activityInfo.name;

                app.userHandle = new UserHandle();

                app.setTags(tagsHandler.getTags(app.id));
                if (!excludedAppList.contains(app.getComponentName())) {
                    apps.add(app);
                }
            }
        }

        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");

        // cache all app icons
        for (AppPojo app : apps) {
            MemoryCacheHelper.cacheAppIconDrawable(ctx, new ComponentName(app.packageName, app.activityName), app.userHandle);
        }

        return apps;
    }
}
