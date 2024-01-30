package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import fr.neamar.kiss.KissApplication;
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

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            if (!replacing) {
                KissApplication.getApplication(ctx).resetIconsHandler();
                // Reload application list
                KissApplication.getApplication(ctx).getDataHandler().reloadApps();
                // Remove all installed shortcuts
                for (String packageName : packageNames) {
                    KissApplication.getApplication(ctx).getDataHandler().removeShortcuts(packageName);
                    KissApplication.getApplication(ctx).getDataHandler().removeFromExcluded(packageName);
                }
            }
        } else {
            KissApplication.getApplication(ctx).resetIconsHandler();

            boolean isAnyPackageVisible = isAnyPackageVisible(ctx, packageNames, user);
            if (isAnyPackageVisible) {
                // Reload application list
                KissApplication.getApplication(ctx).getDataHandler().reloadApps();
                // Reload shortcuts
                KissApplication.getApplication(ctx).getDataHandler().reloadShortcuts();
            }
        }
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
            ComponentName launchingComponent = PackageManagerUtils.getLaunchingComponent(ctx, packageName);
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
                packageNames, new UserHandle(),
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        );

    }

}
