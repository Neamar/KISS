package fr.neamar.kiss.forwarder;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;

import fr.neamar.kiss.MainActivity;

// Deals with any settings in the "User Experience" setting sub-screen
class UXTweaksForwarder extends Forwarder {
    UXTweaksForwarder(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);

        // Lock launcher into portrait mode
        // Do it here (before initializing the view in onCreate) to make the transition as smooth as possible
        if (prefs.getBoolean("force-portrait", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }
}
