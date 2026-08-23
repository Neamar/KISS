package fr.neamar.kiss.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * Vertical scroller hosting the widget area.
 *
 * The area scrolls only through a two-finger gesture; a single finger always
 * belongs to the widget underneath or is forwarded verbatim to the launcher's
 * gesture pipeline through {@link #setEmptyAreaTouchListener(View.OnTouchListener)}.
 *
 * Scrolling can be disabled entirely through settings ("Enable widget
 * scrolling"): this reverts to the legacy static widget area.
 */
public class WidgetScrollView extends NestedScrollView {
    private static final int INVALID_POINTER_ID = -1;

    /**
     * Whether widget scrolling is enabled
     */
    private boolean scrollingEnabled = true;

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

    // Two-finger scroll state
    @Nullable
    private VelocityTracker velocityTracker;
    private int scrollPointerId = INVALID_POINTER_ID;
    private float lastScrollY;
    private int maximumFlingVelocity;

    public WidgetScrollView(Context context) {
        this(context, null);
    }

    public WidgetScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.scrollViewStyle);
    }

    public WidgetScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        maximumFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    }

    /**
     * Enable or disable widget scrolling.
     *
     * When disabled, this view behaves as a static container.
     */
    public void setScrollingEnabled(boolean enabled) {
        if (scrollingEnabled == enabled) {
            return;
        }
        scrollingEnabled = enabled;
        // a disabled view ignores touches itself, but still dispatches them to children
        setEnabled(enabled);
    }

    /**
     * Set the listener receiving empty-area gestures. Events are forwarded
     * verbatim, so they run through the launcher's standard gesture pipeline.
     */
    public void setEmptyAreaTouchListener(@Nullable View.OnTouchListener listener) {
        emptyAreaTouchListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!scrollingEnabled) {
            // never intercept: widgets (including internal lists) keep full gestures
            return false;
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && ev.getPointerCount() >= 2) {
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
        if (!scrollingEnabled) {
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // consume so a possible second finger can still be observed
                forwardingGesture = true;
                forwardToEmptyAreaListener(ev);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                // always carries at least two pointers by definition
                if (twoFingerScrolling) {
                    trackVelocity(ev);
                } else {
                    startTwoFingerScroll(ev);
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (twoFingerScrolling) {
                    switchScrollPointerIfNeeded(ev);
                    trackVelocity(ev);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (twoFingerScrolling) {
                    trackVelocity(ev);
                    scrollByScrollPointer(ev);
                } else {
                    forwardToEmptyAreaListener(ev);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (twoFingerScrolling) {
                    finishTwoFingerScroll(ev);
                } else {
                    forwardToEmptyAreaListener(ev);
                    forwardingGesture = false;
                }
                return true;
        }
        // only exotic actions reach this: delegate them to the super class
        // (mouse wheels arrive via onGenericMotionEvent instead)
        return super.onTouchEvent(ev);
    }

    private void startTwoFingerScroll(MotionEvent ev) {
        twoFingerScrolling = true;
        boolean wasForwarding = forwardingGesture;
        forwardingGesture = false;

        recycleVelocityTracker();
        velocityTracker = VelocityTracker.obtain();
        scrollPointerId = ev.getPointerId(ev.getActionIndex());
        lastScrollY = ev.getY(ev.findPointerIndex(scrollPointerId));
        velocityTracker.addMovement(ev);

        // cancel pending widget long-press checks so no menu opens during the scroll
        cancelWidgetLongPresses();

        // end the delegated gesture cleanly (stolen streams get their
        // ACTION_CANCEL from the ViewGroup automatically)
        if (wasForwarding && emptyAreaTouchListener != null) {
            MotionEvent cancel = MotionEvent.obtain(ev);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            emptyAreaTouchListener.onTouch(this, cancel);
            cancel.recycle();
        }
    }

    private void cancelWidgetLongPresses() {
        if (!(getChildAt(0) instanceof ViewGroup)) {
            return;
        }
        ViewGroup widgetArea = (ViewGroup) getChildAt(0);
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            widgetArea.getChildAt(i).cancelLongPress();
        }
    }

    private void scrollByScrollPointer(MotionEvent ev) {
        int pointerIndex = ev.findPointerIndex(scrollPointerId);
        if (pointerIndex < 0) {
            return;
        }
        float y = ev.getY(pointerIndex);
        float deltaY = lastScrollY - y;
        lastScrollY = y;
        if (deltaY != 0) {
            scrollBy(0, (int) deltaY);
        }
    }

    /**
     * If the pointer driving the scroll was lifted, continue with a remaining one.
     */
    private void switchScrollPointerIfNeeded(MotionEvent ev) {
        int liftedIndex = ev.getActionIndex();
        if (ev.getPointerId(liftedIndex) != scrollPointerId) {
            return;
        }
        int remainingIndex = liftedIndex == 0 ? 1 : 0;
        if (remainingIndex < ev.getPointerCount()) {
            scrollPointerId = ev.getPointerId(remainingIndex);
            lastScrollY = ev.getY(remainingIndex);
        }
    }

    private void finishTwoFingerScroll(MotionEvent ev) {
        try {
            if (velocityTracker != null) {
                velocityTracker.addMovement(ev);
                velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_UP && velocityTracker != null) {
                float velocityY = velocityTracker.getYVelocity(scrollPointerId);
                fling(-Math.round(velocityY));
            }
        } finally {
            twoFingerScrolling = false;
            scrollPointerId = INVALID_POINTER_ID;
            recycleVelocityTracker();
        }
    }

    private void forwardToEmptyAreaListener(MotionEvent ev) {
        if (emptyAreaTouchListener != null) {
            emptyAreaTouchListener.onTouch(this, ev);
        }
    }

    private void trackVelocity(MotionEvent ev) {
        if (velocityTracker != null) {
            velocityTracker.addMovement(ev);
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes, int type) {
        // all scrolling of this area goes through the two-finger gesture:
        // never accept nested scroll handoffs from widgets' internal lists
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        // mouse wheels and trackpads scroll via the generic-motion path, which
        // bypasses the touch pipeline: respect "Enable widget scrolling" here too
        if (!scrollingEnabled && event.getAction() == MotionEvent.ACTION_SCROLL) {
            return false;
        }
        return super.onGenericMotionEvent(event);
    }
}
