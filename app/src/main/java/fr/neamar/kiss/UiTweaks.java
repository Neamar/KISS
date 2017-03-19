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

/**
 * Created by neamar on 19/03/17.
 */

public class UiTweaks {
    static void updateThemePrimaryColor(Activity activity) {
        String primaryColorOverride = getPrimaryColor(activity);

        // Circuit breaker, keep default behavior.
        if (primaryColorOverride.equals("#4caf50")) {
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
        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(primaryColor));
        }
    }

    public static String getPrimaryColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("primary-color", "#4caf50");
    }
}
