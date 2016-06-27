package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;

public class LoadContactsPojos extends LoadPojos<ContactsPojo> {

    public LoadContactsPojos(Context context) {
        super(context, "contact://");
    }

    private Pattern getMobileNumberPattern() {
        InputStream inputStream = context.getResources().openRawResource(R.raw.phone_number_textable);
        StringBuilder mobileDetectionRegex = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while ((line = reader.readLine()) != null)
                mobileDetectionRegex.append(line);

            return Pattern.compile(mobileDetectionRegex.toString());
        } catch (IOException ioex) {
            return null;
        }
    }

    @Override
    protected ArrayList<ContactsPojo> doInBackground(Void... params) {
        Pattern mobileNumberPattern = getMobileNumberPattern();

        long start = System.nanoTime();

        // Run query
        Cursor cur = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                        ContactsContract.Contacts.PHOTO_ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID}, null, null, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC");

        // Prevent duplicates by keeping in memory encountered phones.
        // The string key is "phone" + "|" + "name" (so if two contacts
        // with distinct name share same number, they both get displayed)
        Map<String, ArrayList<ContactsPojo>> mapContacts = new HashMap<>();

        if (cur != null) {
            if (cur.getCount() > 0) {
                while (cur.moveToNext()) {
                    ContactsPojo contact = new ContactsPojo();

                    contact.lookupKey = cur.getString(cur
                            .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

                    contact.timesContacted = Integer.parseInt(cur.getString(cur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)));
                    contact.setName(cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));

                    contact.phone = PhoneNormalizer.normalizePhone(cur.getString(cur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    if (contact.phone == null) {
                        contact.phone = "";
                    }
                    contact.phoneSimplified = contact.phone.replaceAll("[-.(): ]", "");

                    contact.homeNumber = mobileNumberPattern == null ||
                            !mobileNumberPattern.matcher(contact.phoneSimplified).lookingAt();
                    Log.d("issue-480", contact.name + " --- " + contact.phone +
                            " --- " + contact.phoneSimplified +
                            " --- " + (contact.homeNumber ? "land line": "mobile"));

                    contact.starred = cur.getInt(cur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)) != 0;
                    contact.primary = cur.getInt(cur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) != 0;
                    String photoId = cur.getString(cur
                            .getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                    if (photoId != null) {
                        contact.icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                    }

                    contact.id = pojoScheme + contact.lookupKey + contact.phone;

                    if (contact.name != null) {
                        contact.nameNormalized = StringNormalizer.normalize(contact.name);

                        if (mapContacts.containsKey(contact.lookupKey))
                            mapContacts.get(contact.lookupKey).add(contact);
                        else {
                            ArrayList<ContactsPojo> phones = new ArrayList<>();
                            phones.add(contact);
                            mapContacts.put(contact.lookupKey, phones);
                        }
                    }
                }
            }
            cur.close();
        }

        // Retrieve contacts' nicknames
        Cursor nickCursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Nickname.NAME,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                null);

        if (nickCursor != null) {
            if (nickCursor.getCount() > 0) {
                while (nickCursor.moveToNext()) {
                    String lookupKey = nickCursor.getString(
                            nickCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
                    String nick = nickCursor.getString(
                            nickCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));

                    if (nick != null && lookupKey != null && mapContacts.containsKey(lookupKey)) {
                        for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                            contact.setNickname(nick);
                        }
                    }
                }
            }
            nickCursor.close();
        }

        ArrayList<ContactsPojo> contacts = new ArrayList<>();

        Pattern phoneFormatter = Pattern.compile("[ \\.\\(\\)]");
        for (List<ContactsPojo> phones : mapContacts.values()) {
            // Find primary phone and add this one.
            Boolean hasPrimary = false;
            for (ContactsPojo contact : phones) {
                if (contact.primary) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If not available, add all (excluding duplicates).
            if (!hasPrimary) {
                Map<String, Boolean> added = new HashMap<>();
                for (ContactsPojo contact : phones) {
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
