package fr.neamar.kiss.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import fr.neamar.kiss.utils.Log;

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

                    // if height has changed then check if IME insets are visible or not
                    if (heightDiff != 0) {
                        WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(KeyboardManager.this.contentView);
                        boolean imeInsetVisible = windowInsets != null && windowInsets.isVisible(WindowInsetsCompat.Type.ime());
                        setKeyboardIsVisible(imeInsetVisible, listener);
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
