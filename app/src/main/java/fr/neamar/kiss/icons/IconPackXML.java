package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;

public class IconPackXML implements IconPack<IconPackXML.DrawableInfo> {
    private final static String TAG = IconPackXML.class.getSimpleName();
    private final Map<String, HashSet<DrawableInfo>> drawablesByComponent = new HashMap<>(0);
    private final HashSet<DrawableInfo> drawableList = new HashSet<>(0);
    // instance of a resource object of an icon pack
    private Resources packResources;
    // package name of the icons pack
    private final String iconPackPackageName;
    // list of back images available on an icons pack
    private final ArrayList<DrawableInfo> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private DrawableInfo maskImage = null;
    // front image of an icons pack
    private DrawableInfo frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;
    private boolean loaded = false;


    public IconPackXML(String packageName) {
        iconPackPackageName = packageName;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void load(PackageManager packageManager) {
        parseXML(packageManager);
        loaded = true;
    }

    public boolean hasMask() {
        return maskImage != null;
    }

    @Override
    public Collection<DrawableInfo> getDrawableList() {
        return Collections.unmodifiableCollection(drawableList);
    }

    @Nullable
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        return getComponentDrawable(componentName.toString());
    }

    @Nullable
    public Drawable getComponentDrawable(String componentName) {
        HashSet<DrawableInfo> drawables = drawablesByComponent.get(componentName);
        if (drawables == null || drawables.isEmpty())
            return null;
        DrawableInfo drawableInfo = drawables.iterator().next();
        return drawableInfo != null ? getDrawable(drawableInfo) : null;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull DrawableInfo drawableInfo) {
        try {
            return packResources.getDrawable(drawableInfo.drawableId);
        } catch (Resources.NotFoundException ignored) {
        }
        return null;
    }

    @NonNull
    private Bitmap getBitmap(@NonNull DrawableInfo drawableInfo) {
        Drawable drawable = getDrawable(drawableInfo);
        if (drawable == null)
            drawable = new ColorDrawable(Color.WHITE);
        return DrawableUtils.drawableToBitmap(drawable);
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable systemIcon, boolean fitInside) {
        if (systemIcon instanceof BitmapDrawable) {
            return generateBitmap((BitmapDrawable) systemIcon);
        }

        Bitmap bitmap;
        if (systemIcon.getIntrinsicWidth() <= 0 || systemIcon.getIntrinsicHeight() <= 0)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        else
            bitmap = Bitmap.createBitmap(systemIcon.getIntrinsicWidth(), systemIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        systemIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        systemIcon.draw(new Canvas(bitmap));
        return generateBitmap(new BitmapDrawable(ctx.getResources(), bitmap));
    }

    @NonNull
    private BitmapDrawable generateBitmap(@NonNull BitmapDrawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

        // select a random background image
        Random r = new Random();
        int backImageInd = r.nextInt(backImages.size());
        Bitmap backImage = getBitmap(backImages.get(backImageInd));
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

        float offsetLeft = (w - scaledBitmap.getWidth()) / 2f;
        float offsetTop = (h - scaledBitmap.getHeight()) / 2f;
        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mask = getBitmap(maskImage);
            Matrix matScale = new Matrix();
            matScale.setScale(w / (float) mask.getWidth(), h / (float) mask.getHeight());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, offsetLeft, offsetTop, null);
            canvas.drawBitmap(mask, matScale, paint);
            paint.setXfermode(null);
        } else {
            // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, offsetLeft, offsetTop, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(getBitmap(frontImage), 0, 0, null);
        }

        return new BitmapDrawable(packResources, result);
    }

    private void parseXML(PackageManager pm) {
        XmlPullParser xpp = null;

        try {
            // search appfilter.xml into icons pack apk resource folder
            packResources = pm.getResourcesForApplication(iconPackPackageName);
            int appfilterid = packResources.getIdentifier("appfilter", "xml", iconPackPackageName);
            if (appfilterid > 0) {
                xpp = packResources.getXml(appfilterid);
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
                                    int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                    if (drawableId != 0)
                                        backImages.add(new DrawableInfo(drawableName, drawableId));
                                }
                            }
                        }
                        //parse <iconmask> xml tags used as mask of generated icons
                        else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                if (drawableId != 0)
                                    maskImage = new DrawableInfo(drawableName, drawableId);
                            }
                        }
                        //parse <iconupon> xml tags used as front image of generated icons
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                if (drawableId != 0)
                                    frontImage = new DrawableInfo(drawableName, drawableId);
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

                            if (drawableName == null)
                                continue;
                            int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                            if (drawableId != 0) {
                                DrawableInfo drawableInfo = new DrawableInfo(drawableName, drawableId);
                                drawableList.add(drawableInfo);
                                if (componentName != null) {
                                    HashSet<DrawableInfo> infoSet = drawablesByComponent.get(componentName);
                                    if (infoSet == null)
                                        drawablesByComponent.put(componentName, infoSet = new HashSet<>(1));
                                    infoSet.add(drawableInfo);
                                }
                            } else {
                                if (componentName == null)
                                    componentName = "`null`";
                                Log.w(TAG, "Drawable `" + drawableName + "` for " + componentName + " not found");
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


    @NonNull
    @Override
    public String getPackPackageName() {
        return iconPackPackageName;
    }


    public static final class DrawableInfo {
        final String drawableName;
        final int drawableId;

        DrawableInfo(@NonNull String drawableName, int drawableId) {
            this.drawableName = drawableName;
            this.drawableId = drawableId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DrawableInfo that = (DrawableInfo) o;
            return drawableName.equals(that.drawableName);
        }

        @Override
        public int hashCode() {
            return drawableName.hashCode();
        }

        public String getDrawableName() {
            return drawableName;
        }
    }


}
