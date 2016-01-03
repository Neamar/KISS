package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.neamar.kiss.KissApplication;

/**
 * Created by nmitsou on 22.11.15.
 */
public class NewCalendarEventHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        KissApplication.getDataHandler(context).refreshEvents();
    }
}
