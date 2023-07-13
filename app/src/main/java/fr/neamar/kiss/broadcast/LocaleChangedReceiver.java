package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import fr.neamar.kiss.KissApplication;

public class LocaleChangedReceiver extends BroadcastReceiver {

    private static final String TAG = LocaleChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context ctx, Intent intent) {
        // Only handle system broadcasts
        if (!Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            return;
        }

        try {
            // If new locale, then reset tags to load the correct aliases
            KissApplication.getApplication(ctx).getDataHandler().resetTagsHandler();
        }
        catch(IllegalStateException e) {
            // Since Android 8.1, we're not allowed to create a new service
            // when the app is not running
            Log.w(TAG, "Unable to reset tags", e);
        }
        // Reload application list
        KissApplication.getApplication(ctx).getDataHandler().reloadApps();
    }
}
