package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ContactProvider;
import fr.neamar.kiss.pojo.ContactPojo;

public class IncomingCallHandler extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        try {
            DataHandler dataHandler = KissApplication.getDataHandler(context);
            ContactProvider contactProvider = dataHandler.getContactProvider();

            // Stop if contacts are not enabled
            if (contactProvider == null) {
                return;
            }

            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                ContactPojo contactPojo = contactProvider.findByPhone(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                if (contactPojo != null) {
                    dataHandler.addToHistory(context, contactPojo.id);
                }
            }
        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }
    }
}