package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;

import java.util.ArrayList;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ContactProvider;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.pojo.ContactPojo;

public class IncomingSmsHandler extends BroadcastReceiver {

    private SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Only handle SMS received
        if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }

        // Stop if contacts are not enabled
        DataHandler dataHandler = KissApplication.getDataHandler(context);
        ContactProvider contactProvider = dataHandler.getContactProvider();
        if (contactProvider == null) {
            // Contacts have been disabled from settings
            return;
        }

        // Get the SMS message passed in, if any
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        // Retrieve the SMS message received.
        // Since we're not interested in content, we can safely discard
        // all records but the first one
        Object[] pdus = (Object[]) bundle.get("pdus");
        SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[0]);

        // We need to normalize, the Intent send the phone without any formatting and with international code,
        // Contacts are stored with formatting and sometimes without code
        // Thus, normalizing them allow for simple comparison
        String phoneNumber = PhoneNormalizer.normalizePhone(msg.getOriginatingAddress());

        // Retrieve the contact using fast PhoneLookup API
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cur = context.getContentResolver().query(uri, new String[]{
                ContactsContract.PhoneLookup.LOOKUP_KEY,
        }, null, null, null);

        // Unknown contact
        if (cur.getCount() == 0) {
            return;
        }

        cur.moveToNext();
        String lookupKey = cur.getString(cur
                .getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY));
        cur.close();

        // Now, retrieve the contact by its lookup key on our contactProvider
        ArrayList<ContactPojo> contacts = contactProvider.findByLookupKey(lookupKey);
        for (int j = 0; j < contacts.size(); j++) {
            ContactPojo contactPojo = contacts.get(j);
            String contactPhoneNumber = PhoneNormalizer.normalizePhone(contactPojo.phone);
            // It's a match!
            if (contactPhoneNumber.equals(phoneNumber)) {
                dataHandler.addToHistory(context, contactPojo.id);
                return;
            }
        }

    }
}