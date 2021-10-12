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
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import fr.neamar.kiss.R;
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
    private float scaleFactor = 1.0f;
    private boolean loaded = false;


    public IconPackXML(String packageName) {
        iconPackPackageName = packageName;
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized void load(PackageManager packageManager) {
        if (loaded)
            return;
        try {
            packResources = packageManager.getResourcesForApplication(iconPackPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "get icon pack resources" + iconPackPackageName, e);
        }

        parseAppFilterXML();
        loaded = true;
    }

    public synchronized void loadDrawables(PackageManager packageManager) {
        load(packageManager);
        parseDrawableXML();
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
            return generateBitmap((BitmapDrawable) systemIcon, ctx);
        }

        Bitmap bitmap;
        if (systemIcon.getIntrinsicWidth() <= 0 || systemIcon.getIntrinsicHeight() <= 0)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        else
            bitmap = Bitmap.createBitmap(systemIcon.getIntrinsicWidth(), systemIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        systemIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        systemIcon.draw(new Canvas(bitmap));
        return generateBitmap(new BitmapDrawable(ctx.getResources(), bitmap), ctx);
    }

    @NonNull
    private BitmapDrawable generateBitmap(@NonNull BitmapDrawable defaultDrawable, @NonNull Context ctx) {
        // initialize dimensions
        int w = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
        int h = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        float sanitizedScaleFactor = scaleFactor;
        if (maskImage == null && backImages.isEmpty() && frontImage == null) {
            // fall back to rescaling only if necessary
            sanitizedScaleFactor = 1.0f;
        }

        // draw scaled icon
        int scaledWidth = (int) (w * sanitizedScaleFactor);
        int scaledHeight = (int) (h * sanitizedScaleFactor);
        Bitmap defaultBitmap = defaultDrawable.getBitmap();
        if (scaledWidth != w || scaledHeight != h) {
            canvas.drawBitmap(defaultBitmap, getTransformationMatrix(defaultBitmap, w, h, (w - scaledWidth) / 2f, (h - scaledHeight) / 2f), null);
        } else {
            canvas.drawBitmap(defaultBitmap, getResizeMatrix(defaultBitmap, w, h), null);
        }

        // mask the scaled bitmap
        if (maskImage != null) {
            Bitmap maskBitmap = getBitmap(maskImage);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(maskBitmap, getResizeMatrix(maskBitmap, w, h), paint);
            paint.setXfermode(null);
        }

        // draw the background
        if (!backImages.isEmpty()) {
            // select a random background image
            Random r = new Random();
            int backImageInd = r.nextInt(backImages.size());

            Bitmap backImageBitmap = getBitmap(backImages.get(backImageInd));
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            canvas.drawBitmap(backImageBitmap, getResizeMatrix(backImageBitmap, w, h), paint);
            paint.setXfermode(null);
        }

        // draw the front
        if (frontImage != null) {
            Bitmap frontImageBitmap = getBitmap(frontImage);
            Paint paint = new Paint();
            canvas.drawBitmap(frontImageBitmap, getResizeMatrix(frontImageBitmap, w, h), paint);
        }

        return new BitmapDrawable(packResources, result);
    }

    /**
     * @param bitmap    bitmap to draw
     * @param newWidth  new width of bitmap
     * @param newHeight new height of bitmap
     * @return resize matrix
     */
    private Matrix getResizeMatrix(Bitmap bitmap, int newWidth, int newHeight) {
        return getTransformationMatrix(bitmap, newWidth, newHeight, 0f, 0f);
    }

    /**
     * @param bitmap     bitmap to draw
     * @param newWidth   new width of bitmap
     * @param newHeight  new height of bitmap
     * @param offsetLeft left offset of bitmap
     * @param offsetTop  right offset of bitmap
     * @return transformation matrix
     */
    private Matrix getTransformationMatrix(Bitmap bitmap, int newWidth, int newHeight, float offsetLeft, float offsetTop) {
        float scaleWidth = ((float) newWidth) / bitmap.getWidth();
        float scaleHeight = ((float) newHeight) / bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        matrix.postTranslate(offsetLeft, offsetTop);
        return matrix;
    }

    private void parseDrawableXML() {
        if (packResources == null)
            return;

        XmlPullParser xpp = null;
        // search drawable.xml into icons pack apk resource folder
        int drawableXmlId = packResources.getIdentifier("drawable", "xml", iconPackPackageName);
        if (drawableXmlId > 0) {
            xpp = packResources.getXml(drawableXmlId);
        }
        if (xpp == null)
            return;
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case "item":
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if (attrName.equals("drawable")) {
                                    String drawableName = xpp.getAttributeValue(attrIdx);
                                    int drawableId = packResources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                                    if (drawableId != 0) {
                                        DrawableInfo drawableInfo = new DrawableInfo(drawableName, drawableId);
                                        drawableList.add(drawableInfo);
                                    }
                                }
                            }
                            break;
                        case "category":
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing drawable.xml", e);
        }

    }

    private void parseAppFilterXML() {
        if (packResources == null)
            return;

        XmlPullParser xpp = null;
        try {
            // search appfilter.xml into icons pack apk resource folder
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
                            scaleFactor = Float.parseFloat(xpp.getAttributeValue(0));
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

                            if (drawableName == null) {
                                eventType = xpp.next();
                                continue;
                            }
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
