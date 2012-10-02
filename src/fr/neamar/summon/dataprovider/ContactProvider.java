package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.record.ContactRecord;
import fr.neamar.summon.record.Record;

public class ContactProvider extends Provider {
	private ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();

	public ContactProvider(Context context) {
		super(context);

		// Run query
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] { ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.PHOTO_ID };
		String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP
				+ " = '1'";
		String[] selectionArgs = null;
		String sortOrder = ContactsContract.Contacts.DISPLAY_NAME
				+ " COLLATE LOCALIZED ASC";

		Cursor cur = context.getContentResolver().query(uri, projection,
				selection, selectionArgs, sortOrder);

		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				ContactHolder contact = new ContactHolder();
				contact.id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				contact.contactName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				
				String photoId = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
				if(photoId != null)
				{
					contact.icon = ContentUris
							.withAppendedId(
									ContactsContract.Data.CONTENT_URI,
									Long.parseLong(photoId));
				}

				if (contact.contactName != null) {
					contact.contactNameLowerCased = contact.contactName
							.toLowerCase();
					contacts.add(contact);
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
				relevance = 100;
			else if (contactNameLowerCased.contains(" " + query))
				relevance = 50;
			else if (contactNameLowerCased.contains(query))
				relevance = 1;

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
