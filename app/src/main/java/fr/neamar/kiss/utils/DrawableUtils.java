package fr.neamar.kiss.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import fr.neamar.kiss.UIColors;

public class DrawableUtils {

    private static final Paint PAINT = new Paint();
    private static final Path SHAPE_PATH = new Path();
    private static final RectF RECT_F = new RectF();
    public static final String KEY_THEMED_ICONS = "themed-icons";
    private static final String TAG = DrawableUtils.class.getSimpleName();
    private static final IconShape[] TEARDROP_SHAPES = {IconShape.SHAPE_TEARDROP_BR, IconShape.SHAPE_TEARDROP_BL, IconShape.SHAPE_TEARDROP_TL, IconShape.SHAPE_TEARDROP_TR};
    private static final ColorFilter DISABLED_COLOR_FILTER;

    static {
        // initialize color filter for disabled icons
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        DISABLED_COLOR_FILTER = new ColorMatrixColorFilter(matrix);
    }

    public static int dpToPx(@NonNull Context ctx, float valueInDp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, ctx.getResources().getDisplayMetrics());
    }


    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = createCanvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Get percent of icon to use as margin. We use this to avoid clipping the image.
     *
     * @param shape from SHAPE_*
     * @return margin size
     */
    private static float getScaleToFit(IconShape shape) {
        switch (shape) {
            case SHAPE_SYSTEM:
            case SHAPE_CIRCLE:
            case SHAPE_TEARDROP_BR:
            case SHAPE_TEARDROP_BL:
            case SHAPE_TEARDROP_TL:
            case SHAPE_TEARDROP_TR:
                return 0.2071f;  // (sqrt(2)-1)/2 to make a square fit in a circle
            case SHAPE_SQUIRCLE:
                return 0.1f;
            case SHAPE_ROUND_RECT:
                return 0.05f;
            case SHAPE_HEXAGON:
                return 0.26f;
            case SHAPE_OCTAGON:
                return 0.25f;
            default:
                return 0f;
        }
    }

    /**
     * Handle adaptive icons for compatible devices
     *
     * @param ctx             {@link Context}
     * @param icon            the {@link Drawable} to shape
     * @param shape           shape to use
     * @param fitInside       for non {@link AdaptiveIconDrawable}, the icon is resized so it fits in shape
     * @param backgroundColor color used as background for non {@link AdaptiveIconDrawable}
     * @return shaped icon
     */
    @NonNull
    public static Drawable applyIconMaskShape(@NonNull Context ctx, @NonNull Drawable icon, @NonNull IconShape shape, boolean fitInside, @ColorInt int backgroundColor) {
        if (shape == IconShape.SHAPE_SYSTEM && !hasDeviceConfiguredMask()) {
            // if no icon mask can be configured for device, then use icon as is
            return icon;
        }

        Bitmap outputBitmap;
        Canvas outputCanvas;

        int maxIconSize = getMaxIconSize(ctx, icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAdaptiveIconDrawable(icon)) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
            Drawable bgDrawable = adaptiveIcon.getBackground();
            Drawable fgDrawable = adaptiveIcon.getForeground();

            int layerSize = (int) (maxIconSize * (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
            int layerOffset = (layerSize - maxIconSize) / 2;

            outputBitmap = Bitmap.createBitmap(maxIconSize, maxIconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = createCanvas(outputBitmap);

            setIconShapeAndDrawBackground(outputCanvas, backgroundColor, shape, false, icon.hashCode());

            // Stretch adaptive layers because they are 108dp and the icon size is 48dp
            if (bgDrawable != null) {
                bgDrawable.setBounds(-layerOffset, -layerOffset, maxIconSize + layerOffset, maxIconSize + layerOffset);
                bgDrawable.draw(outputCanvas);
            }

            if (fgDrawable != null) {
                fgDrawable.setBounds(-layerOffset, -layerOffset, maxIconSize + layerOffset, maxIconSize + layerOffset);
                fgDrawable.draw(outputCanvas);
            }
        }
        // If icon is not adaptive, put it in a colored canvas to make it have a unified shape
        else {
            float marginPercent = 0;
            if (fitInside) {
                marginPercent = getScaleToFit(shape);
            }
            Rect bounds = getIconBounds(icon, maxIconSize, marginPercent);

            outputBitmap = Bitmap.createBitmap(maxIconSize, maxIconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = createCanvas(outputBitmap);

            setIconShapeAndDrawBackground(outputCanvas, backgroundColor, shape, true, icon.hashCode());

            // Shrink icon so that it fits the shape
            icon.setBounds(bounds);
            icon.draw(outputCanvas);
        }
        return new BitmapDrawable(ctx.getResources(), outputBitmap);
    }

    private static int getMaxIconSize(Context ctx, Drawable icon) {
        int maxIconSize = getMaxIconSize(ctx);
        int maxIntrinsicSize = Math.max(icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        if (maxIntrinsicSize > 0 && maxIntrinsicSize < maxIconSize) {
            return maxIntrinsicSize;
        }
        return maxIconSize;
    }

    private static int getMaxIconSize(Context ctx) {
        return dpToPx(ctx, 96);
    }

    private static Rect getIconBounds(Drawable icon, int maxIconSize, float marginPercent) {
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();

        int margin = Math.round(maxIconSize * marginPercent);

        float aspect = (w > 0 && h > 0) ? (w / (float) h) : 1f;
        if (h > w) {
            h = maxIconSize - margin;
            w = Math.round(h * aspect);
        } else {
            w = maxIconSize - margin;
            h = Math.round(w / aspect);
        }

        int offsetWidth = (maxIconSize - w) / 2;
        int offsetHeight = (maxIconSize - h) / 2;
        return new Rect(offsetWidth, offsetHeight, w + offsetWidth, h + offsetHeight);
    }

    private static Canvas createCanvas(Bitmap bitmap) {
        Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        canvas.setBitmap(bitmap);
        return canvas;
    }

    /**
     * Set the shape of icons and draws background.
     * Synchronized because class fields like {@link DrawableUtils#SHAPE_PATH}, {@link DrawableUtils#RECT_F} and {@link DrawableUtils#PAINT} are reused for every call, which may result in unexpected behaviour if method is called from different threads running in parallel.
     *
     * @param shape type of shape: DrawableUtils.SHAPE_*
     * @param hash, for pseudo random shape if applicable
     */
    private synchronized static void setIconShapeAndDrawBackground(Canvas canvas, @ColorInt int backgroundColor, @NonNull IconShape shape, boolean drawBackground, int hash) {
        shape = getFinalShape(shape, hash);

        int iconSize = canvas.getHeight();
        final Path path = SHAPE_PATH;
        path.rewind();

        switch (shape) {
            case SHAPE_SYSTEM: {
                if (hasDeviceConfiguredMask()) {
                    // get icon mask for device
                    AdaptiveIconDrawable drawable = new AdaptiveIconDrawable(new ColorDrawable(Color.BLACK), new ColorDrawable(Color.BLACK));
                    drawable.setBounds(0, 0, iconSize, iconSize);
                    path.set(drawable.getIconMask());
                } else {
                    // This should never happen, use rect so nothing is clipped
                    path.addRect(0f, 0f, iconSize, iconSize, Path.Direction.CCW);
                }
                break;
            }
            case SHAPE_CIRCLE: {
                int radius = iconSize / 2;
                path.addCircle(radius, radius, radius, Path.Direction.CCW);
                break;
            }
            case SHAPE_SQUIRCLE: {
                int h = iconSize / 2;
                float c = iconSize / 2.333f;
                path.moveTo(h, 0f);
                path.cubicTo(h + c, 0, iconSize, h - c, iconSize, h);
                path.cubicTo(iconSize, h + c, h + c, iconSize, h, iconSize);
                path.cubicTo(h - c, iconSize, 0, h + c, 0, h);
                path.cubicTo(0, h - c, h - c, 0, h, 0);
                path.close();
                break;
            }
            case SHAPE_SQUARE:
                path.addRect(0f, 0f, iconSize, iconSize, Path.Direction.CCW);
                break;
            case SHAPE_ROUND_RECT:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addRoundRect(RECT_F, iconSize / 8f, iconSize / 8f, Path.Direction.CCW);
                break;
            case SHAPE_TEARDROP_RND: // this is handled before we get here
            case SHAPE_TEARDROP_BR:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 90, 270);
                path.lineTo(iconSize, iconSize * 0.70f);
                RECT_F.set(iconSize * 0.70f, iconSize * 0.70f, iconSize, iconSize);
                path.arcTo(RECT_F, 0, 90, false);
                path.close();
                break;
            case SHAPE_TEARDROP_BL:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 180, 270);
                path.lineTo(iconSize * .3f, iconSize);
                RECT_F.set(0f, iconSize * .7f, iconSize * .3f, iconSize);
                path.arcTo(RECT_F, 90, 90, false);
                path.close();
                break;
            case SHAPE_TEARDROP_TL:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 270, 270);
                path.lineTo(0, iconSize * .3f);
                RECT_F.set(0f, 0f, iconSize * .3f, iconSize * .3f);
                path.arcTo(RECT_F, 180, 90, false);
                path.close();
                break;
            case SHAPE_TEARDROP_TR:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 0, 270);
                path.lineTo(iconSize * .7f, 0f);
                RECT_F.set(iconSize * .7f, 0f, iconSize, iconSize * .3f);
                path.arcTo(RECT_F, 270, 90, false);
                path.close();
                break;
            case SHAPE_HEXAGON:
                for (int deg = 0; deg < 360; deg += 60) {
                    float x = ((float) Math.cos(Math.toRadians(deg)) * .5f + .5f) * iconSize;
                    float y = ((float) Math.sin(Math.toRadians(deg)) * .5f + .5f) * iconSize;
                    if (deg == 0)
                        path.moveTo(x, y);
                    else
                        path.lineTo(x, y);
                }
                path.close();
                break;
            case SHAPE_OCTAGON:
                for (int deg = 22; deg < 360; deg += 45) {
                    float x = ((float) Math.cos(Math.toRadians(deg + .5)) * .5f + .5f) * iconSize;
                    float y = ((float) Math.sin(Math.toRadians(deg + .5)) * .5f + .5f) * iconSize;

                    // scale it up to fill the rectangle
                    x = x * 1.0824f - x * 0.0824f;
                    y = y * 1.0824f - y * 0.0824f;

                    if (deg == 22)
                        path.moveTo(x, y);
                    else
                        path.lineTo(x, y);
                }
                path.close();
                break;
        }
        // draw background if applicable
        if (drawBackground && backgroundColor != Color.TRANSPARENT) {
            final Paint paint = PAINT;
            paint.reset();
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(backgroundColor);
            canvas.drawPath(path, paint);
        }
        // make sure we don't draw outside the shape
        canvas.clipPath(path);
    }

    public static boolean isAdaptiveIconDrawable(Drawable drawable) {
        if (hasDeviceConfiguredMask()) {
            return drawable instanceof AdaptiveIconDrawable;
        }
        return false;
    }

    public static boolean hasDeviceConfiguredMask() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static boolean hasThemedIcons() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /**
     * Get themed drawable if applicable
     *
     * @param ctx
     * @param drawable
     * @return themed drawable
     */
    public static Drawable getThemedDrawable(@NonNull Context ctx, @NonNull Drawable drawable) {
        if (isAdaptiveIconDrawable(drawable) &&
                hasThemedIcons() &&
                isThemedIconEnabled(ctx)) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) drawable.mutate();
            Drawable mono = aid.getMonochrome();
            if (mono != null) {
                int[] colors = UIColors.getIconColors(ctx);
                mono = mono.mutate();
                mono.setTint(colors[1]);
                return new AdaptiveIconDrawable(new ColorDrawable(colors[0]), mono);
            }
        }

        return drawable;
    }

    public static boolean isThemedIconEnabled(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(KEY_THEMED_ICONS, false);
    }

    @NonNull
    public synchronized static Drawable generateBackgroundDrawable(@NonNull Context ctx, @ColorInt int backgroundColor, @NonNull IconShape shape) {
        Bitmap bitmap = generateBackgroundBitmap(ctx, backgroundColor, shape, backgroundColor);
        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    @NonNull
    public static Drawable generateCodepointDrawable(@NonNull Context ctx, int codepoint, @ColorInt int textColor, @ColorInt int backgroundColor, @NonNull IconShape shape) {
        int iconSize = getMaxIconSize(ctx);

        Bitmap bitmap = generateBackgroundBitmap(ctx, backgroundColor, shape, codepoint);
        // create a canvas from a bitmap
        Canvas canvas = new Canvas(bitmap);

        // use StaticLayout to draw the text centered
        TextPaint paint = new TextPaint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(.6f * iconSize);

        RectF rectF = new RectF(0, 0, iconSize, iconSize);

        String glyph = new String(Character.toChars(codepoint));
        // If the codepoint glyph is an image we can't use SRC_IN to draw it.
        boolean drawAsHole = true;
        Character.UnicodeBlock block = null;
        try {
            block = Character.UnicodeBlock.of(codepoint);
        } catch (IllegalArgumentException ignored) {
        }
        if (block == null)
            drawAsHole = false;
        else {
            String blockString = block.toString();
            if (Character.UnicodeBlock.DINGBATS.toString().equals(blockString) ||
                    "EMOTICONS".equals(blockString) ||
                    Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS.toString().equals(blockString) ||
                    "MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS".equals(blockString) ||
                    "SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS".equals(blockString) ||
                    "TRANSPORT_AND_MAP_SYMBOLS".equals(blockString))
                drawAsHole = false;
            else if (!Character.UnicodeBlock.BASIC_LATIN.toString().equals(blockString)) {
                // log untested glyphs
                Log.d(TAG, "Codepoint " + codepoint + " with glyph " + glyph + " is in block " + block);
            }
        }
        // we can't draw images (emoticons and symbols) using SRC_IN with transparent color, the result is a square
        if (drawAsHole) {
            // write text with "transparent" (create a hole in the background)
            paint.setColor(Color.TRANSPARENT);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        } else {
            paint.setColor(textColor);
        }

        // draw the letter in the center
        Rect b = new Rect();
        paint.getTextBounds(glyph, 0, glyph.length(), b);
        canvas.drawText(glyph, 0, glyph.length(), iconSize / 2.f - b.centerX(), iconSize / 2.f - b.centerY(), paint);

        rectF.set(b);
        rectF.offset(iconSize / 2.f - rectF.centerX(), iconSize / 2.f - rectF.centerY());
        // pad the rectF so we don't touch the letter
        rectF.inset(rectF.width() * -.3f, rectF.height() * -.4f);

        // stroke a rect with the bounding of the letter
        if (drawAsHole) {
            paint.setColor(Color.TRANSPARENT);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ctx.getResources().getDisplayMetrics().density);
            canvas.drawRoundRect(rectF, rectF.width() / 2.4f, rectF.height() / 2.4f, paint);
        }
        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    @NonNull
    private synchronized static Bitmap generateBackgroundBitmap(@NonNull Context ctx, @ColorInt int backgroundColor, @NonNull IconShape shape, int hash) {
        int iconSize = getMaxIconSize(ctx);
        // create a canvas from a bitmap
        Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        setIconShapeAndDrawBackground(canvas, backgroundColor, shape, true, hash);

        return bitmap;
    }

    public static IconShape getFinalShape(IconShape shape, int hash) {
        switch (shape) {
            case SHAPE_SYSTEM:
                if (!hasDeviceConfiguredMask()) {
                    return IconShape.SHAPE_CIRCLE;
                }
                return shape;
            case SHAPE_TEARDROP_RND:
                return TEARDROP_SHAPES[Math.abs(hash % 4)];
            default:
                return shape;
        }
    }

    public static void setDisabled(Drawable drawable, boolean disabled) {
        if (drawable != null) {
            if (disabled) {
                drawable.setColorFilter(DISABLED_COLOR_FILTER);
            } else {
                drawable.clearColorFilter();
            }
        }
    }

}
