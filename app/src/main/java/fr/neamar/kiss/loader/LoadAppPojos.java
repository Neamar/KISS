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

import java.util.*;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ReloadFilter;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private static final String TAG = LoadAppPojos.class.getSimpleName();
    private static final String LABEL_CACHE = "app-labels";

    public LoadAppPojos(Context context, ReloadFilter filter) {
        super(context, "app://", filter);
    }

    @Override
    protected void onPostExecute(List<AppPojo> apps) {
        // Custom names and exclusions are applied here rather than in the background: they may have been changed during the load
        Context ctx = context.get();
        if (ctx != null) {
            DataHandler dataHandler = KissApplication.getApplication(ctx).getDataHandler();
            Set<String> excludedAppList = dataHandler.getExcluded();
            Set<String> excludedFromHistoryAppList = dataHandler.getExcludedFromHistory();
            Map<String, String> customNames = DBHelper.getCustomAppNames(ctx);
            for (AppPojo app : apps) {
                String customName = customNames.get(app.getComponentName());
                if (customName != null) {
                    app.setName(customName);
                }
                app.setExcluded(excludedAppList.contains(app.getComponentName()));
                app.setExcludedFromHistory(excludedFromHistoryAppList.contains(app.id));
            }
        }
        super.onPostExecute(apps);
    }

    @Override
    protected List<AppPojo> doInBackground(Void... params) {
        long start = System.currentTimeMillis();

        Context ctx = context.get();
        if (ctx == null) {
            return Collections.emptyList();
        }

        Optional<PackageNameFilter> filter = getFilter(PackageNameFilter.class);
        List<AppPojo> apps = filter
                .map(packageNameFilter -> packageNameFilter
                        .execute(packageName -> getApps(ctx, packageName)))
                .orElseGet(() -> getApps(ctx, null));

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to list apps");

        return apps;
    }

    private List<AppPojo> getApps(Context ctx, String packageName) {
        List<AppPojo> result = new ArrayList<>();

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
            for (LauncherActivityInfo activityInfo : PackageManagerUtils.getActivityList(launcherApps, packageName, profile)) {
                if (isCancelled()) {
                    break;
                }
                ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                boolean disabled = PackageManagerUtils.isAppSuspended(appInfo) || isQuietModeEnabled(manager, profile);
                if (!disabled || !isPrivateProfile) {
                    String label = null;
                    long lastUpdateTime = PackageManagerUtils.getLastUpdateTime(appInfo);
                    String labelKey = AppPojo.getComponentName(appInfo.packageName, activityInfo.getName(), user) + "@" + lastUpdateTime;
                    if (isLabelCacheValid) {
                        label = labelCache.getString(labelKey, null);
                    }
                    if (label == null) {
                        label = activityInfo.getLabel().toString();
                    }
                    if (lastUpdateTime != Long.MAX_VALUE) {
                        newLabelCache.put(labelKey, label);
                    }
                    final AppPojo app = createPojo(user, appInfo.packageName, activityInfo.getName(), label, disabled);
                    result.add(app);
                }
            }
        }

        if (!isCancelled() && !newLabelCache.equals(labelCache.getAll())) {
            SharedPreferences.Editor editor = labelCache.edit();
            if (packageName == null || !isLabelCacheValid) {
                editor.clear();
            }
            for (Map.Entry<String, String> entry : newLabelCache.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());
            }
            editor.apply();
        }

        return result;
    }

    private static String getLabelCacheStamp(Context ctx) {
        return Build.FINGERPRINT + "|" + ConfigurationCompat.getLocales(ctx.getResources().getConfiguration()).toLanguageTags();
    }



    private boolean isQuietModeEnabled(UserManager manager, android.os.UserHandle profile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return manager.isQuietModeEnabled(profile);
        }
        return false;
    }

    private AppPojo createPojo(UserHandle userHandle, String packageName, String activityName, CharSequence label, boolean disabled) {
        String id = userHandle.addUserSuffixToString(pojoScheme + packageName + "/" + activityName, '/');

        // Custom names, exclusions and tags are applied once the load completes, as they may change in the meantime
        AppPojo app = new AppPojo(id, packageName, activityName, userHandle, disabled);

        app.setName(label.toString());

        return app;
    }
}
