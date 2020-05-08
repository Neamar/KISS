package fr.neamar.kiss.ui;

import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import android.preference.PreferenceManager;

import fr.neamar.kiss.utils.DrawableUtils;

/**
 * A rounded version of {@link QuickContactBadge]
 *
 * @author kishu27 (http://linkd.in/1laN852)
 */
public class RoundedQuickContactBadge extends QuickContactBadge {
    private final String iconsPack;

    public RoundedQuickContactBadge(Context context) {
        super(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        iconsPack = prefs.getString("icons-pack", "default");
    }

    public RoundedQuickContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        iconsPack = prefs.getString("icons-pack", "default");
    }

    public RoundedQuickContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        iconsPack = prefs.getString("icons-pack", "default");
    }

    public static class RoundedDrawable extends Drawable {

        private final Paint mPaint;
        private final BitmapShader mBitmapShader;
        private final RectF mBitmapRect;
        private final RectF mDisplayBounds;
        private final String mIconsPack;

        public RoundedDrawable(Bitmap bitmap, String iconsPack) {
            mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setShader(mBitmapShader);

            mBitmapRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            mDisplayBounds = new RectF();
            mDisplayBounds.set(mBitmapRect);

            mIconsPack = iconsPack;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            Matrix m = new Matrix();
            mBitmapShader.getLocalMatrix(m);

            // Scale bitmap to display within specified bounds
            int minScale = Math.min(bounds.width(), bounds.height());
            m.setScale(minScale / mBitmapRect.width(), minScale / mBitmapRect.height());

            // When bounds is not a square, ensure we display the bitmap centered
            // (we will clip to display as a centered circle,
            //  and we need to ensure the bitmap is in the right position below)
            if (bounds.width() > bounds.height()) {
                m.postTranslate((bounds.width() - bounds.height()) * 0.5f, 0f);
            }
            mBitmapShader.setLocalMatrix(m);

            mDisplayBounds.set(bounds);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            float radius = mDisplayBounds.height() * 0.5f;

            if(DrawableUtils.isIconsPackAdaptive(mIconsPack)) {
                DrawableUtils.setIconShape(canvas, mPaint, mIconsPack);
            } else {
                canvas.drawCircle(mDisplayBounds.centerX(), mDisplayBounds.centerY(), radius, mPaint);
            }
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

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            drawable = new RoundedDrawable(bitmap, iconsPack);
        }
        super.setImageDrawable(drawable);
    }
}
