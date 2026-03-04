package fr.neamar.kiss.forwarder;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.List;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.result.TagDummyResult;
import fr.neamar.kiss.utils.ViewGroupUtils;

// Deals with any settings in the "User Interface" setting sub-screen
public class InterfaceTweaks extends Forwarder {
    InterfaceTweaks(MainActivity mainActivity) {
        super(mainActivity);

        // Setting the theme needs to be done before setContentView()
        applyTheme(mainActivity, prefs);
    }

    public static void applyTheme(Activity act, SharedPreferences prefs) {
        String theme = getTheme(prefs);
        switch (theme) {
            case "dark":
                act.setTheme(R.style.AppThemeDark);
                break;
            case "transparent":
                act.setTheme(R.style.AppThemeTransparent);
                break;
            case "semi-transparent":
                act.setTheme(R.style.AppThemeSemiTransparent);
                break;
            case "semi-transparent-dark":
                act.setTheme(R.style.AppThemeSemiTransparentDark);
                break;
            case "transparent-dark":
                act.setTheme(R.style.AppThemeTransparentDark);
                break;
            case "amoled-dark":
                act.setTheme(R.style.AppThemeAmoledDark);
                break;
        }

        UIColors.applyOverlay(act, prefs);

        switch (prefs.getString("results-size", "")) {
            case "smallest":
                act.getTheme().applyStyle(R.style.OverlayResultSizeSmallest, true);
                break;
            case "small":
                act.getTheme().applyStyle(R.style.OverlayResultSizeSmall, true);
                break;
            case "medium":
                act.getTheme().applyStyle(R.style.OverlayResultSizeMedium, true);
                break;
            case "large":
                act.getTheme().applyStyle(R.style.OverlayResultSizeLarge, true);
                break;
            case "largest":
                act.getTheme().applyStyle(R.style.OverlayResultSizeLargest, true);
                break;
            case "default":
            default:
                act.getTheme().applyStyle(R.style.OverlayResultSizeStandard, true);
                break;
        }

        if (prefs.getBoolean("icons-hide", false)) {
            act.getTheme().applyStyle(R.style.OverlayResultSizeNoIcons, true);
        }

        applySystemBarInsets(act.getWindow().getDecorView());
    }

    public static void applySettingsTheme(Activity act, SharedPreferences prefs) {
        String theme = getTheme(prefs);
        switch (theme) {
            case "dark":
                act.setTheme(R.style.SettingThemeDark);
                break;
            case "transparent":
                act.setTheme(R.style.SettingTheme);
                break;
            case "semi-transparent":
                act.setTheme(R.style.SettingTheme);
                break;
            case "semi-transparent-dark":
                act.setTheme(R.style.SettingThemeDark);
                break;
            case "transparent-dark":
                act.setTheme(R.style.SettingThemeDark);
                break;
            case "amoled-dark":
                act.setTheme(R.style.SettingThemeAmoledDark);
                break;
            default: // "light"
                act.setTheme(R.style.SettingTheme);
                break;
        }
        UIColors.applyAccentColor(act);
        UIColors.updateThemePrimaryColor(act);
        applySystemBarInsets(act.getWindow().getDecorView());
    }

    public static void setDefaultNightMode(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = getTheme(prefs);
        switch (theme) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                break;
            case "transparent":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
                break;
            case "semi-transparent":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
                break;
            case "semi-transparent-dark":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                break;
            case "transparent-dark":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                break;
            case "amoled-dark":
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                break;
            default: // "light"
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
                break;
        }
    }

    private static String getTheme(SharedPreferences prefs) {
        return prefs.getString("theme", "transparent");
    }

    private static void applySystemBarInsets(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            view.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return WindowInsets.CONSUMED;
            });
        }
    }

    void onCreate() {
        UIColors.clearPrimaryColorCache();
        UIColors.updateThemePrimaryColor(mainActivity);
        applyRoundedCorners(mainActivity);
        swapKissButtonWithMenu(mainActivity);
        tintResources(mainActivity);

        // Transparent Search and Favorites bar
        if (prefs.getBoolean("transparent-favorites", true) && isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar.setBackgroundResource(android.R.color.transparent);
        }
        if (prefs.getBoolean("transparent-search", false)) {
            mainActivity.findViewById(R.id.searchEditLayout).setBackgroundResource(android.R.color.transparent);
            mainActivity.searchEditText.setBackgroundResource(android.R.color.transparent);

            // get theme shadow color
            int shadowColor = getSearchBackgroundColor();

            // make shadow color intense
            float[] hsv = new float[3];
            Color.colorToHSV(shadowColor, hsv);
            // if color is close to black, make it black
            hsv[2] = hsv[2] < 0.5f ? 0f : 1f;
            shadowColor = Color.HSVToColor(hsv);
            mainActivity.searchEditText.setShadowLayer(3, 1, 2, shadowColor);
        }

        if (prefs.getBoolean("pref-hide-search-bar-hint", false)) {
            mainActivity.searchEditText.setHint("");
        }

        if (prefs.getBoolean("large-result-list-margins", false)) {
            ViewGroup.LayoutParams params = mainActivity.listContainer.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;

                int size = mainActivity.getResources().getDimensionPixelSize(R.dimen.list_margin_horizontal_large);
                marginLayoutParams.setMargins(size, marginLayoutParams.topMargin, size, marginLayoutParams.bottomMargin);
                marginLayoutParams.setMarginStart(size);
                marginLayoutParams.setMarginEnd(size);
            }
        }
    }

    void onResume() {
        boolean largeSearchBar = prefs.getBoolean("large-search-bar", false);
        Resources res = mainActivity.getResources();
        int searchHeight;
        if (largeSearchBar) {
            searchHeight = res.getDimensionPixelSize(R.dimen.large_bar_height);
        } else {
            searchHeight = res.getDimensionPixelSize(R.dimen.bar_height);
        }

        mainActivity.findViewById(R.id.searchEditLayout).getLayoutParams().height = searchHeight;
        mainActivity.kissBar.getLayoutParams().height = searchHeight;
        mainActivity.findViewById(R.id.embeddedFavoritesBar).getLayoutParams().height = searchHeight;

        // Large favorite bar
        if (prefs.getBoolean("large-favorites-bar", false) && isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar.getLayoutParams().height = res.getDimensionPixelSize(R.dimen.large_favorite_height);
        }
    }

    private void applyRoundedCorners(MainActivity mainActivity) {
        if (prefs.getBoolean("pref-rounded-bars", true)) {
            mainActivity.kissBar.setBackgroundResource(R.drawable.rounded_kiss_bar);
            mainActivity.findViewById(R.id.externalFavoriteBar).setBackgroundResource(R.drawable.rounded_search_bar);
            mainActivity.findViewById(R.id.searchEditLayout).setBackgroundResource(R.drawable.rounded_search_bar);
        }

        if (prefs.getBoolean("pref-rounded-list", false)) {
            mainActivity.findViewById(R.id.resultLayout).setBackgroundResource(R.drawable.rounded_result_layout);
            // clip list content to rounded corners
            mainActivity.listContainer.setClipToOutline(true);
        }
    }

    private void swapKissButtonWithMenu(MainActivity mainActivity) {
        if (prefs.getBoolean("pref-swap-kiss-button-with-menu", false)) {
            // swap the content from the left and right button wrappers
            List<View> leftHandSideViews = ViewGroupUtils.removeAndGetDirectChildren(mainActivity.rightHandSideButtonsWrapper);
            List<View> rightHandSideViews = ViewGroupUtils.removeAndGetDirectChildren(mainActivity.leftHandSideButtonsWrapper);
            ViewGroupUtils.addAllViews(mainActivity.rightHandSideButtonsWrapper, rightHandSideViews);
            ViewGroupUtils.addAllViews(mainActivity.leftHandSideButtonsWrapper, leftHandSideViews);
            // align whiteLauncherButton to the right
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mainActivity.whiteLauncherButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 1);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
            // align embeddedFavoritesBar to the left of whiteLauncherButton
            layoutParams = (RelativeLayout.LayoutParams) mainActivity.findViewById(R.id.embeddedFavoritesBar).getLayoutParams();
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.LEFT_OF, mainActivity.whiteLauncherButton.getId());
            layoutParams.addRule(RelativeLayout.END_OF, 0);
            layoutParams.addRule(RelativeLayout.START_OF, mainActivity.whiteLauncherButton.getId());
        }
    }

    private void tintResources(MainActivity mainActivity) {
        int primaryColorOverride = UIColors.getPrimaryColor(mainActivity);

        // Circuit breaker, keep default behavior.
        if (primaryColorOverride == UIColors.COLOR_DEFAULT) {
            return;
        }

        // Launcher button should have the main color
        ImageView launcherButton = mainActivity.findViewById(R.id.launcherButton);
        launcherButton.setColorFilter(primaryColorOverride);

        // Kissbar background
        mainActivity.kissBar.getBackground().mutate().setColorFilter(primaryColorOverride, PorterDuff.Mode.SRC_IN);
    }

    private int getSearchBackgroundColor() {
        // get theme shadow color
        @StyleableRes int[] attrs = new int[]{R.attr.searchBackgroundColor};
        TypedArray ta = mainActivity.obtainStyledAttributes(attrs);
        int shadowColor = ta.getColor(0, Color.BLACK);
        ta.recycle();
        return shadowColor;
    }

    private boolean isExternalFavoriteBarEnabled() {
        return prefs.getBoolean("enable-favorites-bar", true);
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        TagDummyResult.resetShape();
        UIColors.clearPrimaryColorCache();
        UIColors.updateThemePrimaryColor(mainActivity);
        tintResources(mainActivity);
    }
}
