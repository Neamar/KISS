package fr.neamar.kiss.ui;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_NO;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Source: <a href="https://github.com/willli666/Android-Trebuchet-Launcher-Standalone/blob/master/src/com/cyanogenmod/trebuchet/LauncherAppWidgetHostView.java">LauncherAppWidgetHostView.java</a>
 */
public class WidgetView extends AppWidgetHostView {
    protected boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;
    private float xPos;
    private float yPos;

    private static final int POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    private static final int KEEP_ALIVE = 1;

    /**
     * An {@link ThreadPoolExecutor} to be used with async task with no limit on the queue size.
     */
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        } else {
            THREAD_POOL_EXECUTOR = null;
        }
    }

    public WidgetView(Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setExecutor(THREAD_POOL_EXECUTOR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setOnLightBackground(context.getResources().getConfiguration().uiMode);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after long press is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for long press events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                postCheckForLongClick();
                xPos = ev.getX();
                yPos = ev.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(ev.getX() - xPos) > 5 || Math.abs(ev.getY() - yPos) > 5) {
                    mHasPerformedLongPress = false;
                    cancelPendingLongPress();
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                cancelPendingLongPress();
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    /**
     * Cancel the pending long-press check here too: once a parent has
     * intercepted a gesture, the resulting ACTION_CANCEL bypasses
     * {@link #onInterceptTouchEvent} entirely.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
                mHasPerformedLongPress = false;
                cancelPendingLongPress();
                break;
        }
        return super.onTouchEvent(ev);
    }

    protected class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !WidgetView.this.mHasPerformedLongPress) {
                if (performLongClick()) {
                    WidgetView.this.mHasPerformedLongPress = true;
                }
            }
        }

        void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void cancelPendingLongPress() {
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        cancelPendingLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // calculate size in dips
        float density = getResources().getDisplayMetrics().density;
        int widthDips = (int) (w / density);
        int heightDips = (int) (h / density);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAppWidgetSize(Bundle.EMPTY, Collections.singletonList(new SizeF(widthDips, heightDips)));
        } else {
            updateAppWidgetSize(null, widthDips, heightDips, widthDips, heightDips);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public void setOnLightBackground(int uiMode) {
        switch (uiMode & UI_MODE_NIGHT_MASK) {
            case UI_MODE_NIGHT_NO:
                setOnLightBackground(true);
            case UI_MODE_NIGHT_YES:
                setOnLightBackground(false);
        }
    }
}