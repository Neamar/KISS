package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.UserManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private static final String TAG = LoadAppPojos.class.getSimpleName();
    private static final String LABEL_CACHE = "app-labels";

    public LoadAppPojos(Context context) {
        super(context, AppPojo.SCHEME);
    }

    @Override
    protected void onPostExecute(List<AppPojo> apps) {
        // Custom names and exclusions are applied here rather than in the background: they may have been changed during the load
        Context ctx = context.get();
        if (ctx != null) {
            DataHandler dataHandler = KissApplication.getApplication(ctx).getDataHandler();
            Set<String> excludedAppList = dataHandler.getExcluded();
            Set<String> excludedFromHistoryAppList = dataHandler.getExcludedFromHistory();
            Set<String> excludedShortcutsAppList = dataHandler.getExcludedShortcutApps();
            Map<String, String> customNames = DBHelper.getCustomAppNames(ctx);
            moveDataOfReplacedActivities(ctx, dataHandler, apps, customNames);
            for (AppPojo app : apps) {
                String customName = customNames.get(app.id);
                if (customName != null) {
                    app.setName(customName);
                }
                app.setExcluded(excludedAppList.contains(app.id));
                app.setExcludedFromHistory(excludedFromHistoryAppList.contains(app.id));
                app.setExcludedShortcuts(excludedShortcutsAppList.contains(app.packageName));
            }
        }
        super.onPostExecute(apps);
    }

    @Override
    protected List<AppPojo> doInBackground(Void... params) {
        long start = System.currentTimeMillis();

        List<AppPojo> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        // Reading a label loads the app's resources: slow, and it blocks the main thread on Android 10-11 (#2715).
        // Only do it for new or updated apps, or when the system or its languages changed.
        SharedPreferences labelCache = ctx.getSharedPreferences(LABEL_CACHE, Context.MODE_PRIVATE);
        String stamp = getLabelCacheStamp(ctx);
        boolean isLabelCacheValid = stamp.equals(labelCache.getString("stamp", null));
        // replaces the cache after the load, to forget removed apps
        Map<String, String> newLabelCache = new HashMap<>();
        newLabelCache.put("stamp", stamp);

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
                    long lastUpdateTime = PackageManagerUtils.getLastUpdateTime(appInfo);
                    String labelKey = AppPojo.getId(appInfo.packageName, activityInfo.getName(), user) + "@" + lastUpdateTime;
                    if (isLabelCacheValid) {
                        label = labelCache.getString(labelKey, null);
                    }
                    if (label == null) {
                        label = activityInfo.getLabel().toString();
                    }
                    if (lastUpdateTime != Long.MAX_VALUE) {
                        newLabelCache.put(labelKey, label);
                        // Also keep the name of the app, displayed on its shortcuts
                        String appNameKey = getAppNameKey(appInfo.packageName, user, lastUpdateTime);
                        if (!newLabelCache.containsKey(appNameKey)) {
                            String appName = isLabelCacheValid ? labelCache.getString(appNameKey, null) : null;
                            if (appName == null) {
                                appName = ctx.getPackageManager().getApplicationLabel(appInfo).toString();
                            }
                            newLabelCache.put(appNameKey, appName);
                        }
                    }
                    final AppPojo app = createPojo(user, appInfo.packageName, activityInfo.getName(), label, disabled);
                    apps.add(app);
                }
            }
        }

        if (!isCancelled() && !newLabelCache.equals(labelCache.getAll())) {
            SharedPreferences.Editor editor = labelCache.edit().clear();
            for (Map.Entry<String, String> entry : newLabelCache.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());
            }
            editor.apply();
        }

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to list apps");

        return apps;
    }

    /**
     * Apps letting users pick their icon (Firefox, Signal...) do it by switching to another launcher activity (#2603).
     * Keep tags and custom name of the previous activity.
     */
    private static void moveDataOfReplacedActivities(Context ctx, DataHandler dataHandler, List<AppPojo> apps, Map<String, String> customNames) {
        List<String> ids = new ArrayList<>(apps.size());
        for (AppPojo app : apps) {
            ids.add(app.id);
        }

        // read from db, as the cache also holds default tags
        Map<String, String> moves = getReplacedActivities(DBHelper.loadTags(ctx).keySet(), ids);
        for (Map.Entry<String, String> move : moves.entrySet()) {
            dataHandler.getTagsHandler().moveTags(move.getKey(), move.getValue());
        }

        moves = getReplacedActivities(customNames.keySet(), ids);
        for (Map.Entry<String, String> move : moves.entrySet()) {
            DBHelper.moveCustomApp(ctx, move.getKey(), move.getValue());
            customNames.put(move.getValue(), customNames.remove(move.getKey()));
        }
    }

    /**
     * @param storedIds    app ids data is stored for
     * @param installedIds ids of installed apps
     * @return stored key -> installed key, for stored activities no longer installed whose package now has a single activity without data
     */
    static Map<String, String> getReplacedActivities(Collection<String> storedIds, Collection<String> installedIds) {
        Map<String, String> installedByPackage = new HashMap<>();
        for (String key : installedIds) {
            String packageKey = getPackageKey(key);
            // null when the package has several activities: can't tell which one replaced the old one
            installedByPackage.put(packageKey, installedByPackage.containsKey(packageKey) ? null : key);
        }

        Map<String, String> moves = new HashMap<>();
        for (String key : storedIds) {
            // an installed key maps to itself or to null, and is skipped below
            String newKey = installedByPackage.get(getPackageKey(key));
            if (newKey != null && !storedIds.contains(newKey) && !moves.containsValue(newKey)) {
                moves.put(key, newKey);
            }
        }
        return moves;
    }

    /**
     * @return key without its activity, e.g. "app://pkg//12" for "app://pkg/activity/12"
     */
    private static String getPackageKey(String id) {
        int activityStart = id.indexOf('/', AppPojo.SCHEME.length()) + 1;
        int userStart = id.indexOf('/', activityStart);
        return id.substring(0, activityStart) + (userStart == -1 ? "" : id.substring(userStart));
    }

    private static String getLabelCacheStamp(Context ctx) {
        return Build.FINGERPRINT + "|" + ConfigurationCompat.getLocales(ctx.getResources().getConfiguration()).toLanguageTags();
    }

    private static String getAppNameKey(String packageName, UserHandle user, long lastUpdateTime) {
        return user.addUserSuffixToString(packageName, '#') + "@" + lastUpdateTime;
    }

    /**
     * @return name of given app as read by the last load of apps, null if unknown
     */
    @Nullable
    public static String getCachedAppName(Context ctx, ApplicationInfo appInfo, UserHandle user) {
        SharedPreferences labelCache = ctx.getSharedPreferences(LABEL_CACHE, Context.MODE_PRIVATE);
        if (!getLabelCacheStamp(ctx).equals(labelCache.getString("stamp", null))) {
            return null;
        }
        return labelCache.getString(getAppNameKey(appInfo.packageName, user, PackageManagerUtils.getLastUpdateTime(appInfo)), null);
    }

    private boolean isQuietModeEnabled(UserManager manager, android.os.UserHandle profile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return manager.isQuietModeEnabled(profile);
        }
        return false;
    }

    private AppPojo createPojo(UserHandle userHandle, String packageName, String activityName, CharSequence label, boolean disabled) {
        String id = AppPojo.getId(packageName, activityName, userHandle);

        // Custom names, exclusions and tags are applied once the load completes, as they may change in the meantime
        AppPojo app = new AppPojo(id, packageName, activityName, userHandle, disabled);

        app.setName(label.toString());

        return app;
    }
}
