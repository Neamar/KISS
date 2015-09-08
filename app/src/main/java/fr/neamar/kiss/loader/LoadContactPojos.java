package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactPojo;

public class LoadContactPojos extends LoadPojos<ContactPojo> {

    public LoadContactPojos(Context context) {
        super(context, "contact://");
    }

    @Override
    protected ArrayList<ContactPojo> doInBackground(Void... params) {
        Pattern homePattern = Pattern.compile("^\\+33\\s?[1-5]");

        long start = System.nanoTime();

        // Run query
        Cursor cur = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                        ContactsContract.Contacts.PHOTO_ID}, null, null, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC");

        // Prevent duplicates by keeping in memory encountered phones.
        // The string key is "phone" + "|" + "name" (so if two contacts
        // with distinct name share same number, they both get displayed)
        HashMap<String, ArrayList<ContactPojo>> mapContacts = new HashMap<>();

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                ContactPojo contact = new ContactPojo();

                contact.lookupKey = cur.getString(cur
                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                contact.timesContacted = Integer.parseInt(cur.getString(cur
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)));
                contact.name = cur.getString(cur
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                contact.phone = PhoneNormalizer.normalizePhone(cur.getString(cur
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                if (contact.phone == null) {
                    contact.phone = "";
                }

                contact.homeNumber = homePattern.matcher(contact.phone).lookingAt();

                contact.starred = cur.getInt(cur
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)) != 0;
                contact.primary = cur.getInt(cur
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)) != 0;
                String photoId = cur.getString(cur
                        .getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                if (photoId != null) {
                    contact.icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                            Long.parseLong(photoId));
                }

                contact.id = pojoScheme + contact.lookupKey + contact.phone;

                if (contact.name != null) {
                    contact.nameLowerCased = StringNormalizer.normalize(contact.name);

                    if (mapContacts.containsKey(contact.lookupKey))
                        mapContacts.get(contact.lookupKey).add(contact);
                    else {
                        ArrayList<ContactPojo> phones = new ArrayList<>();
                        phones.add(contact);
                        mapContacts.put(contact.lookupKey, phones);
                    }
                }
            }
        }
        cur.close();

        ArrayList<ContactPojo> contacts = new ArrayList<>();

        Pattern phoneFormatter = Pattern.compile("[ \\.\\(\\)]");
        for (ArrayList<ContactPojo> phones : mapContacts.values()) {
            // Find primary phone and add this one.
            Boolean hasPrimary = false;
            for (ContactPojo contact : phones) {
                if (contact.primary) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If not available, add all (excluding duplicates).
            if (!hasPrimary) {
                HashMap<String, Boolean> added = new HashMap<>();
                for (ContactPojo contact : phones) {
                    String uniqueKey = phoneFormatter.matcher(contact.phone).replaceAll("");
                    uniqueKey = uniqueKey.replaceAll("^\\+33", "0");
                    uniqueKey = uniqueKey.replaceAll("^\\+1", "0");
                    if (!added.containsKey(uniqueKey)) {
                        added.put(uniqueKey, true);
                        contacts.add(contact);
                    }
                }
            }
        }
        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list contacts");
        return contacts;
    }
}
