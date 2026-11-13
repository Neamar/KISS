package fr.neamar.kiss.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

/**
 * Vertical scroller hosting the widget area.
 *
 * The area scrolls only through a two-finger gesture; a single finger always
 * belongs to the widget underneath or is forwarded verbatim to the launcher's
 * gesture pipeline through {@link #setEmptyAreaTouchListener(View.OnTouchListener)}.
 *
 * When the widgets fit the viewport, this view behaves as a static container.
 */
public class WidgetScrollView extends ScrollView {
    /**
     * Whether the current gesture is an active two-finger scroll
     */
    private boolean twoFingerScrolling;

    /**
     * Whether the current single-finger gesture is being forwarded
     */
    private boolean forwardingGesture;

    /**
     * Listener receiving empty-area gestures, handled as if performed on a non-widget area
     */
    @Nullable
    private View.OnTouchListener emptyAreaTouchListener;

    public WidgetScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the listener receiving empty-area gestures. Events are forwarded
     * verbatim, so they run through the launcher's standard gesture pipeline.
     */
    public void setEmptyAreaTouchListener(@Nullable View.OnTouchListener listener) {
        emptyAreaTouchListener = listener;
    }

    /**
     * Whether the widgets overflow the viewport
     */
    private boolean canScroll() {
        return canScrollVertically(1) || canScrollVertically(-1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!canScroll()) {
            // never intercept: widgets (including internal lists) keep full gestures
            return false;
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // any touch stops in-flight momentum (standard Android fling behavior):
            // a fling without velocity replaces the current one
            fling(0);
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            // two-finger scroll: steal the gesture from the widget underneath
            startTwoFingerScroll(ev);
            return true;
        }

        // never steal single-finger gestures from widgets
        return false;
    }

    /**
     * No click semantics of its own: clicks belong to the widgets underneath.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (twoFingerScrolling) {
            if (action == MotionEvent.ACTION_POINTER_UP) {
                // the scroll ends as soon as a finger is lifted, to fling with the velocity of both fingers:
                // ScrollView forgets it when the finger it tracks is lifted before the other one
                twoFingerScrolling = false;
                return superOnTouchEvent(ev, MotionEvent.ACTION_UP);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                twoFingerScrolling = false;
            }
            return super.onTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (!canScroll()) {
                return false;
            }
            // consume so a possible second finger can still be observed
            forwardingGesture = true;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            startTwoFingerScroll(ev);
            return true;
        }

        if (forwardingGesture && emptyAreaTouchListener != null) {
            emptyAreaTouchListener.onTouch(this, ev);
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            forwardingGesture = false;
        }
        return true;
    }

    private void startTwoFingerScroll(MotionEvent ev) {
        twoFingerScrolling = true;

        // end the delegated gesture cleanly (stolen streams get their
        // ACTION_CANCEL from the ViewGroup automatically)
        if (forwardingGesture && emptyAreaTouchListener != null) {
            MotionEvent cancel = MotionEvent.obtain(ev);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            emptyAreaTouchListener.onTouch(this, cancel);
            cancel.recycle();
        }
        forwardingGesture = false;

        // ScrollView only scrolls gestures it has seen from their beginning:
        // start one here, the rest of the gesture is then handled by super.onTouchEvent()
        superOnTouchEvent(ev, MotionEvent.ACTION_DOWN);
    }

    /**
     * Have ScrollView handle the given event as if it was of the given action
     */
    private boolean superOnTouchEvent(MotionEvent ev, int action) {
        MotionEvent event = MotionEvent.obtain(ev);
        event.setAction(action);
        boolean handled = super.onTouchEvent(event);
        event.recycle();
        return handled;
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // all scrolling of this area goes through the two-finger gesture:
        // never accept nested scroll handoffs from widgets' internal lists
        return false;
    }
}
