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
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
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
    public static String generateShortcutId(ShortcutRecord shortcutRecord) {
        return ShortcutPojo.SCHEME + shortcutRecord.name.toLowerCase(Locale.ROOT);
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
    public static ShortcutInfo getShortCut(Context context, String packageName, String shortcutId) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        final UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        if (launcherApps.hasShortcutHostPermission() && !TextUtils.isEmpty(packageName)) {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(packageName);
            query.setShortcutIds(Collections.singletonList(shortcutId));
            query.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

            List<android.os.UserHandle> userHandles = launcherApps.getProfiles();

            // find the correct UserHandle and get shortcut
            for (android.os.UserHandle userHandle : userHandles) {
                if (userManager.isUserRunning(userHandle) && userManager.isUserUnlocked(userHandle)) {
                    List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, userHandle);
                    if (shortcuts != null) {
                        for (ShortcutInfo shortcut : shortcuts) {
                            if (shortcut.isEnabled()) {
                                return shortcut;
                            }
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
    public static ShortcutRecord createShortcutRecord(Context context, ShortcutInfo shortcutInfo, boolean includePackageName) {
        if (shortcutInfo.hasKeyFieldsOnly()) {
            // If ShortcutInfo holds only key fields shortcut including data must be fetched
            shortcutInfo = getShortCut(context, shortcutInfo.getPackage(), shortcutInfo.getId());
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
            UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            fr.neamar.kiss.utils.UserHandle user = new fr.neamar.kiss.utils.UserHandle(manager.getSerialNumberForUser(shortcutInfo.getUserHandle()), shortcutInfo.getUserHandle());
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

        if (userManager.getSerialNumberForUser(shortcutInfo.getUserHandle()) != 0) {
            // Hide all shortcuts for apps of managed profiles. Shortcuts currently don't support multiple profiles at all!!!
            return false;
        }

        String packageName = shortcutInfo.getPackage();
        String componentName = ShortcutUtil.getComponentName(context, shortcutInfo);

        // if related package is excluded from KISS then the shortcut must be excluded too
        boolean isExcluded = excludedApps.contains(componentName) || excludedShortcutApps.contains(packageName);
        return !isExcluded;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static boolean pinShortcut(@NonNull Context context, @NonNull String packageName, @NonNull String shortcutId) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps.hasShortcutHostPermission()) {
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getShortcuts(context, packageName);
            final ShortcutInfo shortcutToPin = shortcutInfos.stream()
                    .filter(shortcutInfo -> !shortcutInfo.isPinned())
                    .filter(shortcutInfo -> shortcutInfo.getId().equals(shortcutId))
                    .findAny()
                    .orElse(null);
            if (shortcutToPin != null) {
                List<String> pinnedShortcutIds = shortcutInfos.stream()
                        .filter(ShortcutInfo::isPinned)
                        .filter(shortcutInfo -> shortcutInfo.getUserHandle().equals(shortcutToPin.getUserHandle()))
                        .map(ShortcutInfo::getId)
                        .collect(Collectors.toList());
                pinnedShortcutIds.add(shortcutId);

                launcherApps.pinShortcuts(packageName, pinnedShortcutIds, shortcutToPin.getUserHandle());
                KissApplication.getApplication(context).getDataHandler().updateShortcut(shortcutToPin, false);
                return true;
            }
        }
        return false;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static boolean unpinShortcut(@NonNull Context context, @NonNull String packageName, @NonNull String shortcutId) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps.hasShortcutHostPermission()) {
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getShortcuts(context, packageName);
            final ShortcutInfo shortcutToUnpin = shortcutInfos.stream()
                    .filter(shortcutInfo -> shortcutInfo.isPinned())
                    .filter(shortcutInfo -> shortcutInfo.getId().equals(shortcutId))
                    .findAny()
                    .orElse(null);
            if (shortcutToUnpin != null) {
                List<String> pinnedShortcutIds = shortcutInfos.stream()
                        .filter(ShortcutInfo::isPinned)
                        .filter(shortcutInfo -> shortcutInfo.getUserHandle().equals(shortcutToUnpin.getUserHandle()))
                        .map(ShortcutInfo::getId)
                        .collect(Collectors.toList());
                pinnedShortcutIds.remove(shortcutId);

                launcherApps.pinShortcuts(packageName, pinnedShortcutIds, shortcutToUnpin.getUserHandle());
                return true;
            }
        }
        return false;
    }
}
