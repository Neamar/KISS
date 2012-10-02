package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.record.ContactRecord;
import fr.neamar.summon.record.Record;

public class ContactProvider extends Provider {
	private ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();

	public ContactProvider(Context context) {
		super(context);

		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String sortOrder = ContactsContract.Contacts.DISPLAY_NAME
				+ " COLLATE LOCALIZED ASC";
		Cursor cur = context.getContentResolver().query(
				uri,
				new String[] { ContactsContract.Contacts.DISPLAY_NAME,
						ContactsContract.Contacts.HAS_PHONE_NUMBER,
						ContactsContract.Contacts._ID }, null, null, sortOrder);

		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				ContactHolder contact = new ContactHolder();
				contact.id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				contact.contactName = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if(contact.contactName != null)
				{
					contact.contactNameLowerCased = contact.contactName.toLowerCase();
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
