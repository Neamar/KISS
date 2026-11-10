package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.UserManager;

import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.AppRecord;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private static final String TAG = LoadAppPojos.class.getSimpleName();
    private static final String LABEL_CACHE_STAMP = "stamp";
    private final TagsHandler tagsHandler;

    public LoadAppPojos(Context context) {
        super(context, "app://");
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected List<AppPojo> doInBackground(Void... params) {
        long start = System.currentTimeMillis();

        List<AppPojo> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = KissApplication.getApplication(ctx).getDataHandler().getExcluded();
        Set<String> excludedFromHistoryAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedFromHistory();
        Set<String> excludedShortcutsAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedShortcutApps();

        // Reading a label loads the app's resources: slow, and it blocks the main thread on Android 10-11 (#2715).
        // Only do it for new or updated apps, or when the system or its languages changed.
        SharedPreferences labelCache = ctx.getSharedPreferences("app-labels", Context.MODE_PRIVATE);
        String stamp = Build.FINGERPRINT + "|" + ConfigurationCompat.getLocales(ctx.getResources().getConfiguration()).toLanguageTags();
        boolean isLabelCacheValid = stamp.equals(labelCache.getString(LABEL_CACHE_STAMP, null));
        // rewritten on every load, to forget removed apps
        SharedPreferences.Editor newLabelCache = labelCache.edit().clear().putString(LABEL_CACHE_STAMP, stamp);

        UserManager manager = ContextCompat.getSystemService(ctx, UserManager.class);
        LauncherApps launcherApps = ContextCompat.getSystemService(ctx, LauncherApps.class);

        // Handle multi-profile support introduced in Android 5 (#542)
        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            boolean isPrivateProfile = PackageManagerUtils.isPrivateProfile(launcherApps, profile);
            UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
            for (LauncherActivityInfo activityInfo : PackageManagerUtils.getActivityList(launcherApps, null, profile)) {
                if (isCancelled()) {
                    break;
                }
                ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                boolean disabled = PackageManagerUtils.isAppSuspended(appInfo) || isQuietModeEnabled(manager, profile);
                if (!disabled || !isPrivateProfile) {
                    String label = null;
                    long lastUpdateTime = PackageManagerUtils.getLastUpdateTime(ctx, appInfo.packageName);
                    String labelKey = AppPojo.getComponentName(appInfo.packageName, activityInfo.getName(), user) + "@" + lastUpdateTime;
                    if (isLabelCacheValid) {
                        label = labelCache.getString(labelKey, null);
                    }
                    if (label == null) {
                        label = activityInfo.getLabel().toString();
                    }
                    if (lastUpdateTime != Long.MAX_VALUE) {
                        newLabelCache.putString(labelKey, label);
                    }
                    final AppPojo app = createPojo(user, appInfo.packageName, activityInfo.getName(), label, disabled, excludedAppList, excludedFromHistoryAppList, excludedShortcutsAppList);
                    apps.add(app);
                }
            }
        }

        if (!isCancelled()) {
            newLabelCache.apply();
        }

        Map<String, AppRecord> customApps = DBHelper.getCustomAppData(ctx);
        for (AppPojo app : apps) {
            AppRecord customApp = customApps.get(app.getComponentName());
            if (customApp == null)
                continue;
            if (customApp.hasCustomName())
                app.setName(customApp.name);
        }

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to list apps");

        return apps;
    }

    private boolean isQuietModeEnabled(UserManager manager, android.os.UserHandle profile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return manager.isQuietModeEnabled(profile);
        }
        return false;
    }

    private AppPojo createPojo(UserHandle userHandle, String packageName, String activityName, CharSequence label, boolean disabled, Set<String> excludedAppList, Set<String> excludedFromHistoryAppList, Set<String> excludedShortcutsAppList) {
        String id = userHandle.addUserSuffixToString(pojoScheme + packageName + "/" + activityName, '/');

        boolean isExcluded = excludedAppList.contains(AppPojo.getComponentName(packageName, activityName, userHandle));
        boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);
        boolean isExcludedShortcuts = excludedShortcutsAppList.contains(packageName);

        AppPojo app = new AppPojo(id, packageName, activityName, userHandle, isExcluded, isExcludedFromHistory, isExcludedShortcuts, disabled);

        app.setName(label.toString());

        app.setTags(tagsHandler.getTags(app.id));

        return app;
    }
}
