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
    // list of back images available on an icons pack
    private final List<Bitmap> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private Bitmap maskImage = null;
    // front image of an icons pack
    private Bitmap frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;
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
        backImages.clear();
        cacheClear();

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
                        //parse <iconback> xml tags used as backgroud of generated icons
                        if (xpp.getName().equals("iconback")) {
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmap(drawableName);
                                    if (iconback != null) {
                                        backImages.add(iconback);
                                    }
                                }
                            }
                        }
                        //parse <iconmask> xml tags used as mask of generated icons
                        else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                maskImage = loadBitmap(drawableName);
                            }
                        }
                        //parse <iconupon> xml tags used as front image of generated icons
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                frontImage = loadBitmap(drawableName);
                            }
                        }
                        //parse <scale> xml tags used as scale factor of original bitmap icon
                        else if (xpp.getName().equals("scale")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                factor = Float.valueOf(xpp.getAttributeValue(0));
                            }
                        }
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

    private Bitmap loadBitmap(String drawableName) {
        int id = iconPackres.getIdentifier(drawableName, "drawable", iconsPackPackageName);
        if (id > 0) {
            //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
            Drawable bitmap = iconPackres.getDrawable(id);
            if (bitmap instanceof BitmapDrawable) {
                return ((BitmapDrawable) bitmap).getBitmap();
            }
        }
        return null;
    }

    public Bitmap getBitmap(String component) {
        if (packagesDrawables.containsKey(component)) {
            String drawableName = packagesDrawables.get(component);
            return loadBitmap(drawableName);
        } else {
            return null;
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
        Drawable customIcon = cacheGetDrawable(componentName.toString(), true);
        if (customIcon != null) {
            return customIcon;
        }
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

        // Search first in cache
        Drawable systemIcon = cacheGetDrawable(componentName.toString());
        if (systemIcon != null)
            return systemIcon;

        systemIcon = this.getDefaultAppDrawable(componentName, userHandle);
        if (systemIcon instanceof BitmapDrawable) {
            Drawable generated = generateBitmap(systemIcon);
            cacheStoreDrawable(componentName.toString(), generated);
            return generated;
        }
        return systemIcon;
    }

    private Drawable generateBitmap(Drawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

        // select a random background image
        Random r = new Random();
        int backImageInd = r.nextInt(backImages.size());
        Bitmap backImage = backImages.get(backImageInd);
        int w = backImage.getWidth();
        int h = backImage.getHeight();

        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // draw the background first
        canvas.drawBitmap(backImage, 0, 0, null);

        // scale original icon
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) defaultBitmap).getBitmap(), (int) (w * factor), (int) (h * factor), false);

        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(maskImage, 0, 0, new Paint());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
            canvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);
        } else { // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(frontImage, 0, 0, null);
        }

        return new BitmapDrawable(iconPackres, result);
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

    public Map<String, String> getPackagesDrawables() {
        return packagesDrawables;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    
    private void cacheStoreDrawable(String key, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            File drawableFile = cacheGetFileName(key);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable in cache " + e);
            }
        }
    }

    private Drawable cacheGetDrawable(String key) {
        cacheGetDrawable(key, false);
    }
    private Drawable cacheGetDrawable(String key, boolean isCustomDrawable) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            File file = (isCustomDrawable) ? getCustomFileName(key) : cacheGetFileName(key);
            fis = new FileInputStream(file);
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        return new File(getIconsCacheDir() + iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    public File getCustomFileName(String key) {
        return new File(getCustomIconsDir().getPath() + key.hashCode() + ".png");
    }

    private File getCustomIconsDir() {
        return new File(this.ctx.getFilesDir().getPath() + "/custom/");
    }

    private File getIconsCacheDir() {
        return new File(this.ctx.getCacheDir().getPath() + "/icons/");
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        clearIconData(false);
    }
    private void clearIconData(boolean isCustom) {
        File iconsDir = (isCustom) ? this.getCustomIconsDir()
                                   : this.getIconsCacheDir();
        if (!iconsDir.isDirectory())
            return;

        for (File item : iconsDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
            }
        }
    }

}
