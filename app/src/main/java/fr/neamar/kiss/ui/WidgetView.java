package fr.neamar.kiss.ui;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_NO;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WidgetView extends AppWidgetHostView {
    private boolean mHasPerformedLongPress;
    // Watch for long press events at this level to make sure
    // users can always pick up this widget, whatever its content does with touches
    private final GestureDetector longPressDetector;

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
        longPressDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (isAttachedToWindow() && hasWindowFocus()) {
                    performLongClick();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setExecutor(THREAD_POOL_EXECUTOR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setOnLightBackground(context.getResources().getConfiguration().uiMode);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        longPressDetector.onTouchEvent(ev);
        boolean handled = super.dispatchTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            mHasPerformedLongPress = false;
        }
        return handled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after long press is triggered
        return mHasPerformedLongPress;
    }

    @Override
    public boolean performLongClick() {
        // View also detects long presses itself when no child handles the touch: only perform one per gesture
        if (!mHasPerformedLongPress) {
            mHasPerformedLongPress = super.performLongClick();
        }
        return mHasPerformedLongPress;
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
                break;
            case UI_MODE_NIGHT_YES:
                setOnLightBackground(false);
                break;
        }
    }
}