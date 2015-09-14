package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.neamar.kiss.KissApplication;

/**
 * This class gets called when an application is created or removed on the
 * system
 * <p/>
 * We then recreate our data set.
 *
 * @author dorvaryn
 */
public class NewAppInstalledHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if(intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            // This is an upgrade, and we'll receive a new app install soon.
            // No need to reset the datahandler twice.
            return;
        }
        KissApplication.resetDataHandler(ctx);
    }

}
