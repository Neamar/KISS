package fr.neamar.kiss.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.shortcut.SaveAllOreoShortcutsAsync;
import fr.neamar.kiss.shortcut.SaveSingleOreoShortcutAsync;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

public class ShortcutUtil {

    final static private String TAG = "ShortcutUtil";

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(String shortcutName){
        return ShortcutPojo.SCHEME + shortcutName.toLowerCase(Locale.ROOT);
    }

    /**
     * @return true if shortcuts are enabled in settings and android version is higher or equals android 8
     */
    public static boolean areShortcutsEnabled(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                prefs.getBoolean("enable-shortcuts", true);

    }

    /**
     * Save all oreo shortcuts to DB
     */
    public static void addAllShortcuts(Context context){
        new SaveAllOreoShortcutsAsync(context).execute();
    }

    /**
     * Save single shortcut to DB via pin request
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static void addShortcut(Context context, Intent intent){
        new SaveSingleOreoShortcutAsync(context, intent).execute();
    }

    /**
     * Remove all shortcuts saved in the database
     */
    public static void removeAllShortcuts(Context context){
        DBHelper.removeAllShortcuts(context);
    }

    /**
     * @return all shortcuts from all applications available on the device
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getAllShortcuts(Context context) {
        return getShortcut(context, null);
    }

    /**
     * @return all shortcuts for given package name
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcut(Context context, String packageName) {
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

        if(!TextUtils.isEmpty(packageName)){
            shortcutQuery.setPackage(packageName);
        }

        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            shortcutInfoList.addAll(launcherApps.getShortcuts(shortcutQuery, profile));
        }

        return shortcutInfoList;
    }

    /**
     * Create ShortcutPojo from ShortcutInfo
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static ShortcutRecord createShortcutRecord(Context context, ShortcutInfo shortcutInfo, boolean includePackageName){
        ShortcutRecord record = new ShortcutRecord();
        record.packageName = shortcutInfo.getPackage();
        record.intentUri = ShortcutPojo.OREO_PREFIX + shortcutInfo.getId();

        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
        Bitmap icon = iconDrawable == null ? null : DrawableUtils.drawableToBitmap(iconDrawable);
        if (icon != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, baos);
            record.icon_blob = baos.toByteArray();
        }

        String appName = getAppNameFromPackageName(context, shortcutInfo.getPackage());

        if (shortcutInfo.getShortLabel() != null) {
            if(includePackageName && !TextUtils.isEmpty(appName)){
                record.name = appName + ": " + shortcutInfo.getShortLabel().toString();
            } else {
                record.name = shortcutInfo.getShortLabel().toString();
            }
        } else if (shortcutInfo.getLongLabel() != null) {
            if(includePackageName && !TextUtils.isEmpty(appName)){
                record.name = appName + ": " + shortcutInfo.getLongLabel().toString();
            } else {
                record.name =shortcutInfo.getLongLabel().toString();
            }
        } else {
            Log.d(TAG, "Invalid shortcut for " + record.packageName + ", ignoring");
            return null;
        }

        return record;
    }

    /**
     *
     * @return App name from package name
     */
    public static String getAppNameFromPackageName(Context context, String Packagename) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(Packagename, PackageManager.GET_META_DATA);
            String appName = (String) packageManager.getApplicationLabel(info);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }



}
