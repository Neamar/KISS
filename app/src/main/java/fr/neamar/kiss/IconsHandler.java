package fr.neamar.kiss;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
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
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 * 
 */

public class IconsHandler {

    // map with available icons packs
    private HashMap<String, String> iconsPacks = new HashMap<>();
    // map with available drawable for an icons pack
    private HashMap<String, String> packagesDrawables = new HashMap<>();
    // instance of a resource object of an icon pack
    private Resources iconPackres;
    // package name of the icons pack
    private String iconsPackPackageName;
    // list of back images available on an icons pack
    private List<Bitmap> backImages = new ArrayList<Bitmap>();
    // bitmap mask of an icons pack
    private Bitmap maskImage = null;
    // front image of an icons pack
    private Bitmap frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;

    private PackageManager pm;
    private Context ctx;

    private static final String TAG = "IconsHandler";

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        loadAvailableIconsPacks();
        loadIconsPack();
    }

    public void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        loadIconsPack(prefs.getString("icons-pack", "default"));

    }

    public void loadIconsPack(String packageName) {

        //clear icons pack
        iconsPackPackageName = packageName;
        packagesDrawables.clear();
        backImages.clear();
        cacheClear();

        if (iconsPackPackageName.equalsIgnoreCase("default")) {
            return ;
        }

        XmlPullParser xpp = null;

        try {
            iconPackres = pm.getResourcesForApplication(iconsPackPackageName);
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", iconsPackPackageName);
            if (appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            }

            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("iconback")) {
                            for(int i=0; i<xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmap(drawableName);
                                    if (iconback != null) {
                                        backImages.add(iconback);
                                    }
                                }
                            }
                        }
                        else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                maskImage = loadBitmap(drawableName);
                            }
                        }
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                frontImage = loadBitmap(drawableName);
                            }
                        }
                        else if (xpp.getName().equals("scale")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                factor = Float.valueOf(xpp.getAttributeValue(0));
                            }
                        }
                        if (xpp.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for(int i=0; i<xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                }
                                else if (xpp.getAttributeName(i).equals("drawable")) {
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
                return ((BitmapDrawable)bitmap).getBitmap();
            }
        }
        return null;
    }

    public Drawable getDrawableIconForPackage(ComponentName componentName) {
        try {
            if (!iconsPackPackageName.equalsIgnoreCase("default")) { //search for custom icons pack

                String drawable = packagesDrawables.get(componentName.toString());
                if (drawable != null) { //there is a custom icon
                    int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
                    if (id > 0) {
                        Drawable bitmap = iconPackres.getDrawable(id);
                        return bitmap;
                    }
                }

                //search first in cache
                Drawable systemIcon = cacheGetDrawable(componentName.toString());
                if (systemIcon != null)
                    return systemIcon;

                systemIcon= pm.getActivityIcon(componentName);
                if (systemIcon instanceof BitmapDrawable) {
                    Drawable generated = generateBitmap(componentName.toString(), systemIcon);
                    cacheStoreDrawable(componentName.toString(), generated);
                    return generated;
                }
                return systemIcon;
            }

            return pm.getActivityIcon(componentName);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to found component " + componentName.toString()+ e);
            return null;
        }
    }

    private Drawable generateBitmap(String appPackageName, Drawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

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

        // create a mutable mask bitmap with the same mask
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(((BitmapDrawable)defaultBitmap).getBitmap(), (int)(w * factor), (int)(h * factor), false);

        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(maskImage,0, 0, new Paint());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth())/2, (h - scaledBitmap.getHeight())/2, null);
            canvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);
        } else { // draw the scaled bitmap without mask        
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth())/2, (h - scaledBitmap.getHeight())/2, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(frontImage, 0, 0, null);
        }

        return new BitmapDrawable(iconPackres, result);
    }


    private void loadAvailableIconsPacks() {

        List<ResolveInfo> launcherthemes = pm.queryIntentActivities(new Intent("fr.neamar.kiss.THEMES"), PackageManager.GET_META_DATA);
        List<ResolveInfo> adwlauncherthemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

        launcherthemes.addAll(adwlauncherthemes);

        for(ResolveInfo ri  : launcherthemes) {
            String packageName = ri.activityInfo.packageName;
            ApplicationInfo ai = null;
            try {
                ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String name  = pm.getApplicationLabel(ai).toString();
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

    private boolean cacheStoreDrawable(String key, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            File drawableFile = cacheGetFileName(key);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable)drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable in cache " + e);
            } 
        }
        return false;
    }


    private Drawable cacheGetDrawable(String key) {

        File drawableFile = cacheGetFileName(key);
        if (!drawableFile.isFile()) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(drawableFile);
            BitmapDrawable drawable = new BitmapDrawable(BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    private File cacheGetFileName(String key) {
        File cacheDir = this.ctx.getCacheDir();
        File drawableFile = new File (cacheDir.getPath()+"/icon_" + iconsPackPackageName+ "_"+ key.hashCode() + ".png");

        return drawableFile;
    }

    private void cacheClear() {
        File cacheDir = this.ctx.getCacheDir();
        File[] icons = cacheDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith("icon_")) {
                    return true;
                }

                return false;
            }
        });

        for (File file : icons) { //delete cached icons
            file.delete();
        }
    }

}
