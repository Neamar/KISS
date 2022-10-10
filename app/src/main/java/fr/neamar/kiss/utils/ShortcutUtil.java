package fr.neamar.kiss.utils;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.shortcut.SaveAllOreoShortcutsAsync;
import fr.neamar.kiss.shortcut.SaveSingleOreoShortcutAsync;

public class ShortcutUtil {

    final static private String TAG = "ShortcutUtil";

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(String shortcutName) {
        return ShortcutPojo.SCHEME + shortcutName.toLowerCase(Locale.ROOT);
    }

    /**
     * @return true if shortcuts are enabled in settings and android version is higher or equals android 8
     */
    public static boolean areShortcutsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                prefs.getBoolean("enable-shortcuts", true);

    }

    /**
     * Save all oreo shortcuts to DB
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static void addAllShortcuts(Context context) {
        new SaveAllOreoShortcutsAsync(context).execute();
    }

    /**
     * Save single shortcut to DB via pin request
     */
    @TargetApi(Build.VERSION_CODES.O)
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
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getAllShortcuts(Context context) {
        return getShortcuts(context, null);
    }

    /**
     * @return all shortcuts for given package name
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcuts(Context context, String packageName) {
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

        if (!TextUtils.isEmpty(packageName)) {
            shortcutQuery.setPackage(packageName);
        }

        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            shortcutInfoList.addAll(launcherApps.getShortcuts(shortcutQuery, profile));
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
            for (UserHandle userHandle : userHandles) {
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
    @TargetApi(Build.VERSION_CODES.O)
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

        String appName = getAppNameFromPackageName(context, shortcutInfo.getPackage());

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
     * @return App name from package name
     */
    public static String getAppNameFromPackageName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return (String) packageManager.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @param context
     * @param shortcutInfo
     * @return component name related to {@link ShortcutInfo}.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Nullable
    public static String getComponentName(Context context, ShortcutInfo shortcutInfo) {
        if (shortcutInfo != null && shortcutInfo.getActivity() != null) {
            UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            fr.neamar.kiss.utils.UserHandle user = new fr.neamar.kiss.utils.UserHandle(manager.getSerialNumberForUser(shortcutInfo.getUserHandle()), shortcutInfo.getUserHandle());
            return AppPojo.getComponentName(shortcutInfo.getPackage(), shortcutInfo.getActivity().getClassName(), user);
        }
        return null;
    }

    /**
     * @param shortcuts all shortcuts
     * @return shortcuts which should be updated
     */
    public static List<ShortcutInfo> getShortcutsToUpdate(List<ShortcutInfo> shortcuts) {
        List<ShortcutInfo> result = new ArrayList<>();
        for (ShortcutInfo shortcut : shortcuts) {
            if (isShortcutToUpdate(shortcut)) {
                result.add(shortcut);
            }
        }
        return result;
    }

    /**
     * @param shortcut
     * @return true, if shortcut must be update on change of package
     */
    private static boolean isShortcutToUpdate(ShortcutInfo shortcut) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        // do not update pinned shortcuts, this should be done by user through UI only
        return !shortcut.isPinned();
    }


}
