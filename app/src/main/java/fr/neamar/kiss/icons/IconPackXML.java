package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;

public class IconPackXML implements IconPack<IconPackXML.DrawableInfo> {
    protected static final String TAG = IconPackXML.class.getSimpleName();
    private final Map<String, Set<DrawableInfo>> drawablesByComponent = new HashMap<>(0);
    private final Map<String, DrawableInfo> drawableList = new HashMap<>(0);
    // instance of a resource object of an icon pack
    private Resources packResources;
    // package name of the icons pack
    private final String iconPackPackageName;
    // list of back images available on an icons pack
    private final List<Drawable> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private Drawable maskImage = null;
    // front image of an icons pack
    private Drawable frontImage = null;
    // scale factor of an icons pack
    private float scaleFactor = 1.0f;
    private volatile boolean loaded = false;
    private Utilities.AsyncRun<Drawable> mLoadTask = null;

    public IconPackXML(Context context, String packageName) {
        iconPackPackageName = packageName;
        // start async loading
        mLoadTask = Utilities.runAsync((task) -> {
            if (task == mLoadTask) {
                load(context.getPackageManager());
            }
            return null;
        }, (task, result) -> {
            if (!task.isCancelled() && task == mLoadTask) {
                mLoadTask = null;
            }
        });
    }

    public boolean isLoaded() {
        if (!loaded) {
            synchronized (this) {
                return loaded;
            }
        }
        return loaded;
    }

    private void load(PackageManager packageManager) {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    try {
                        packResources = packageManager.getResourcesForApplication(iconPackPackageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Unable to get icon pack resources: " + iconPackPackageName, e);
                    }
                    parseAppFilterXML();
                    loaded = true;
                }
            }
        }
    }

    public void loadDrawables(PackageManager packageManager) {
        load(packageManager);
        parseDrawableXML();
    }

    /**
     * @return true, if icon pack contains any images that can mask icons
     */
    public boolean hasMask() {
        return isLoaded() && (maskImage != null || !backImages.isEmpty() || frontImage != null);
    }

    @Override
    public Collection<DrawableInfo> getDrawableList() {
        return Collections.unmodifiableCollection(drawableList.values());
    }

    @Nullable
    private CalendarDrawable getCalendarDrawable(@Nullable String componentName) {
        Set<DrawableInfo> drawables = drawablesByComponent.get(componentName);
        if (drawables != null)
            for (DrawableInfo info : drawables)
                if (info instanceof CalendarDrawable)
                    return (CalendarDrawable) info;
        return null;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        String componentNameStr = componentName.toString();
        CalendarDrawable calendar = getCalendarDrawable(componentNameStr);
        if (calendar != null) {
            return getDrawable(calendar);
        }

        Set<DrawableInfo> drawables = drawablesByComponent.get(componentNameStr);
        if (drawables != null) {
            for (DrawableInfo info : drawables) {
                Drawable drawable = getDrawable(info);
                if (drawable != null) {
                    return drawable;
                }
            }
        }
        return null;
    }

    @Override
    public Drawable getDrawable(@NonNull DrawableInfo drawableInfo) {
        return drawableInfo.getDrawable(packResources, iconPackPackageName);
    }

    /**
     * Get {@link BitmapDrawable} from {@link Drawable}
     * Convert any drawable into a bitmap drawable with same size.
     *
     * @param icon any {@link Drawable}
     * @return a {@link BitmapDrawable}
     */
    private BitmapDrawable getBitmapDrawable(Drawable icon) {
        if (icon instanceof BitmapDrawable) {
            return (BitmapDrawable) icon;
        }

        Bitmap bitmap = DrawableUtils.drawableToBitmap(icon);
        return new BitmapDrawable(packResources, bitmap);
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside, @ColorInt int backgroundColor) {
        if (!loaded) {
            Log.w(TAG, "Icon Pack " + iconPackPackageName + " not yet loaded!");
        }
        BitmapDrawable defaultDrawable = getBitmapDrawable(icon);

        // initialize dimensions
        int w = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
        int h = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
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
            canvas.drawBitmap(defaultBitmap, getTransformationMatrix(defaultBitmap, scaledWidth, scaledHeight, (w - scaledWidth) / 2f, (h - scaledHeight) / 2f), null);
        } else {
            canvas.drawBitmap(defaultBitmap, getResizeMatrix(defaultBitmap, w, h), null);
        }

        // mask the scaled bitmap
        if (maskImage != null) {
            Bitmap maskBitmap = DrawableUtils.drawableToBitmap(maskImage);
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

            Bitmap backImageBitmap = DrawableUtils.drawableToBitmap(backImages.get(backImageInd));
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            canvas.drawBitmap(backImageBitmap, getResizeMatrix(backImageBitmap, w, h), paint);
            paint.setXfermode(null);
        }

        // draw the front
        if (frontImage != null) {
            Bitmap frontImageBitmap = DrawableUtils.drawableToBitmap(frontImage);
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
        synchronized (this) {
            if (packResources == null)
                return;

            XmlPullParser xpp = null;
            // search drawable.xml into icons pack apk resource folder
            int drawableXmlId = getIdentifier("drawable", "xml");
            if (drawableXmlId != 0) {
                xpp = packResources.getXml(drawableXmlId);
            }
            if (xpp == null)
                return;

            long start = System.currentTimeMillis();
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
                                        if (!drawableList.containsKey(drawableName)) {
                                            DrawableInfo drawableInfo = new SimpleDrawable(drawableName);
                                            drawableList.put(drawableName, drawableInfo);
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

            long end = System.currentTimeMillis();
            Log.i(TAG, (end - start) + " milliseconds to parse drawable.xml");
        }
    }

    /**
     * See {@link Resources#getIdentifier(String, String, String)}
     */
    private int getIdentifier(String name, String defType) {
        return packResources.getIdentifier(name, defType, iconPackPackageName);
    }

    private void parseAppFilterXML() {
        if (packResources == null)
            return;

        long start = System.currentTimeMillis();

        Map<String, CalendarDrawable> calendarDrawablesByPrefix = new HashMap<>(0);
        try {
            XmlPullParser xpp = findAppFilterXml();
            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        //parse <iconback> xml tags used as background of generated icons
                        if (xpp.getName().equals("iconback")) {
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Drawable drawable = getDrawable(new SimpleDrawable(drawableName));
                                    if (drawable != null) {
                                        backImages.add(drawable);
                                    }
                                }
                            }
                        }
                        //parse <iconmask> xml tags used as mask of generated icons
                        else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                Drawable drawable = getDrawable(new SimpleDrawable(drawableName));
                                if (drawable != null) {
                                    maskImage = drawable;
                                }
                            }
                        }
                        //parse <iconupon> xml tags used as front image of generated icons
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                Drawable drawable = getDrawable(new SimpleDrawable(drawableName));
                                if (drawable != null) {
                                    frontImage = drawable;
                                }
                            }
                        }
                        //parse <scale> xml tags used as scale factor of original bitmap icon
                        else if (xpp.getName().equals("scale")) {
                            String factor = xpp.getAttributeValue(null, "factor");
                            if (factor == null && xpp.getAttributeCount() > 0) {
                                factor = xpp.getAttributeValue(0);
                            }
                            if (factor != null) {
                                try {
                                    scaleFactor = Float.parseFloat(factor);
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        //parse <item> xml tags for custom icons
                        else if (xpp.getName().equals("item")) {
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
                            if (!drawableList.containsKey(drawableName)) {
                                DrawableInfo drawableInfo = new SimpleDrawable(drawableName);
                                drawableList.put(drawableName, drawableInfo);
                            }

                            if (componentName != null) {
                                Set<DrawableInfo> infoSet = drawablesByComponent.get(componentName);
                                if (infoSet == null)
                                    drawablesByComponent.put(componentName, infoSet = new HashSet<>(1));
                                infoSet.add(drawableList.get(drawableName));
                            } else {
                                Log.w(TAG, "Drawable `" + drawableName + "` for component `null` not found");
                            }
                        }
                        //parse <calendar>
                        else if (xpp.getName().equals("calendar")) {
                            String componentName = null;
                            String prefix = null;

                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                } else if (xpp.getAttributeName(i).equals("prefix")) {
                                    prefix = xpp.getAttributeValue(i);
                                }
                            }

                            if (componentName != null && prefix != null) {
                                if (!calendarDrawablesByPrefix.containsKey(prefix)) {
                                    CalendarDrawable calendarDrawable = new CalendarDrawable(prefix);
                                    calendarDrawablesByPrefix.put(prefix, calendarDrawable);
                                }

                                CalendarDrawable drawableInfo = calendarDrawablesByPrefix.get(prefix);
                                Set<DrawableInfo> infoSet = drawablesByComponent.get(componentName);
                                if (infoSet == null)
                                    drawablesByComponent.put(componentName, infoSet = new HashSet<>(1));
                                infoSet.add(drawableInfo);
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error parsing appfilter.xml ", e);
        }

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to parse appfilter.xml");
    }

    private XmlPullParser findAppFilterXml() throws XmlPullParserException {
        // search appfilter.xml in icon pack's apk resource folder for xml files
        int appFilterIdXml = getIdentifier("appfilter", "xml");
        if (appFilterIdXml != 0) {
            return packResources.getXml(appFilterIdXml);
        }

        // search appfilter.xml in icon pack's apk resource folder for raw files (supporting icon pack studio)
        int appFilterIdRaw = getIdentifier("appfilter", "raw");
        if (appFilterIdRaw != 0) {
            InputStream input = packResources.openRawResource(appFilterIdRaw);
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = xppf.newPullParser();
            xpp.setInput(input, "UTF-8");
            return xpp;
        }
        return null;
    }

    @NonNull
    @Override
    public String getPackPackageName() {
        return iconPackPackageName;
    }


    public static abstract class DrawableInfo {
        private final String drawableName;

        protected DrawableInfo(@NonNull String drawableName) {
            this.drawableName = drawableName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DrawableInfo)) return false;
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

        /**
         * resolve given drawable name to a drawable id and cache it for later reuse
         *
         * @param resources
         * @param iconPackPackageName
         * @return drawable id
         */
        @DrawableRes
        private int getDrawableId(@NonNull Resources resources, @NonNull String iconPackPackageName) {
            Integer drawableId = getCachedDrawableId();
            if (drawableId == null) {
                String drawableName = getDrawableName();
                if (!TextUtils.isEmpty(drawableName)) {
                    drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPackageName);
                    if (drawableId == 0) {
                        Log.w(TAG, "Unable to load resource id for: " + getDrawableName());
                    }
                } else {
                    drawableId = 0;
                }
                cacheDrawableId(drawableName, drawableId);
            }
            return drawableId;
        }

        protected abstract void cacheDrawableId(String drawableName, Integer drawableId);

        protected abstract Integer getCachedDrawableId();

        @Nullable
        public Drawable getDrawable(@NonNull Resources resources, @NonNull String iconPackPackageName) {
            try {
                int drawableId = getDrawableId(resources, iconPackPackageName);
                if (drawableId != 0) {
                    return ResourcesCompat.getDrawable(resources, drawableId, null);
                }
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Unable to load resource for: " + getDrawableName(), e);
            }
            return null;
        }
    }

    public static class SimpleDrawable extends DrawableInfo {

        @DrawableRes
        private Integer drawableId;

        public SimpleDrawable(@NonNull String drawableName) {
            super(drawableName);
        }

        @Override
        protected void cacheDrawableId(String drawableName, Integer drawableId) {
            this.drawableId = drawableId;
        }

        @Override
        protected Integer getCachedDrawableId() {
            return drawableId;
        }
    }

    public static class CalendarDrawable extends DrawableInfo {

        private final Map<String, Integer> drawableIds = new HashMap<>(31);

        protected CalendarDrawable(@NonNull String drawableNamePrefix) {
            super(drawableNamePrefix);
        }

        /**
         * Builds drawable name for day of month: using prefix and adding current day
         *
         * @return drawable name
         */
        @Override
        public String getDrawableName() {
            int dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            return super.getDrawableName() + dayOfMonth;
        }

        @Override
        protected void cacheDrawableId(String drawableName, Integer drawableId) {
            drawableIds.put(drawableName, drawableId);
        }

        @Override
        protected Integer getCachedDrawableId() {
            String drawableName = getDrawableName();
            return drawableIds.get(drawableName);
        }
    }
}
