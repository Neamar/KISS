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

    static class RoundedDrawable extends Drawable
    {

        private final Paint mPaint;
        private final BitmapShader mBitmapShader;
        private final RectF mRect;

        RoundedDrawable(Bitmap bitmap) {
            mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setShader(mBitmapShader);
            
            mRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            Matrix m = new Matrix();
            mBitmapShader.getLocalMatrix(m);
            m.setScale(bounds.width() / mRect.width(), bounds.height() / mRect.height());
            mRect.set(bounds);
            mBitmapShader.setLocalMatrix(m);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawRoundRect(mRect, mRect.width() * .5f, mRect.height() * .5f, mPaint);
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
        if ( drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            drawable = new RoundedDrawable(bitmap);
        }
        super.setImageDrawable(drawable);
    }
}
