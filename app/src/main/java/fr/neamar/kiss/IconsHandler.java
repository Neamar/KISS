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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

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
import fr.neamar.kiss.utils.DrawableUtils;

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
        clearOldCache();
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
    void loadIconsPack(String packageName) {

        //clear icons pack
        iconsPackPackageName = packageName;
        packagesDrawables.clear();
        backImages.clear();
        cacheClear();

        // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default") || DrawableUtils.isIconsPackAdaptive(iconsPackPackageName)) {
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
                        else if (xpp.getName().equals("scale") && xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                            factor = Float.parseFloat(xpp.getAttributeValue(0));
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
            Drawable bitmap = iconPackres.getDrawable(id);
            if (bitmap instanceof BitmapDrawable) {
                return ((BitmapDrawable) bitmap).getBitmap();
            }
        }
        return null;
    }


    @NonNull
    private Drawable getDefaultAppDrawable(ComponentName componentName, UserHandle userHandle) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        Drawable icon = info.getBadgedIcon(0);

                        // Handle the adaptive icons
                        if(DrawableUtils.isIconsPackAdaptive(iconsPackPackageName)) {
                            return DrawableUtils.handleAdaptiveIcons(ctx, icon);
                        }
                        else {
                            return icon;
                        }
                    }
                }

                // This should never happen, let's just return the first icon
                return icons.get(0).getBadgedIcon(0);
            } else {
                return pm.getActivityIcon(componentName);
            }
        } catch (NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to found component " + componentName.toString() + e);
            return new ColorDrawable(Color.WHITE);
        }
    }


    /**
     * Get or generate icon for an app
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandle userHandle) {
        // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default") || DrawableUtils.isIconsPackAdaptive(iconsPackPackageName)) {
            return this.getDefaultAppDrawable(componentName, userHandle);
        }

        // Search first in cache
        {
            Drawable cacheIcon = cacheGetDrawable(componentName.toString());
            if (cacheIcon != null)
                return cacheIcon;
        }

        // check the icon pack for a resource
        {
            String drawable = packagesDrawables.get(componentName.toString());
            if (drawable != null) { //there is a custom icon
                int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
                if (id > 0) {
                    try {
                        return iconPackres.getDrawable(id);
                    } catch (Resources.NotFoundException e) {
                        // Unable to load icon, keep going.
                        e.printStackTrace();
                    }
                }
            }
        }

        // apply icon pack back, mask and front over the system drawable
        Drawable systemIcon = this.getDefaultAppDrawable(componentName, userHandle);
        BitmapDrawable generated;
        if (systemIcon instanceof BitmapDrawable) {
            generated = generateBitmap((BitmapDrawable) systemIcon);
        } else {
            Bitmap bitmap;
            if (systemIcon.getIntrinsicWidth() <= 0 || systemIcon.getIntrinsicHeight() <= 0)
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
            else
                bitmap = Bitmap.createBitmap(systemIcon.getIntrinsicWidth(), systemIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            systemIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            systemIcon.draw(new Canvas(bitmap));
            generated = generateBitmap(new BitmapDrawable(this.ctx.getResources(), bitmap));
        }
        cacheStoreDrawable(componentName.toString(), generated);
        return generated;
    }

    private BitmapDrawable generateBitmap(BitmapDrawable defaultBitmap) {

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
        canvas.setDensity(Bitmap.DENSITY_NONE);

        // draw the background first
        canvas.drawBitmap(backImage, 0, 0, null);

        // scale original icon
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap.getBitmap(), (int) (w * factor), (int) (h * factor), false);
        scaledBitmap.setDensity(Bitmap.DENSITY_NONE);

        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Matrix matScale = new Matrix();
            matScale.setScale(w / (float) maskImage.getWidth(), h / (float) maskImage.getHeight());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2f, (h - scaledBitmap.getHeight()) / 2f, null);
            canvas.drawBitmap(maskImage, matScale, paint);
            paint.setXfermode(null);
        } else { // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2f, (h - scaledBitmap.getHeight()) / 2f, null);
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

    HashMap<String, String> getIconsPacks() {
        return iconsPacks;
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

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
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
        return new File(getIconsCacheDir(), iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        File dir = new File(this.ctx.getCacheDir(), "icons");
        if (!dir.exists() && !dir.mkdir())
            throw new IllegalStateException("failed to create path " + dir.getPath());
        return dir;
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        File[] fileList = cacheDir.listFiles();
        if (fileList != null) {
            for (File item : fileList) {
                if (!item.delete()) {
                    Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
                }
            }
        }
    }

    // Before we fixed the cache path actually returning a folder, a lot of icons got dumped
    // directly in ctx.getCacheDir() so we need to clean it
    private void clearOldCache() {
        File newCacheDir = new File(this.ctx.getCacheDir(), "icons");

        if (!newCacheDir.isDirectory()) {
            File[] fileList = ctx.getCacheDir().listFiles();
            if (fileList != null) {
                int count = 0;
                for (File file : fileList) {
                    if (file.isFile())
                        count += file.delete() ? 1 : 0;
                }
                Log.i(TAG, "Removed " + count + " cache file(s) from the old path");
            }
        }
    }
}
