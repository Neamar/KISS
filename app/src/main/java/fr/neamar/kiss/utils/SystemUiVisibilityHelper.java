package fr.neamar.kiss.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class SystemUiVisibilityHelper implements View.OnSystemUiVisibilityChangeListener {
    private static final String TAG = SystemUiVisibilityHelper.class.getSimpleName();
    private final Activity mActivity;
    private final Handler mHandler;
    private final SharedPreferences prefs;
    private boolean mKeyboardVisible;
    private boolean mIsScrolling;
    private int mPopupCount;

    // This is used to emulate SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private final Runnable autoApplySystemUiRunnable = this::autoApplySystemUi;

    private void autoApplySystemUi() {
        if (!mKeyboardVisible && !mIsScrolling && mPopupCount == 0)
            applySystemUi();
    }

    public SystemUiVisibilityHelper(Activity activity) {
        mActivity = activity;
        mHandler = new Handler(Looper.getMainLooper());
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        View decorView = mActivity.getWindow()
                .getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(this);
        mKeyboardVisible = false;
        mIsScrolling = false;
        mPopupCount = 0;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (mIsScrolling)
                applyScrollSystemUi();
            else
                applySystemUi();
        }
    }

    public void onKeyboardVisibilityChanged(boolean isVisible) {
        mKeyboardVisible = isVisible;
        if (isVisible) {
            mHandler.removeCallbacks(autoApplySystemUiRunnable);
            applySystemUi(false, false, hasBlackNotificationIcons());
        } else {
            autoApplySystemUiRunnable.run();
        }
    }

    private void applySystemUi() {
        applySystemUi(isPreferenceHideNavBar(), isPreferenceHideStatusBar(), hasBlackNotificationIcons());
    }

    private void applySystemUi(boolean hideNavBar, boolean hideStatusBar, boolean hasBlackNotificationIcons) {
        int visibility = 0;
        if (hideNavBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility = visibility
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; // hide nav bar
            } else {
                visibility = visibility
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; // hide nav bar
            }
        }
        if (hideStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                visibility = visibility
                        | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
            }
        }
        if (hideNavBar || hideStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility = visibility
                        | View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
        }
        if (hasBlackNotificationIcons) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                visibility = visibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }

        View decorView = mActivity.getWindow()
                .getDecorView();
        decorView.setSystemUiVisibility(visibility);
    }

    public void applyScrollSystemUi() {
        mIsScrolling = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            applySystemUi();
        }
    }

    public void resetScroll() {
        mIsScrolling = false;
        if (!mKeyboardVisible)
            mHandler.post(autoApplySystemUiRunnable);
    }

    private boolean isPreferenceHideNavBar() {
        return prefs.getBoolean("pref-hide-navbar", false);
    }

    private boolean isPreferenceHideStatusBar() {
        return prefs.getBoolean("pref-hide-statusbar", false);
    }

    private boolean hasBlackNotificationIcons() {
        return prefs.getBoolean("black-notification-icons", false);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("onSystemUiVisibilityChange %x", visibility));

        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0)
            sb.append("\n SYSTEM_UI_FLAG_HIDE_NAVIGATION");

        if ((visibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0)
            sb.append("\n SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN");
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0)
            sb.append("\n SYSTEM_UI_FLAG_FULLSCREEN");

        if ((visibility & View.SYSTEM_UI_FLAG_IMMERSIVE) != 0)
            sb.append("\n SYSTEM_UI_FLAG_IMMERSIVE");
        if ((visibility & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0)
            sb.append("\n SYSTEM_UI_FLAG_IMMERSIVE_STICKY");

        if ((visibility & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0)
            sb.append("\n SYSTEM_UI_FLAG_LIGHT_STATUS_BAR");

        Log.d(TAG, sb.toString());

        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0) {
            applySystemUi();
        }

        if (visibility == 0) {
            mHandler.postDelayed(autoApplySystemUiRunnable, 1500);
        }
    }

    public void copyVisibility(View contentView) {
        View decorView = mActivity.getWindow()
                .getDecorView();
        int visibility = decorView.getSystemUiVisibility();
        contentView.setSystemUiVisibility(visibility);
    }

    public void addPopup() {
        mPopupCount += 1;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            applySystemUi(false, false, false);
        }
    }

    public void popPopup() {
        mPopupCount -= 1;
        if (mPopupCount < 0) {
            Log.e(TAG, "Popup count negative!");
            mPopupCount = 0;
        }
    }
}
