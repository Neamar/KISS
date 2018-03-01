package fr.neamar.kiss.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

import java.lang.reflect.Field;

/**
 * A rounded version of {@link QuickContactBadge]
 *
 * @author kishu27 (http://linkd.in/1laN852)
 */
public class RoundedQuickContactBadge extends QuickContactBadge {

    /**
     * This path is used to mask out the outer edges of a circle on this View
     */
    private static Bitmap rounder = null;
    private static Paint xferPaint;

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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (rounder != null) {
            if (rounder.getWidth() != w || rounder.getHeight() != h) {
                rounder.recycle();
            } else {
                // we have everything set-up already
                return;
            }
        }

        // We have to make sure our rounded corners have an alpha channel in most cases
        rounder = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(rounder);

        // We're going to apply this paint eventually using a porter-duff xfer mode.
        // This will allow us to only overwrite certain pixels. RED is arbitrary. This
        // could be any color that was fully opaque (alpha = 255)
        xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.RED);

        // We're just reusing xferPaint to paint a normal looking rounded box
        canvas.drawRoundRect(new RectF(0, 0, w, h), w, h, xferPaint);

        // Now we apply the 'magic sauce' to the paint
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(rounder, 0, 0, xferPaint);
    }
}
