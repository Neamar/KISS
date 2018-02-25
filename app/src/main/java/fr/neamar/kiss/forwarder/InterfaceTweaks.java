package fr.neamar.kiss.forwarder;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.widget.ImageView;
import android.widget.ProgressBar;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;

// Deals with any settings in the "User Interface" setting sub-screen
class InterfaceTweaks extends Forwarder {
    InterfaceTweaks(MainActivity mainActivity) {
        super(mainActivity);

        // Setting the theme needs to be done before setContentView()
        String theme = prefs.getString("theme", "light");
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
        }
    }

    void onCreate() {
        UIColors.updateThemePrimaryColor(mainActivity);
        tintResources(mainActivity);

        // Transparent Search and Favorites bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (prefs.getBoolean("transparent-favorites", false)) {
                mainActivity.favoritesBar.setBackgroundColor(Color.TRANSPARENT);
            }
            if (prefs.getBoolean("transparent-search", false)) {
                mainActivity.findViewById(R.id.searchEditLayout).setBackgroundColor(Color.TRANSPARENT);
                mainActivity.searchEditText.setBackgroundColor(Color.TRANSPARENT);
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

    private void tintResources(MainActivity mainActivity) {
        String primaryColorOverride = UIColors.getPrimaryColor(mainActivity);

        // Circuit breaker, keep default behavior.
        if (primaryColorOverride.equals(UIColors.COLOR_DEFAULT)) {
            return;
        }

        int primaryColor = Color.parseColor(primaryColorOverride);

        // Launcher button should have the main color
        ImageView launcherButton = (ImageView) mainActivity.findViewById(R.id.launcherButton);
        launcherButton.setColorFilter(primaryColor);
        ProgressBar loaderBar = (ProgressBar) mainActivity.findViewById(R.id.loaderBar);
        loaderBar.getIndeterminateDrawable().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN);

        // Kissbar background
        mainActivity.kissBar.setBackgroundColor(primaryColor);
    }
}
