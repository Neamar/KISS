package fr.neamar.summon.lite.task;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import fr.neamar.summon.lite.holder.ContactHolder;

public class LoadContactHolders extends LoadHolders<ContactHolder>{
	
	public LoadContactHolders(Context context) {
		super(context, "contact://");
	}

	@Override
	protected ArrayList<ContactHolder> doInBackground(Void... params) {
		long start = System.nanoTime();
		
		// Run query
		Cursor cur = context
				.getContentResolver()
				.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						new String[] {
								ContactsContract.Contacts.LOOKUP_KEY,
								ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
								ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
								ContactsContract.CommonDataKinds.Phone.NUMBER,
								ContactsContract.CommonDataKinds.Phone.STARRED,
								ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
								ContactsContract.Contacts.PHOTO_ID },
						null, null, null);

		// Prevent duplicates by keeping in memory encountered phones.
		// The string key is "phone" + "|" + "name" (so if two contacts
		// with
		// distincts name share same number, they both get displayed
		HashMap<String, ArrayList<ContactHolder>> mapContacts = new HashMap<String, ArrayList<ContactHolder>>();

		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				ContactHolder contact = new ContactHolder();

				contact.lookupKey = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				contact.timesContacted = Integer.parseInt(cur.getString(cur
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)));
				contact.name = cur.getString(cur
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				contact.phone = cur.getString(cur
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				contact.homeNumber = contact.phone
						.matches("^(\\+33|0)[1-5].*");
				contact.starred = cur.getInt(cur
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)) != 0;
				contact.primary = cur.getInt(cur
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)) != 0;
				String photoId = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
				if (photoId != null) {
					contact.icon = ContentUris.withAppendedId(
							ContactsContract.Data.CONTENT_URI,
							Long.parseLong(photoId));
				}

				contact.id = holderScheme + contact.lookupKey
						+ contact.phone;

				if (contact.name != null) {
					contact.nameLowerCased = contact.name.toLowerCase()
							.replaceAll("[èéêë]", "e")
							.replaceAll("[ûù]", "u")
							.replaceAll("[ïî]", "i")
							.replaceAll("[àâ]", "a")
							.replaceAll("ô", "o")
							.replaceAll("[ÈÉÊË]", "E");

					if (mapContacts.containsKey(contact.lookupKey))
						mapContacts.get(contact.lookupKey).add(contact);
					else {
						ArrayList<ContactHolder> phones = new ArrayList<ContactHolder>();
						phones.add(contact);
						mapContacts.put(contact.lookupKey, phones);
					}
				}
			}
		}
		cur.close();
		ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();
		for (ArrayList<ContactHolder> phones : mapContacts.values()) {
			// Find primary phone and add this one.
			Boolean hasPrimary = false;
			for (int j = 0; j < phones.size(); j++) {
				ContactHolder contact = phones.get(j);
				if (contact.primary) {
					contacts.add(contact);
					hasPrimary = true;
					break;
				}
			}

			// If not available, add all.
			if (!hasPrimary) {
				for (int j = 0; j < phones.size(); j++) {
					contacts.add(phones.get(j));
				}
			}
		}
		long end = System.nanoTime();
		Log.i("time", Long.toString((end - start) / 1000000)
				+ " milliseconds to list contacts");
		return contacts;
	}	
}
