package fr.neamar.kiss.ui;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import fr.neamar.kiss.utils.DrawableUtils;

public class KeyboardManager {

    private static final String TAG = KeyboardManager.class.getSimpleName();

    @FunctionalInterface
    public interface OnKeyboardListener {
        void onKeyboardVisibilityChanged(boolean isVisible);
    }

    public KeyboardManager(Context context) {
        mContext = context;
    }

    protected final Context mContext;

    protected View contentView;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private boolean keyboardIsVisible;

    protected void setKeyboardIsVisible(boolean isVisible, final OnKeyboardListener listener) {
        if (isVisible != keyboardIsVisible) {
            Log.d(TAG, "onKeyboardVisibilityChanged(" + isVisible + ")");
            keyboardIsVisible = isVisible;
            if (listener != null) {
                listener.onKeyboardVisibilityChanged(isVisible);
            }
        }
    }

    public void registerKeyboardListener(View view, final OnKeyboardListener listener) {
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
                            setKeyboardIsVisible(imeInsetVisible, listener);
                        }
                    } else {
                        int changeThreshold = DrawableUtils.dpToPx(mContext, 150);
                        if (heightDiff > changeThreshold) {
                            // we assume that keyboard was shown
                            setKeyboardIsVisible(true, listener);
                        } else if (heightDiff < -changeThreshold) {
                            // we assume that keyboard was hidden
                            setKeyboardIsVisible(false, listener);
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
