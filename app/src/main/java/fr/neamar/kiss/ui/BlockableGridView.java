package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * GridView displaying the results, tuned for mouse usage:
 * <ul>
 *     <li>a mouse right click opens the item menu, like a long click</li>
 *     <li>data changes can wait until no finger / button is down on the grid</li>
 * </ul>
 */
public class BlockableGridView extends GridView {
    private boolean touched = false;
    /**
     * Set while a right click is being processed: the matching move / up events must not trigger a regular click
     */
    private boolean consumingSecondaryClick = false;
    @Nullable
    private Runnable afterTouch = null;
    // Runs afterTouch once the touch ended, unless it was cancelled or a new touch started meanwhile
    private final Runnable runAfterTouch = () -> {
        if (!touched && afterTouch != null) {
            Runnable runnable = afterTouch;
            afterTouch = null;
            runnable.run();
        }
    };

    public BlockableGridView(Context context) {
        super(context);
    }

    public BlockableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Run once no finger is on the grid. Changing the data while a finger is down cancels a long press,
     * which then turns into a click on release, and drops a pending click.
     * Only the last runnable passed while the grid is touched is run.
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

        if (action == MotionEvent.ACTION_DOWN && isSecondaryClick(ev)) {
            consumingSecondaryClick = true;
            openItemMenuAt(ev.getX(), ev.getY());
            return true;
        }
        if (consumingSecondaryClick) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                consumingSecondaryClick = false;
            }
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            touched = true;
        }
        boolean handled = super.dispatchTouchEvent(ev);
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && touched) {
            touched = false;
            if (afterTouch != null) {
                // A tap is only clicked once its pressed state was displayed: run after it
                removeCallbacks(runAfterTouch);
                postDelayed(runAfterTouch, ViewConfiguration.getPressedStateDuration() + 1);
            }
        }
        return handled;
    }

    private static boolean isSecondaryClick(MotionEvent ev) {
        return ev.isFromSource(InputDevice.SOURCE_MOUSE)
                && (ev.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0;
    }

    /**
     * Same behavior as a long click on the item under the mouse pointer
     */
    private void openItemMenuAt(float x, float y) {
        int position = pointToPosition((int) x, (int) y);
        if (position == INVALID_POSITION || getOnItemLongClickListener() == null) {
            return;
        }
        View child = getChildAt(position - getFirstVisiblePosition());
        if (child != null) {
            getOnItemLongClickListener().onItemLongClick(this, child, position, getItemIdAtPosition(position));
        }
    }
}
