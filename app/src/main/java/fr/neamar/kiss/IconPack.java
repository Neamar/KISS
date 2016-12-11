package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class consists of a storage for an android icon pack, provides a way to load one
 * and to retrieve drawables from the icon pack
 * <p>
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 *
 * @author Antoine Bolvy (saveman71)
 * @author nmitsou
 */
class IconPack {

    private static final String TAG = "IconPack";

    // map with available icons packs
    private HashMap<String, String> iconsPacks = new HashMap<>();
    // map with available drawable for an icons pack
    private Map<String, String> packagesDrawables = new HashMap<>();
    // instance of a resource object of an icon pack
    private Resources iconPackRes;
    // package name of the icons pack
    public final String packageName;
    // list of back images available on an icons pack
    private List<Bitmap> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private Bitmap maskImage = null;
    // front image of an icons pack
    private Bitmap frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;

    // loaded status of the icon pack
    private boolean loaded = false;

    /**
     * Construct an icon pack
     *
     * @param packageName Android package ID of the package to parse
     */
    IconPack(Context context, String packageName) {
        this.packageName = packageName;
        this.loaded = parseIconPack(context, packageName);
    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    private boolean parseIconPack(Context context, String packageName) {
        XmlPullParser xpp = null;

        PackageManager pm = context.getPackageManager();

        try {
            // search appfilter.xml into icons pack apk resource folder
            iconPackRes = pm.getResourcesForApplication(packageName);
            int appfilterid = iconPackRes.getIdentifier("appfilter", "xml", packageName);
            if (appfilterid > 0) {
                xpp = iconPackRes.getXml(appfilterid);
            }

            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        //parse <iconback> xml tags used as background of generated icons
                        if (xpp.getName().equals("iconback")) {
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmapFromIconPack(drawableName);
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
                                maskImage = loadBitmapFromIconPack(drawableName);
                            }
                        }
                        //parse <iconupon> xml tags used as front image of generated icons
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                frontImage = loadBitmapFromIconPack(drawableName);
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
        }
        catch (Exception e) {
            Log.e(TAG, "Error parsing appfilter.xml " + e);
            return false;
        }
        return true;
    }

    /**
     * @param drawableIdentifier The identifier to look for in the iconpack's resources
     * @return The drawable if found, or null
     */
    @Nullable
    private BitmapDrawable loadDrawableFromIconPack(String drawableIdentifier) {
        int id = iconPackRes.getIdentifier(drawableIdentifier, "drawable", packageName);
        if (id > 0) {
            // noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
            Drawable icon = iconPackRes.getDrawable(id);
            if (icon instanceof BitmapDrawable) {
                return (BitmapDrawable) icon;
            }
        }
        return null;
    }

    /**
     * @param drawableIdentifier The identifier to look for in the iconpack's resources
     * @return The bitmap if found, or null
     */
    @Nullable
    private Bitmap loadBitmapFromIconPack(String drawableIdentifier) {
        BitmapDrawable drawable = loadDrawableFromIconPack(drawableIdentifier);
        return drawable != null ? drawable.getBitmap() : null;
    }

    /**
     * This method generates the icon pack's alternative icon from a random background image (backImages),
     * a mask image (maskImage), and a front image (e.g. for a glossy effect) (frontImage)
     *
     * @param defaultBitmap The icon to generate the bitmap from
     * @return The generated drawable, or the default one if the icon pack does not support
     * generating alternative icons
     * @see <a href="http://forum.xda-developers.com/showthread.php?t=1649891">[GUIDE] Apex Launcher Theme Tutorial</a>
     */
    private Drawable generateBitmap(Drawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

        // If for some reason the defaultBitmap is not a BitmapDrawable, we won't be able to process it,
        // so return it now
        if (!(defaultBitmap instanceof BitmapDrawable)) {
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
        }
        else { // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(frontImage, 0, 0, null);
        }

        return new BitmapDrawable(iconPackRes, result);
    }

    /**
     * Scan for installed icons packs
     */
    public static HashMap<String, String> loadAvailableIconsPacks(Context context) {
        HashMap<String, String> iconPacks = new HashMap<>();
        PackageManager pm = context.getPackageManager();

        List<ResolveInfo> launcherThemes = pm.queryIntentActivities(new Intent("fr.neamar.kiss.THEMES"), PackageManager.GET_META_DATA);
        List<ResolveInfo> ADWLauncherThemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

        launcherThemes.addAll(ADWLauncherThemes);

        for (ResolveInfo ri : launcherThemes) {
            String packageName = ri.activityInfo.packageName;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String name = pm.getApplicationLabel(ai).toString();
                iconPacks.put(packageName, name);
            }
            catch (PackageManager.NameNotFoundException e) {
                // shouldn't happen
                Log.e(TAG, "Unable to find package " + packageName + e);
            }
        }
        return iconPacks;
    }

    /**
     * Get or generate an icon from a ComponentName
     */
    public Drawable getIcon(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();

        String drawableIdentifier = packagesDrawables.get(componentName.toString());
        if (drawableIdentifier != null) {
            // There is a custom icon
            return loadDrawableFromIconPack(drawableIdentifier);
        }

        // Generate the alternative icon from the icon pack components
        return generateBitmap(pm.getActivityIcon(componentName));
    }

    public boolean isLoaded() {
        return loaded;
    }
}
