package fr.neamar.kiss.utils;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.shortcut.SaveAllOreoShortcutsAsync;
import fr.neamar.kiss.shortcut.SaveSingleOreoShortcutAsync;

public class ShortcutUtil {

    private final static String TAG = ShortcutUtil.class.getSimpleName();

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(UserHandle userHandle, @NonNull ShortcutRecord shortcutRecord) {
        if (userHandle == null) {
            return ShortcutPojo.SCHEME + shortcutRecord.name.toLowerCase(Locale.ROOT);
        } else {
            return userHandle.addUserSuffixToString(ShortcutPojo.SCHEME + shortcutRecord.packageName + "/" + shortcutRecord.intentUri, '/');
        }
    }

    /**
     * @return true if this device supports shortcuts, and shortcuts are enabled in settings
     */
    public static boolean areShortcutsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return canDeviceShowShortcuts() && prefs.getBoolean("enable-shortcuts", true);
    }

    /**
     * @return whether this device is running Android 8 (API 26) or higher.
     * Officially shortcuts were first supported by Android 7.1 (API 25),
     * but we use shortcut APIs only available in Android 8.
     */
    public static boolean canDeviceShowShortcuts() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Save all oreo shortcuts to DB
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public static void addAllShortcuts(Context context) {
        new SaveAllOreoShortcutsAsync(context).execute();
    }

    /**
     * Save single shortcut to DB via pin request
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public static void addShortcut(Context context, Intent intent) {
        new SaveSingleOreoShortcutAsync(context, intent).execute();
    }

    /**
     * Remove all shortcuts saved in the database
     */
    public static void removeAllShortcuts(Context context) {
        DBHelper.removeAllShortcuts(context);
    }

    /**
     * @return all shortcuts from all applications available on the device
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getAllShortcuts(Context context) {
        return getShortcuts(context, null);
    }

    /**
     * @return all shortcuts for given package name
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcuts(Context context, String packageName) {
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        if (launcherApps.hasShortcutHostPermission()) {
            LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
            shortcutQuery.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

            if (!TextUtils.isEmpty(packageName)) {
                shortcutQuery.setPackage(packageName);
            }

            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                if (manager.isUserRunning(profile) && manager.isUserUnlocked(profile)) {
                    List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(shortcutQuery, profile);
                    if (shortcuts != null) {
                        shortcutInfoList.addAll(shortcuts);
                    }
                }
            }
        }

        return shortcutInfoList;
    }

    /**
     * @return return a specific shortcut for given package name and id
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public static ShortcutInfo getShortCut(Context context, @NonNull android.os.UserHandle user, String packageName, String shortcutId) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        if (launcherApps.hasShortcutHostPermission() && !TextUtils.isEmpty(packageName)) {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(packageName);
            query.setShortcutIds(Collections.singletonList(shortcutId));
            query.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

            final UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

            // find the correct shortcut
            if (userManager.isUserRunning(user) && userManager.isUserUnlocked(user)) {
                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, user);
                if (shortcuts != null) {
                    for (ShortcutInfo shortcut : shortcuts) {
                        if (shortcut.isEnabled()) {
                            return shortcut;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Create ShortcutPojo from ShortcutInfo
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Nullable
    public static ShortcutRecord createShortcutRecord(Context context, ShortcutInfo shortcutInfo, boolean includePackageName) {
        if (shortcutInfo.hasKeyFieldsOnly()) {
            // If ShortcutInfo holds only key fields shortcut including data must be fetched
            shortcutInfo = getShortCut(context, shortcutInfo.getUserHandle(), shortcutInfo.getPackage(), shortcutInfo.getId());
            if (shortcutInfo == null) {
                return null;
            }
        }

        ShortcutRecord record = new ShortcutRecord();
        record.packageName = shortcutInfo.getPackage();
        record.intentUri = ShortcutPojo.OREO_PREFIX + shortcutInfo.getId();

        String appName = PackageManagerUtils.getLabel(context, shortcutInfo.getPackage(), new UserHandle(context, shortcutInfo.getUserHandle()));

        if (shortcutInfo.getShortLabel() != null) {
            if (includePackageName && !TextUtils.isEmpty(appName)) {
                record.name = appName + ": " + shortcutInfo.getShortLabel().toString();
            } else {
                record.name = shortcutInfo.getShortLabel().toString();
            }
        } else if (shortcutInfo.getLongLabel() != null) {
            if (includePackageName && !TextUtils.isEmpty(appName)) {
                record.name = appName + ": " + shortcutInfo.getLongLabel().toString();
            } else {
                record.name = shortcutInfo.getLongLabel().toString();
            }
        } else {
            Log.d(TAG, "Invalid shortcut for " + record.packageName + ", ignoring");
            return null;
        }

        return record;
    }

    /**
     * @param context
     * @param shortcutInfo
     * @return component name related to {@link ShortcutInfo}.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Nullable
    public static String getComponentName(@NonNull Context context, @Nullable ShortcutInfo shortcutInfo) {
        if (shortcutInfo != null && shortcutInfo.getActivity() != null) {
            UserHandle user = new UserHandle(context, shortcutInfo.getUserHandle());
            return AppPojo.getComponentName(shortcutInfo.getPackage(), shortcutInfo.getActivity().getClassName(), user);
        }
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static boolean isShortcutVisible(@NonNull Context context, @NonNull ShortcutInfo shortcutInfo, @NonNull Set<String> excludedApps, @NonNull Set<String> excludedShortcutApps) {
        if (!shortcutInfo.isEnabled()) {
            return false;
        }

        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (PackageManagerUtils.isPrivateProfile(launcherApps, shortcutInfo.getUserHandle())) {
            if (userManager.isQuietModeEnabled(shortcutInfo.getUserHandle())) {
                return false;
            }
        }

        String packageName = shortcutInfo.getPackage();
        String componentName = ShortcutUtil.getComponentName(context, shortcutInfo);

        // if related package is excluded from KISS then the shortcut must be excluded too
        boolean isExcluded = excludedApps.contains(componentName) || excludedShortcutApps.contains(packageName);
        return !isExcluded;
    }
}
