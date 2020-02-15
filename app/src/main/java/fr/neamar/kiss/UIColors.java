package fr.neamar.kiss;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

public class UIColors {
    public static final int COLOR_DEFAULT = 0xFF4caf50;
    // Source: https://material.io/guidelines/style/color.html#color-color-palette
    public static final int[] COLOR_LIST = new int[]{
            0xFF4CAF50,
            0xFFD32F2F,
            0xFFC2185B,
            0xFF7B1FA2,
            0xFF512DA8,
            0xFF303F9F,
            0xFF1976D2,
            0xFF0288D1,
            0xFF0097A7,
            0xFF00796B,
            0xFF388E3C,
            0xFF689F38,
            0xFFAFB42B,
            0xFFFBC02D,
            0xFFFFA000,
            0xFFF57C00,
            0xFFE64A19,
            0xFF5D4037,
            0xFF616161,
            0xFF455A64,
            0xFF000000
    };

    private static final int[] OVERLAY_LIST = new int[]{
            -1,
            R.style.OverlayAccentD32F2F,
            R.style.OverlayAccentC2185B,
            R.style.OverlayAccent7B1FA2,
            R.style.OverlayAccent512DA8,
            R.style.OverlayAccent303F9F,
            R.style.OverlayAccent1976D2,
            R.style.OverlayAccent0288D1,
            R.style.OverlayAccent0097A7,
            R.style.OverlayAccent00796B,
            R.style.OverlayAccent388E3C,
            R.style.OverlayAccent689F38,
            R.style.OverlayAccentAFB42B,
            R.style.OverlayAccentFBC02D,
            R.style.OverlayAccentFFA000,
            R.style.OverlayAccentF57C00,
            R.style.OverlayAccentE64A19,
            R.style.OverlayAccent5D4037,
            R.style.OverlayAccent616161,
            R.style.OverlayAccent455A64,
            -1,
    };

    private static final String COLOR_DEFAULT_STR = String.format("#%06X", COLOR_DEFAULT & 0xFFFFFF);

    private static int primaryColor = -1;

    // https://stackoverflow.com/questions/25815769/how-to-really-programmatically-change-primary-and-accent-color-in-android-loll
    public static void applyOverlay(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        // We want to update the accent color for the theme.
        // Each possible accent color is defined as a custom overlay, we need to find the matching one and apply it
        int primaryColor = getPrimaryColor(activity);

        for (int i = 0; i < COLOR_LIST.length; i++) {

            if (COLOR_LIST[i] == primaryColor) {
                int resId = OVERLAY_LIST[i];
                if(resId != -1) {
                    activity.getTheme().applyStyle(resId, true);
                }
                return;
            }
        }
    }

    public static void updateThemePrimaryColor(Activity activity) {
        int notificationBarColorOverride = getNotificationBarColor(activity);

        // Circuit breaker, keep default behavior.
        if (notificationBarColorOverride == COLOR_DEFAULT) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(notificationBarColorOverride);
        }

        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(notificationBarColorOverride));
        }
    }

    private static int getNotificationBarColor(Context context) {
        return Color.parseColor(
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("notification-bar-color", COLOR_DEFAULT_STR)
        );
    }

    public static int getPrimaryColor(Context context) {
        if (primaryColor == -1) {
            String primaryColorStr = PreferenceManager.getDefaultSharedPreferences(context).getString("primary-color", COLOR_DEFAULT_STR);

            // Transparent can't be displayed for text color, replace with light gray.
            if (primaryColorStr.equals("#00000000") || primaryColorStr.equals("#AAFFFFFF")) {
                primaryColor = 0xFFBDBDBD;
            } else {
                primaryColor = Color.parseColor(primaryColorStr);
            }
        }

        return primaryColor;
    }

    static void clearPrimaryColorCache(Context context) {
        primaryColor = -1;
    }
}
