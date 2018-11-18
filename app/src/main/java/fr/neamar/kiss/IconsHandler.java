package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fr.neamar.kiss.utils.UserHandle;

/**
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

public class IconsHandler {

    private static final String TAG = "IconsHandler";
    // map with available icons packs
    private final HashMap<String, String> iconsPacks = new HashMap<>();
    // map with available drawable for an icons pack
    private final Map<String, String> packagesDrawables = new HashMap<>();
    // instance of a resource object of an icon pack
    private Resources iconPackres;
    // package name of the icons pack
    private String iconsPackPackageName;
    private final PackageManager pm;
    private final Context ctx;

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        loadAvailableIconsPacks();
        loadIconsPack();
    }

    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        loadIconsPack(prefs.getString("icons-pack", "default"));

    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    public void loadIconsPack(String packageName) {

        //clear icons pack
        iconsPackPackageName = packageName;
        packagesDrawables.clear();

        // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default")) {
            return;
        }

        XmlPullParser xpp = null;

        try {
            // search appfilter.xml into icons pack apk resource folder
            iconPackres = pm.getResourcesForApplication(iconsPackPackageName);
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", iconsPackPackageName);
            if (appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            }

            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        //parse <item> xml tags for custom icons
                        if (xpp.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                } else if (xpp.getAttributeName(i).equals("drawable")) {
                                    drawableName = xpp.getAttributeValue(i);
                                }
                            }
                            if (!packagesDrawables.containsKey(componentName)) {
                                packagesDrawables.put(componentName, drawableName);
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing appfilter.xml " + e);
        }

    }

    private Drawable getDefaultAppDrawable(ComponentName componentName, UserHandle userHandle) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                LauncherActivityInfo info = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle()).get(0);
                return info.getBadgedIcon(0);
            } else {
                return pm.getActivityIcon(componentName);
            }
        } catch (NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to found component " + componentName.toString() + e);
            return null;
        }
    }


    /**
     * Get or generate icon for an app
     */
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandle userHandle) {
        // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default")) {
            return this.getDefaultAppDrawable(componentName, userHandle);
        }

        String drawable = packagesDrawables.get(componentName.toString());
        if (drawable != null) { //there is a custom icon
            int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
            if (id > 0) {
                //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
                try {
                    return iconPackres.getDrawable(id);
                } catch(Resources.NotFoundException e) {
                    // Unable to load icon, keep going.
                    e.printStackTrace();
                }
            }
        }

        return this.getDefaultAppDrawable(componentName, userHandle);
    }

    /**
     * Scan for installed icons packs
     */
    private void loadAvailableIconsPacks() {

        List<ResolveInfo> launcherthemes = pm.queryIntentActivities(new Intent("fr.neamar.kiss.THEMES"), PackageManager.GET_META_DATA);
        List<ResolveInfo> adwlauncherthemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

        launcherthemes.addAll(adwlauncherthemes);

        for (ResolveInfo ri : launcherthemes) {
            String packageName = ri.activityInfo.packageName;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String name = pm.getApplicationLabel(ai).toString();
                iconsPacks.put(packageName, name);
            } catch (PackageManager.NameNotFoundException e) {
                // shouldn't happen
                Log.e(TAG, "Unable to found package " + packageName + e);
            }
        }
    }

    public HashMap<String, String> getIconsPacks() {
        return iconsPacks;
    }

}
