package fr.neamar.kiss.broadcast;

import static fr.neamar.kiss.dataprovider.ProviderName.APPS;
import static fr.neamar.kiss.dataprovider.ProviderName.SHORTCUTS;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

/**
 * This class gets called when an application is created or removed on the
 * system
 * <p/>
 * We then recreate our data set.
 *
 * @author dorvaryn
 */
public class PackageAddedRemovedHandler extends BroadcastReceiver {

    public static void handleEvent(@NonNull Context ctx, @Nullable String action, @NonNull String[] packageNames, @NonNull UserHandle user, boolean replacing) {
        if (packageNames.length == 1 && packageNames[0].equalsIgnoreCase(ctx.getPackageName())) {
            // When running KISS locally, sending a new version of the APK immediately triggers a "package removed" for fr.neamar.kiss,
            // There is no need to handle this event.
            // Discarding it makes startup time much faster locally as apps don't have to be loaded twice.
            return;
        }

        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (!replacing) {
                for (String packageName : packageNames) {
                    if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("enable-app-history", true)) {
                        KissApplication.getApplication(ctx).getDataHandler().addPackageToHistory(ctx, user, packageName);
                    }
                }
            }
        }

        KissApplication.getMimeTypeCache(ctx).clearCache();

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            if (!replacing) {
                KissApplication.getApplication(ctx).resetIconsHandler();
                // Reload application list
                KissApplication.getApplication(ctx).getDataHandler().reload(APPS);
                // Remove all installed shortcuts
                for (String packageName : packageNames) {
                    KissApplication.getApplication(ctx).getDataHandler().removeShortcuts(packageName);
                    KissApplication.getApplication(ctx).getDataHandler().removeFromExcluded(packageName);
                }
            }
        } else {
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                for (String packageName : packageNames) {
                    moveDataOfReplacedActivity(ctx, packageName, user);
                }
            }
            KissApplication.getApplication(ctx).resetIconsHandler();

            boolean isAnyPackageVisible = isAnyPackageVisible(ctx, packageNames, user);
            if (isAnyPackageVisible) {
                KissApplication.getApplication(ctx).getDataHandler().reload(APPS, SHORTCUTS);
            }
        }
    }

    /**
     * Apps letting users pick their icon (Firefox, Signal...) do it by switching to another launcher activity (#2603).
     * Keep tags and custom name of the previous activity.
     */
    private static void moveDataOfReplacedActivity(Context ctx, String packageName, UserHandle user) {
        LauncherApps launcherApps = ContextCompat.getSystemService(ctx, LauncherApps.class);
        List<LauncherActivityInfo> activities = PackageManagerUtils.getActivityList(launcherApps, packageName, user.getRealHandle());
        if (activities.size() != 1) {
            // can't tell which activity replaced the old one
            return;
        }
        String activityName = activities.get(0).getName();

        String id = user.addUserSuffixToString("app://" + packageName + "/" + activityName, '/');
        // read from db, as the cache also holds default tags
        String oldId = getReplacedKey(DBHelper.loadTags(ctx).keySet(), id, "app://".length(), '/');
        if (oldId != null) {
            KissApplication.getApplication(ctx).getDataHandler().getTagsHandler().moveTags(oldId, id);
        }

        String componentName = AppPojo.getComponentName(packageName, activityName, user);
        String oldComponentName = getReplacedKey(DBHelper.getCustomAppData(ctx).keySet(), componentName, 0, '#');
        if (oldComponentName != null) {
            DBHelper.moveCustomApp(ctx, oldComponentName, componentName);
        }
    }

    /**
     * @param storedKeys    keys data is stored for, as "pkg/activity", followed by userSeparator and user serial for other profiles
     * @param newKey        key of the only activity of its package, same format
     * @param prefixLength  length of a scheme preceding the package, if any
     * @return stored key of another activity of the same package and user, null if there is none or newKey already has data
     */
    static String getReplacedKey(Collection<String> storedKeys, String newKey, int prefixLength, char userSeparator) {
        if (storedKeys.contains(newKey)) {
            return null;
        }
        String packageKey = getPackageKey(newKey, prefixLength, userSeparator);
        for (String key : storedKeys) {
            if (getPackageKey(key, prefixLength, userSeparator).equals(packageKey)) {
                return key;
            }
        }
        return null;
    }

    /**
     * @return key without its activity, e.g. "app://pkg//12" for "app://pkg/activity/12"
     */
    private static String getPackageKey(String key, int prefixLength, char userSeparator) {
        int activityStart = key.indexOf('/', prefixLength) + 1;
        int userStart = key.indexOf(userSeparator, activityStart);
        return key.substring(0, activityStart) + (userStart == -1 ? "" : key.substring(userStart));
    }

    /**
     * @param ctx
     * @param packageNames
     * @param userHandle
     * @return true, if any of packages has activity for launching and is not excluded from KISS
     */
    private static boolean isAnyPackageVisible(Context ctx, String[] packageNames, UserHandle userHandle) {
        Set<String> excludedApps = KissApplication.getApplication(ctx).getDataHandler().getExcluded();
        for (String packageName : packageNames) {
            ComponentName launchingComponent = PackageManagerUtils.getLaunchingComponent(ctx, packageName, userHandle);
            if (launchingComponent != null) {
                boolean isExcluded = excludedApps.contains(AppPojo.getComponentName(launchingComponent.getPackageName(), launchingComponent.getClassName(), userHandle));
                if (!isExcluded) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String[] packageNames = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
        if (packageNames == null && intent.getData() != null) {
            String packageName = intent.getData().getSchemeSpecificPart();
            packageNames = new String[]{packageName};
        }
        if (packageNames == null) {
            return;
        }

        handleEvent(ctx,
                intent.getAction(),
                packageNames, UserHandle.OWNER,
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        );

    }

}
