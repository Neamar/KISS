package fr.neamar.kiss.forwarder;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.NullSearcher;

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

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        //if motion movement ends
        if ((event.getAction() == MotionEvent.ACTION_CANCEL) || (event.getAction() == MotionEvent.ACTION_UP)) {
            // and minimalistic mode is enabled,
            // and we want to display history on touch
            if (prefs.getBoolean("history-hide", false) && prefs.getBoolean("history-onclick", false)) {
                // and we're currently in minimalistic mode with no results,
                // and we're not looking at the app list
                if ((mainActivity.kissBar.getVisibility() != View.VISIBLE) && (mainActivity.searchEditText.getText().toString().isEmpty())) {
                    if ((mainActivity.list.getAdapter() == null) || (mainActivity.list.getAdapter().isEmpty())) {
                        mainActivity.runTask(new HistorySearcher(mainActivity));
                    }
                }
            }
            if (prefs.getBoolean("history-hide", false) && prefs.getBoolean("favorites-hide", false)) {
                mainActivity.findViewById(R.id.favoritesBar).setVisibility(View.VISIBLE);
            }
        }

        return false;
    }

    public void updateRecords(String query) {
        if (query.length() == 0) {
            if (prefs.getBoolean("history-hide", false)) {
                mainActivity.list.setVerticalScrollBarEnabled(false);
                mainActivity.searchEditText.setHint("");
                mainActivity.runTask(new NullSearcher(mainActivity));
                //Hide default scrollview
                mainActivity.findViewById(R.id.main_empty).setVisibility(View.GONE);

            } else {
                mainActivity.list.setVerticalScrollBarEnabled(true);
                mainActivity.searchEditText.setHint(R.string.ui_search_hint);
                mainActivity.runTask(new HistorySearcher(mainActivity));
                //Show default scrollview
                mainActivity.findViewById(R.id.main_empty).setVisibility(View.VISIBLE);
            }
        }
    }
}
