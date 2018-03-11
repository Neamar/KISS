package fr.neamar.kiss.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

import java.lang.reflect.Field;

/**
 * A rounded version of {@link QuickContactBadge]
 *
 * @author kishu27 (http://linkd.in/1laN852)
 */
public class RoundedQuickContactBadge extends QuickContactBadge {

    public RoundedQuickContactBadge(Context context) {
        super(context);
        init(); //Set our initialization
    }

    public RoundedQuickContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(); //Set our initialization
    }

    public RoundedQuickContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(); //Set our initialization
    }

    public static class RoundedDrawable extends Drawable {

        private final Paint mPaint;
        private final BitmapShader mBitmapShader;
        private final RectF mBitmapRect;
        private RectF mDisplayBounds;

        public RoundedDrawable(Bitmap bitmap) {
            mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setShader(mBitmapShader);

            mBitmapRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            mDisplayBounds = new RectF();
            mDisplayBounds.set(mBitmapRect);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            Matrix m = new Matrix();
            mBitmapShader.getLocalMatrix(m);

            // Scale bitmap to display within specified bounds
            int minScale = Math.min(bounds.width(), bounds.height());
            m.setScale(minScale / mBitmapRect.width(), minScale / mBitmapRect.height());
            mBitmapShader.setLocalMatrix(m);
            
            mDisplayBounds.set(bounds);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            float radius = mDisplayBounds.height() / 2;
            canvas.drawCircle(radius, radius, radius, mPaint);
            // canvas.drawRoundRect(mBitmapRect, radius, radius, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

    }

    /**
     * Initialize our stuff
     */
    private void init() {

        //Use reflection to reset the default triangular overlay from default quick contact badge
        try {
            Field field = QuickContactBadge.class.getDeclaredField("mOverlay");
            field.setAccessible(true);

            //Using null to not draw anything at all
            field.set(this, null);

        } catch (Exception e) {
            //No-op, just well off with the default overlay
        }

    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            drawable = new RoundedDrawable(bitmap);
        }
        super.setImageDrawable(drawable);
    }
}
