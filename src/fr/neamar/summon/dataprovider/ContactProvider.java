package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.SparseArray;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.record.ContactRecord;
import fr.neamar.summon.record.Record;

public class ContactProvider extends Provider {
	private ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();

	public ContactProvider(Context context) {
		super(context);

		// Run query
		Cursor cur = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
				null, null);

		// Prevent duplicates by keeping in memory encountered phones.
		// The string key is "phone" + "|" + "name" (so if two contacts with
		// distincts name share same number, they both get displayed
		HashMap<String, Boolean> phones = new HashMap<String, Boolean>();

		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				ContactHolder contact = new ContactHolder();
				
				contact.id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				contact.contactName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				contact.contactPhone = cur
						.getString(cur
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

				String photoId = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
				if (photoId != null) {
					contact.icon = ContentUris.withAppendedId(
							ContactsContract.Data.CONTENT_URI,
							Long.parseLong(photoId));
				}

				if (!phones.containsKey(contact.contactPhone + '|'
						+ contact.contactName)
						&& contact.contactName != null) {
					contact.contactNameLowerCased = contact.contactName
							.toLowerCase();
					contacts.add(contact);

					phones.put(
							contact.contactPhone + '|' + contact.contactName,
							true);
				}
			}
		}
		cur.close();
	}

	public ArrayList<Record> getRecords(String query) {
		query = query.toLowerCase();

		ArrayList<Record> records = new ArrayList<Record>();

		int relevance;
		String contactNameLowerCased;
		for (int i = 0; i < contacts.size(); i++) {
			relevance = 0;
			contactNameLowerCased = contacts.get(i).contactNameLowerCased;
			
			if (contactNameLowerCased.startsWith(query))
				relevance = 50;
			else if (contactNameLowerCased.contains(" " + query))
				relevance = 40;

			if (relevance > 0) {
				contacts.get(i).displayContactName = contacts.get(i).contactName
						.replaceFirst("(?i)(" + Pattern.quote(query) + ")",
								"{$1}");
				Record r = new ContactRecord(contacts.get(i));
				r.relevance = relevance;
				records.add(r);
			}
		}

		return records;
	}
}
