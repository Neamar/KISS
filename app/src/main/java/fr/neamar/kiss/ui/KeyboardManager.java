package fr.neamar.kiss.ui;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

public class KeyboardManager {

    private static final String TAG = KeyboardManager.class.getSimpleName();

    @FunctionalInterface
    public interface OnKeyboardListener {
        void onKeyboardVisibilityChanged(boolean isVisible);
    }

    public KeyboardManager(Context context) {
        mContext = context;
    }

    private final Context mContext;

    private View contentView;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private boolean keyboardIsVisible;

    protected int dpToPx(float valueInDp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, mContext.getResources().getDisplayMetrics());
    }

    protected void setKeyboardIsVisible(final OnKeyboardListener listener, boolean isVisible) {
        if (isVisible != keyboardIsVisible) {
            Log.d(TAG, "onKeyboardVisibilityChanged(" + isVisible + ")");
            keyboardIsVisible = isVisible;
            if (listener != null) {
                listener.onKeyboardVisibilityChanged(isVisible);
            }
        }
    }

    public void registerKeyboardListener(final OnKeyboardListener listener, View view) {
        this.contentView = view;
        unregisterKeyboardListener();
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = 0;

            @Override
            public void onGlobalLayout() {
                int newHeight = KeyboardManager.this.contentView.getHeight();
                if (previousHeight != 0) {
                    int heightDiff = previousHeight - newHeight;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // if height has changed then check if IME insets are visible or not
                        if (heightDiff != 0) {
                            WindowInsets windowInsets = KeyboardManager.this.contentView.getRootWindowInsets();
                            boolean imeInsetVisible = windowInsets.isVisible(WindowInsets.Type.ime());
                            setKeyboardIsVisible(listener, imeInsetVisible);
                        }
                    } else {
                        int changeThreshold = dpToPx(150);
                        if (heightDiff > changeThreshold) {
                            // we assume that keyboard was shown
                            setKeyboardIsVisible(listener, true);
                        } else if (heightDiff < -changeThreshold) {
                            // we assume that keyboard was hidden
                            setKeyboardIsVisible(listener, false);
                        }
                    }
                }
                previousHeight = newHeight;
            }
        };
        this.contentView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    public void unregisterKeyboardListener() {
        if (onGlobalLayoutListener != null) {
            this.contentView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }
}
