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
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.AppRecord;
import fr.neamar.kiss.db.DBHelper;
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
        long start = System.currentTimeMillis();

        ArrayList<AppPojo> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = KissApplication.getApplication(ctx).getDataHandler().getExcluded();
        Set<String> excludedFromHistoryAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedFromHistory();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                    final AppPojo app = createPojo(user, appInfo.packageName, activityInfo.getName(), activityInfo.getLabel(), excludedAppList, excludedFromHistoryAppList);
                    apps.add(app);
                }
            }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                final AppPojo app = createPojo(new UserHandle(), appInfo.packageName, info.activityInfo.name, info.loadLabel(manager), excludedAppList, excludedFromHistoryAppList);
                apps.add(app);
            }
        }

        HashMap<String, AppRecord> customApps = DBHelper.getCustomAppData(ctx);
        for (AppPojo app : apps) {
            AppRecord customApp = customApps.get(app.getComponentName());
            if (customApp == null)
                continue;
            if (customApp.hasCustomName())
                app.setName(customApp.name);
            if (customApp.hasCustomIcon())
                app.setCustomIconId(customApp.dbId);
        }

        long end = System.currentTimeMillis();
        Log.i("time", (end - start) + " milliseconds to list apps");

        return apps;
    }

    private AppPojo createPojo(UserHandle userHandle, String packageName, String activityName, CharSequence label, Set<String> excludedAppList, Set<String> excludedFromHistoryAppList) {
        String id = userHandle.addUserSuffixToString(pojoScheme + packageName + "/" + activityName, '/');

        boolean isExcluded = excludedAppList.contains(AppPojo.getComponentName(packageName, activityName, userHandle));
        boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);

        AppPojo app = new AppPojo(id, packageName, activityName, userHandle, isExcluded, isExcludedFromHistory);

        app.setName(label.toString());

        app.setTags(tagsHandler.getTags(app.id));

        return app;
    }
}
