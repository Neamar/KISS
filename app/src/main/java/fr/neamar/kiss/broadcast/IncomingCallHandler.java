package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.pojo.ContactsPojo;

public class IncomingCallHandler extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Only handle calls received
        if (!"android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            return;
        }

        try {
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
            ContactsProvider contactsProvider = dataHandler.getContactsProvider();

            // Stop if contacts are not enabled
            if (contactsProvider == null) {
                return;
            }

            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                if (phoneNumber == null) {
                    // Skipping (private call)
                    return;
                }

                ContactsPojo contactPojo = contactsProvider.findByPhone(phoneNumber);
                if (contactPojo != null) {
                    dataHandler.addToHistory(contactPojo.getHistoryId());
                }
            }
        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }
    }
}
