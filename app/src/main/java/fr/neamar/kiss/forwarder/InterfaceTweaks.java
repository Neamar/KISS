package fr.neamar.kiss.forwarder;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.List;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.utils.ViewGroupUtils;

// Deals with any settings in the "User Interface" setting sub-screen
class InterfaceTweaks extends Forwarder {
    InterfaceTweaks(MainActivity mainActivity) {
        super(mainActivity);

        // Setting the theme needs to be done before setContentView()
        String theme = prefs.getString("theme", "transparent");
        switch (theme) {
            case "dark":
                mainActivity.setTheme(R.style.AppThemeDark);
                break;
            case "transparent":
                mainActivity.setTheme(R.style.AppThemeTransparent);
                break;
            case "semi-transparent":
                mainActivity.setTheme(R.style.AppThemeSemiTransparent);
                break;
            case "semi-transparent-dark":
                mainActivity.setTheme(R.style.AppThemeSemiTransparentDark);
                break;
            case "transparent-dark":
                mainActivity.setTheme(R.style.AppThemeTransparentDark);
                break;
            case "amoled-dark":
                mainActivity.setTheme(R.style.AppThemeAmoledDark);
                break;
        }

        UIColors.applyOverlay(mainActivity);
    }

    void onCreate() {
        UIColors.updateThemePrimaryColor(mainActivity);
        applyRoundedCorners(mainActivity);
        swapKissButtonWithMenu(mainActivity);
        tintResources(mainActivity);

        // Transparent Search and Favorites bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (prefs.getBoolean("transparent-favorites", true)) {
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
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (prefs.getBoolean("black-notification-icons", false)) {
                // Apply the flag to any view, so why not the edittext!
                mainActivity.searchEditText.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
    }

    private void applyRoundedCorners(MainActivity mainActivity) {
        if (prefs.getBoolean("pref-rounded-bars", true)) {
            mainActivity.kissBar.setBackgroundResource(R.drawable.rounded_kiss_bar);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mainActivity.findViewById(R.id.externalFavoriteBar).setBackgroundResource(R.drawable.rounded_search_bar);
                mainActivity.findViewById(R.id.searchEditLayout).setBackgroundResource(R.drawable.rounded_search_bar);
            } else {
                // Before API21, you can't access values from current theme using ?attr/
                // So we made two different drawables (#931).
                if (getSearchBackgroundColor() == Color.WHITE) {
                    mainActivity.findViewById(R.id.externalFavoriteBar).setBackgroundResource(R.drawable.rounded_search_bar_pre21_light);
                    mainActivity.findViewById(R.id.searchEditLayout).setBackgroundResource(R.drawable.rounded_search_bar_pre21_light);
                } else {
                    mainActivity.findViewById(R.id.externalFavoriteBar).setBackgroundResource(R.drawable.rounded_search_bar_pre21_dark);
                    mainActivity.findViewById(R.id.searchEditLayout).setBackgroundResource(R.drawable.rounded_search_bar_pre21_dark);
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Tinting is not properly applied pre lollipop if there is no solid background, so we need to manually set the background color
            mainActivity.kissBar.setBackgroundColor(UIColors.getPrimaryColor(mainActivity));
        }

        if (prefs.getBoolean("pref-rounded-list", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mainActivity.findViewById(R.id.resultLayout).setBackgroundResource(R.drawable.rounded_result_layout);
                // clip list content to rounded corners
                mainActivity.listContainer.setClipToOutline(true);
            } else {
                // Before API21, you can't access values from current theme using ?attr/
                // So we made two different drawables (#931).
                if (getSearchBackgroundColor() == Color.WHITE) {
                    mainActivity.findViewById(R.id.resultLayout).setBackgroundResource(R.drawable.rounded_result_layout_pre21_light);
                } else {
                    mainActivity.findViewById(R.id.resultLayout).setBackgroundResource(R.drawable.rounded_result_layout_pre21_dark);
                }
            }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
            }
            // align embeddedFavoritesBar to the left of whiteLauncherButton
            layoutParams = (RelativeLayout.LayoutParams) mainActivity.findViewById(R.id.embeddedFavoritesBar).getLayoutParams();
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.LEFT_OF, mainActivity.whiteLauncherButton.getId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutParams.addRule(RelativeLayout.END_OF, 0);
                layoutParams.addRule(RelativeLayout.START_OF, mainActivity.whiteLauncherButton.getId());
            }
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar loaderBar = mainActivity.findViewById(R.id.loaderBar);
            loaderBar.getIndeterminateDrawable().setColorFilter(primaryColorOverride, PorterDuff.Mode.SRC_IN);
        }

        // Kissbar background
        mainActivity.kissBar.getBackground().mutate().setColorFilter(primaryColorOverride, PorterDuff.Mode.SRC_IN);
    }

    private int getSearchBackgroundColor() {
        // get theme shadow color
        int[] attrs = new int[]{R.attr.searchBackgroundColor /* index 0 */};
        TypedArray ta = mainActivity.obtainStyledAttributes(attrs);
        int shadowColor = ta.getColor(0, Color.BLACK);
        ta.recycle();
        return shadowColor;
    }
}
