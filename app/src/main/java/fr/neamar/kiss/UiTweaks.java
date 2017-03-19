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
import android.widget.ImageView;

public class UiTweaks {
    static String DEFAULT_GREEN = "#4caf50";

    static void updateThemePrimaryColor(Activity activity) {
        String primaryColorOverride = getPrimaryColor(activity);

        // Circuit breaker, keep default behavior.
        if (primaryColorOverride.equals(DEFAULT_GREEN)) {
            return;
        }

        int primaryColor = Color.parseColor(primaryColorOverride);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(primaryColor);
        }

        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(primaryColor));
        }
    }

    static void tintResources(MainActivity mainActivity) {
        String primaryColorOverride = getPrimaryColorForDisplay(mainActivity);

        // Circuit breaker, keep default behavior.
        if (primaryColorOverride.equals(DEFAULT_GREEN)) {
            return;
        }

        int primaryColor = Color.parseColor(primaryColorOverride);

        // Launcher button should have the main color
        ImageView launcherButton = (ImageView) mainActivity.findViewById(R.id.launcherButton);
        launcherButton.setColorFilter(primaryColor);

        // Kissbar background
        mainActivity.findViewById(R.id.main_kissbar).setBackgroundColor(primaryColor);
    }

    public static String getPrimaryColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("primary-color", DEFAULT_GREEN);
    }

    public static String getPrimaryColorForDisplay(Context context) {
        String primaryColor = getPrimaryColor(context);

        // Transparent can't be displayed for text color, replace with light gray.
        if(primaryColor.equals("#00000000")) {
            return "#BDBDBD";
        }

        return primaryColor;
    }
}
