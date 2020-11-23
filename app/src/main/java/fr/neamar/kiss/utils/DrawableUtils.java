package fr.neamar.kiss.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.Shader.TileMode;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import fr.neamar.kiss.R;

public class DrawableUtils {

    public static final int SHAPE_SYSTEM = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_SQUARE = 2;
    public static final int SHAPE_SQUIRCLE = 3;
    public static final int SHAPE_ROUND_RECT = 4;
    private static final int SHAPE_TEARDROP_BR = 5;
    private static final int SHAPE_TEARDROP_BL = 6;
    private static final int SHAPE_TEARDROP_TL = 7;
    private static final int SHAPE_TEARDROP_TR = 8;
    private static final int SHAPE_TEARDROP_RND = 9;
    public static final int SHAPE_HEXAGON = 10;
    public static final int SHAPE_OCTAGON = 11;

    public static final int[] SHAPE_LIST = {
            SHAPE_SYSTEM,
            SHAPE_CIRCLE,
            SHAPE_SQUARE,
            SHAPE_SQUIRCLE,
            SHAPE_ROUND_RECT,
            SHAPE_TEARDROP_BR,
            SHAPE_TEARDROP_BL,
            SHAPE_TEARDROP_TL,
            SHAPE_TEARDROP_TR,
            SHAPE_HEXAGON,
            SHAPE_OCTAGON,
    };

    private static final Paint PAINT = new Paint();
    private static final Path SHAPE_PATH = new Path();
    private static final RectF RECT_F = new RectF();

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable applyIconMaskShape(Context ctx, Drawable icon, int shape, boolean fitInside) {
        return applyIconMaskShape(ctx, icon, shape, fitInside, Color.WHITE);
    }

    public static Drawable applyIconMaskShape(Context ctx, Drawable icon, int shape, @ColorInt int backgroundColor) {
        return applyIconMaskShape(ctx, icon, shape, false, backgroundColor);
    }

    /**
     * Get percent of icon to use as margin. We use this to avoid clipping the image.
     *
     * @param shape from SHAPE_*
     * @return margin size
     */
    private static float getScaleToFit(int shape) {
        switch (shape) {
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
        }
        return 0.f;
    }

    /**
     * Handle adaptive icons for compatible devices
     */
    @SuppressLint("NewApi")
    public static Drawable applyIconMaskShape(Context ctx, Drawable icon, int shape, boolean fitInside, @ColorInt int backgroundColor) {
        if (shape == SHAPE_SYSTEM)
            return icon;
        if (shape == SHAPE_TEARDROP_RND)
            shape = SHAPE_TEARDROP_BR + (icon.hashCode() % 4);

        Bitmap outputBitmap;
        Canvas outputCanvas;
        final Paint outputPaint = PAINT;
        outputPaint.reset();
        outputPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        if (isAdaptiveIconDrawable(icon)) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
            Drawable bgDrawable = adaptiveIcon.getBackground();
            Drawable fgDrawable = adaptiveIcon.getForeground();

            int layerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108f, ctx.getResources().getDisplayMetrics()));
            int iconSize = Math.round(layerSize / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
            int layerOffset = (layerSize - iconSize) / 2;

            // Create a bitmap of the icon to use it as the shader of the outputBitmap
            Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBitmap);

            // Stretch adaptive layers because they are 108dp and the icon size is 48dp
            bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
            bgDrawable.draw(iconCanvas);

            fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
            fgDrawable.draw(iconCanvas);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);

            outputPaint.setShader(new BitmapShader(iconBitmap, TileMode.CLAMP, TileMode.CLAMP));
            setIconShape(outputCanvas, outputPaint, shape);
            outputPaint.setShader(null);
        }
        // If icon is not adaptive, put it in a white canvas to make it have a unified shape
        else if (icon != null) {
            // Shrink icon fit inside the shape
            int iconSize;
            int iconOffset = 0;
            if (fitInside) {
                float marginPercent = getScaleToFit(shape);
                int iconHeight = icon.getIntrinsicHeight();
                if (iconHeight <= 0)
                    iconHeight = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
                iconSize = Math.round((1f + 2f * marginPercent) * iconHeight);
                iconOffset = Math.round(marginPercent * iconHeight);
            } else {
                // we don't have antialiasing when clipping so we make the icon bigger and let the View downscale
                iconSize = 2 * ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
            }

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint.setColor(backgroundColor);

            // Shrink icon so that it fits the shape
            int bottomRightCorner = iconSize - iconOffset;
            icon.setBounds(iconOffset, iconOffset, bottomRightCorner, bottomRightCorner);

            setIconShape(outputCanvas, outputPaint, shape);
            icon.draw(outputCanvas);
        } else {
            int iconSize = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);

            outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            outputCanvas = new Canvas(outputBitmap);
            outputPaint.setColor(0xFF000000);

            setIconShape(outputCanvas, outputPaint, shape);
        }
        return new BitmapDrawable(ctx.getResources(), outputBitmap);
    }

    /**
     * Set the shape of adaptive icons
     *
     * @param shape type of shape: DrawableUtils.SHAPE_*
     */
    private static void setIconShape(Canvas canvas, Paint paint, int shape) {
        float iconSize = canvas.getHeight();
        Path path = SHAPE_PATH;
        path.rewind();

        switch (shape) {
            case SHAPE_CIRCLE: {
                int radius = (int) iconSize / 2;
                canvas.drawCircle(radius, radius, radius, paint);

                path.addCircle(radius, radius, radius, Path.Direction.CCW);
                break;
            }
            case SHAPE_SQUIRCLE: {
                int h = (int) iconSize / 2;
                float c = iconSize / 2.333f;
                path.moveTo(h, 0f);
                path.cubicTo(h + c, 0, iconSize, h - c, iconSize, h);
                path.cubicTo(iconSize, h + c, h + c, iconSize, h, iconSize);
                path.cubicTo(h - c, iconSize, 0, h + c, 0, h);
                path.cubicTo(0, h - c, h - c, 0, h, 0);
                path.close();

                canvas.drawPath(path, paint);
                break;
            }
            case SHAPE_SQUARE:
                canvas.drawRect(0f, 0f, iconSize, iconSize, paint);

                path.addRect(0f, 0f, iconSize, iconSize, Path.Direction.CCW);
                break;
            case SHAPE_ROUND_RECT:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                canvas.drawRoundRect(RECT_F, iconSize / 8f, iconSize / 12f, paint);

                path.addRoundRect(RECT_F, iconSize / 8f, iconSize / 12f, Path.Direction.CCW);
                break;
            case SHAPE_TEARDROP_RND: // this is handled before we get here
            case SHAPE_TEARDROP_BR:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 90, 270);
                path.lineTo(iconSize, iconSize * 0.70f);
                RECT_F.set(iconSize * 0.70f, iconSize * 0.70f, iconSize, iconSize);
                path.arcTo(RECT_F, 0, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_BL:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 180, 270);
                path.lineTo(iconSize * .3f, iconSize);
                RECT_F.set(0f, iconSize * .7f, iconSize * .3f, iconSize);
                path.arcTo(RECT_F, 90, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_TL:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 270, 270);
                path.lineTo(0, iconSize * .3f);
                RECT_F.set(0f, 0f, iconSize * .3f, iconSize * .3f);
                path.arcTo(RECT_F, 180, 90, false);
                path.close();

                canvas.drawPath(path, paint);
                break;
            case SHAPE_TEARDROP_TR:
                RECT_F.set(0f, 0f, iconSize, iconSize);
                path.addArc(RECT_F, 0, 270);
                path.lineTo(iconSize * .7f, 0f);
                RECT_F.set(iconSize * .7f, 0f, iconSize, iconSize * .3f);
                path.arcTo(RECT_F, 270, 90, false);
                path.close();

                canvas.drawPath(path, paint);
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

                canvas.drawPath(path, paint);
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

                canvas.drawPath(path, paint);
                break;
        }
        // make sure we don't draw outside the shape
        canvas.clipPath(path);
    }

    public static boolean isAdaptiveIconDrawable(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return drawable instanceof AdaptiveIconDrawable;
        }
        return false;
    }
}
