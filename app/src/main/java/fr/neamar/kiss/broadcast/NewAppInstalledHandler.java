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
        KissApplication.resetDataHandler(ctx);
    }

}
