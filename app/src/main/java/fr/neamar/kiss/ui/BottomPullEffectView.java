package fr.neamar.kiss.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EdgeEffect;

/**
 * View that renders that over-scroll/"pulled too far" effect
 * <p>
 * Parts (or even all) of the given effect parameters may be discarded with the underlying Android
 * platform does not support them.
 */
public class BottomPullEffectView extends View {
    private EdgeEffect effect;
    private float lastPullDistance;
    private float lastPullDisplacement;
    private boolean lastPullAnimated;

    public BottomPullEffectView(Context context) {
        super(context);
    }

    public BottomPullEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomPullEffectView(Context context, AttributeSet attrs, int flags) {
        super(context, attrs, flags);
    }

    /**
     * Force the pull effect to display the given pull distance and left/right displacement
     *
     * @param distance     Pull distance (0.0f – 1.0f)
     * @param displacement Left/right displacement (0.0f → right, 0.5f → center, 1.0f → left)
     * @param animated     Should this pull eventually fade away?
     */
    public void setPull(float distance, float displacement, boolean animated) {
        // Reset internal effect state by creating a new instance
        //XXX: This may cause unnecessary GC runs!
        this.effect = new EdgeEffect(this.getContext());

        // Provide new pull effect data
        this.effect.setSize(this.getWidth(), this.getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.effect.onPull(distance, displacement);
        } else {
            this.effect.onPull(distance);
        }

        if (!animated) {
            // Prevent more than one frame being drawn
            this.effect.finish();
        }

        this.lastPullDistance = distance;
        this.lastPullDisplacement = displacement;
        this.lastPullAnimated = animated;

        // Request scene to be redrawn
        this.invalidate();
    }

    /**
     * Draw a release animation for the previous pull effect
     */
    public void releasePull() {
        if (this.effect == null) {
            return;
        }

        // Recreate effect without `finish()`-ing it, so that the release effect will be
        // properly drawn
        if (!this.lastPullAnimated) {
            this.setPull(this.lastPullDistance, this.lastPullDisplacement, true);
        }

        this.effect.onRelease();
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (this.effect == null) {
            return;
        }

        final int canvas_save_count = canvas.save();
        canvas.translate(-this.getWidth(), this.getHeight());
        canvas.rotate(180, this.getWidth(), 0);

        this.effect.setSize(this.getWidth(), this.getHeight());
        boolean invalidate = this.effect.draw(canvas);

        canvas.restoreToCount(canvas_save_count);

        if (invalidate) {
            this.invalidate();
        }
    }
}
