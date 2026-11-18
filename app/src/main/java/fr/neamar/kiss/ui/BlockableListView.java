package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ListView subclass that provides an interface for (temporarily) blocking all of it's input events
 */
public class BlockableListView extends ListView {
    private boolean touchEventsBlocked = false;
    private boolean touched = false;
    @Nullable
    private Runnable afterTouch = null;

    public BlockableListView(Context context) {
        super(context);
    }

    public BlockableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Prevent this ListView from receiving any new touch events
     * <p>
     * Use {@link #unblockTouchEvents()} to end the blockage.
     */
    public void blockTouchEvents() {
        this.touchEventsBlocked = true;
    }

    /**
     * Stop preventing this ListView from receiving touch events
     */
    public void unblockTouchEvents() {
        this.touchEventsBlocked = false;
    }

    /**
     * Run once no finger is on the list. Changing the data while a finger is down cancels a long press,
     * which then turns into a click on release, and drops a pending click.
     * Only the last runnable passed while the list is touched is run.
     */
    public void runWhenNotTouched(@NonNull Runnable runnable) {
        if (touched) {
            afterTouch = runnable;
        } else {
            afterTouch = null;
            runnable.run();
        }
    }

    /**
     * Forget the runnable waiting for the touch to end, e.g. because newer data was displayed
     */
    public void cancelRunWhenNotTouched() {
        afterTouch = null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            touched = true;
        }
        boolean handled = super.dispatchTouchEvent(ev);
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && touched) {
            touched = false;
            if (afterTouch != null) {
                Runnable runnable = afterTouch;
                afterTouch = null;
                // A tap is only clicked once its pressed state was displayed: run after it
                postDelayed(runnable, ViewConfiguration.getPressedStateDuration() + 1);
            }
        }
        return handled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.touchEventsBlocked || super.onTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        return this.touchEventsBlocked || super.performClick();
    }
}
