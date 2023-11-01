package fr.neamar.kiss;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class UIColors {
    public static final int COLOR_DEFAULT = 0xFF4CAF50;

    public static final int COLOR_TRANSPARENT = 0x00000000;
    public static final int COLOR_LIGHT_TRANSPARENT = 0xAAFFFFFF;
    public static final int COLOR_DARK_TRANSPARENT = 0xAA000000;
    public static final int COLOR_SYSTEM = 0x00FFFFFF;

    // Source: https://material.io/guidelines/style/color.html#color-color-palette
    private static final int[] COLOR_LIST = new int[]{
            COLOR_DEFAULT,
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

    private static final String COLOR_DEFAULT_STR = colorToString(COLOR_DEFAULT);

    private static int primaryColor = -1;

    // https://stackoverflow.com/questions/25815769/how-to-really-programmatically-change-primary-and-accent-color-in-android-loll
    public static void applyOverlay(Activity activity, SharedPreferences prefs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        // We want to update the accent color for the theme.
        // Each possible accent color is defined as a custom overlay, we need to find the matching one and apply it
        int primaryColor = getPrimaryColor(activity);

        for (int i = 0; i < COLOR_LIST.length; i++) {
            if (COLOR_LIST[i] == primaryColor) {
                int resId = OVERLAY_LIST[i];
                if (resId != -1) {
                    activity.getTheme().applyStyle(resId, true);
                }
                break;
            }
        }


        String shadowStyle = prefs.getString("theme-shadow", "default");
        switch (shadowStyle) {
            case "enabled":
                activity.getTheme().applyStyle(R.style.OverlayShadowEnabled, true);
                break;
            case "disabled":
                activity.getTheme().applyStyle(R.style.OverlayShadowDisabled, true);
                break;
        }

        String separatorStyle = prefs.getString("theme-separator", "default");
        switch (separatorStyle) {
            case "disabled":
                activity.getTheme().applyStyle(R.style.OverlaySeparatorDisabled, true);
                break;
            case "light":
                activity.getTheme().applyStyle(R.style.OverlaySeparatorLight, true);
                break;
            case "dark":
                activity.getTheme().applyStyle(R.style.OverlaySeparatorDark, true);
                break;
        }

        String resultColorStyle = prefs.getString("theme-result-color", "default");
        switch (resultColorStyle) {
            case "light":
                activity.getTheme().applyStyle(R.style.OverlayResultColorLight, true);
                break;
            case "dark":
                activity.getTheme().applyStyle(R.style.OverlayResultColorDark, true);
                break;
            case "semi-light":
                activity.getTheme().applyStyle(R.style.OverlayResultColorSemiLight, true);
                break;
            case "semi-dark":
                activity.getTheme().applyStyle(R.style.OverlayResultColorSemiDark, true);
                break;
            case "black":
                activity.getTheme().applyStyle(R.style.OverlayResultColorBlack, true);
                break;
        }

        String wallpaperStyle = prefs.getString("theme-wallpaper", "default");
        switch (wallpaperStyle) {
            case "enabled":
                activity.getTheme().applyStyle(R.style.OverlayWallpaperEnabled, true);
                break;
            case "disabled":
                activity.getTheme().applyStyle(R.style.OverlayWallpaperDisabled, true);
                break;
        }

        String barColor = prefs.getString("theme-bar-color", "default");
        switch (barColor) {
            case "light":
                activity.getTheme().applyStyle(R.style.OverlayBarColorLight, true);
                break;
            case "dark":
                activity.getTheme().applyStyle(R.style.OverlayBarColorDark, true);
                break;
            case "semi-light":
                activity.getTheme().applyStyle(R.style.OverlayBarColorSemiLight, true);
                break;
            case "semi-dark":
                activity.getTheme().applyStyle(R.style.OverlayBarColorSemiDark, true);
                break;
            case "black":
                activity.getTheme().applyStyle(R.style.OverlayBarColorBlack, true);
                break;
        }
    }

    public static void updateThemePrimaryColor(Activity activity) {
        int notificationBarColorOverride = getNotificationBarColor(activity);

        // Circuit breaker, keep default behavior.
        if (notificationBarColorOverride == COLOR_DEFAULT) {
            return;
        }

        updateThemePrimaryColor(notificationBarColorOverride, activity.getWindow());
        updateThemePrimaryColor(notificationBarColorOverride, activity.getActionBar());

    }

    public static void updateThemePrimaryColor(Context context, Dialog dialog) {
        int notificationBarColorOverride = getNotificationBarColor(context);

        // Circuit breaker, keep default behavior.
        if (notificationBarColorOverride == COLOR_DEFAULT) {
            return;
        }

        updateThemePrimaryColor(notificationBarColorOverride, dialog.getWindow());
        updateThemePrimaryColor(notificationBarColorOverride, dialog.getActionBar());
    }

    private static void updateThemePrimaryColor(int notificationBarColorOverride, Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(notificationBarColorOverride);
        }
    }

    private static void updateThemePrimaryColor(int notificationBarColorOverride, ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(notificationBarColorOverride));
        }
    }

    private static int getNotificationBarColor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // use accent color from system if available
            return getColor(context, "notification-bar-color", getNotificationBarColorRes(context));
        } else {
            return getColor(context, "notification-bar-color");
        }
    }

    @ColorRes
    @RequiresApi(Build.VERSION_CODES.S)
    private static int getNotificationBarColorRes(Context context) {
        if (isDarkMode(context)) {
            return android.R.color.system_neutral1_800;
        } else {
            return android.R.color.system_neutral2_100;
        }
    }

    /**
     * Get primary color.
     *
     * @param context
     * @return color from preferences
     */
    @ColorInt
    public static int getPrimaryColor(Context context) {
        if (primaryColor == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // use accent color from system if available
                primaryColor = getColor(context, "primary-color", android.R.color.system_accent1_100);
            } else {
                primaryColor = getColor(context, "primary-color");
            }
            // Transparent can't be displayed for text color, replace with light gray.
            if (primaryColor == COLOR_TRANSPARENT || primaryColor == COLOR_LIGHT_TRANSPARENT) {
                primaryColor = 0xFFBDBDBD;
            }
        }

        return primaryColor;
    }

    /**
     * Get color for notification dots.
     *
     * @param context
     * @return color from preferences
     */
    @ColorInt
    public static int getNotificationDotColor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // use accent color from system if available
            return getColor(context, "primary-color", getNotificationDotColorRes(context));
        } else {
            // fall back to primary color
            return getPrimaryColor(context);
        }
    }

    @ColorRes
    @RequiresApi(Build.VERSION_CODES.S)
    private static int getNotificationDotColorRes(Context context) {
        if (isDarkMode(context)) {
            return android.R.color.system_accent3_200;
        } else {
            return android.R.color.system_accent3_200;
        }
    }

    /**
     * Get color from preferences
     *
     * @param context
     * @param preferenceKey
     * @return color from preferences, use {@link UIColors#COLOR_DEFAULT} when saved value is {@link UIColors#COLOR_SYSTEM}
     */
    @ColorInt
    private static int getColor(Context context, String preferenceKey) {
        String colorStr = PreferenceManager.getDefaultSharedPreferences(context).getString(preferenceKey, COLOR_DEFAULT_STR);
        int color = Color.parseColor(colorStr);
        if (color == COLOR_SYSTEM) {
            return COLOR_DEFAULT;
        }
        return color;
    }

    /**
     * Get color from preferences.
     *
     * @param context
     * @param preferenceKey
     * @return color from preferences, use color given by {@code getColor} when saved value is {@link UIColors#COLOR_SYSTEM}
     */
    @ColorInt
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static int getColor(@NonNull Context context, @NonNull String preferenceKey, @ColorRes int systemColorId) {
        String colorStr = PreferenceManager.getDefaultSharedPreferences(context).getString(preferenceKey, COLOR_DEFAULT_STR);
        int color = Color.parseColor(colorStr);
        if (color == COLOR_SYSTEM) {
            color = context.getResources().getColor(systemColorId);
        }
        return color;
    }

    public static void clearPrimaryColorCache() {
        primaryColor = -1;
    }

    public static int[] getColorList() {
        return COLOR_LIST;
    }


    /**
     * @param context
     * @return icon colors from system
     */
    @ColorInt
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static int[] getIconColors(Context context) {
        Resources res = context.getResources();
        int[] colors = new int[2];
        if (isDarkMode(context)) {
            colors[0] = res.getColor(android.R.color.system_neutral1_800);
            colors[1] = res.getColor(android.R.color.system_accent1_100);
        } else {
            colors[0] = res.getColor(android.R.color.system_accent1_100);
            colors[1] = res.getColor(android.R.color.system_neutral2_700);
        }
        return colors;
    }

    /**
     * @param context
     * @return true, if dark mode is enabled
     */
    private static boolean isDarkMode(Context context) {
        Resources res = context.getResources();
        return (res.getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
    }

    /**
     * @param color, color to transform
     * @return color transformed to string
     */
    public static String colorToString(@ColorInt int color) {
        return String.format("#%08X", color);
    }

}
