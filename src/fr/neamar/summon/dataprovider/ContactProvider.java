package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.holder.Holder;

public class ContactProvider extends Provider {
	private ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();

	public ContactProvider(Context context) {
		super(context);
		holderScheme = "contact://";
		Thread thread = new Thread(null, initContactsList);
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.start();
	}

	protected Runnable initContactsList = new Runnable() {
		public void run() {
			long start = System.nanoTime();

			// Run query
			Cursor cur = context.getContentResolver().query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					null, null, null);

			// Prevent duplicates by keeping in memory encountered phones.
			// The string key is "phone" + "|" + "name" (so if two contacts with
			// distincts name share same number, they both get displayed
			HashMap<String, Boolean> phones = new HashMap<String, Boolean>();

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					ContactHolder contact = new ContactHolder();

					contact.lookupKey = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
					contact.id = holderScheme + contact.lookupKey;
					contact.timesContacted = Integer
							.parseInt(cur.getString(cur
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)));
					contact.name = cur
							.getString(cur
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
					contact.phone = cur
							.getString(cur
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					contact.homeNumber = contact.phone.matches("^(\\+33|0)[1-5].*");
					contact.starred = cur
							.getInt(cur
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)) != 0;
					String photoId = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
					if (photoId != null) {
						contact.icon = ContentUris.withAppendedId(
								ContactsContract.Data.CONTENT_URI,
								Long.parseLong(photoId));
					}

					if (!phones.containsKey(contact.phone + '|' + contact.name)
							&& contact.name != null) {
						contact.nameLowerCased = contact.name.toLowerCase()
								.replaceAll("[èéêë]", "e")
								.replaceAll("[ûù]", "u")
								.replaceAll("[ïî]", "i")
								.replaceAll("[àâ]", "a").replaceAll("ô", "o")
								.replaceAll("[ÈÉÊË]", "E");
						contacts.add(contact);

						phones.put(contact.phone + '|' + contact.name, true);
					}
				}
			}
			cur.close();

			long end = System.nanoTime();
			Log.i("time", Long.toString((end - start) / 1000000)
					+ " milliseconds to list contacts");
		}
	};

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		int relevance;
		String contactNameLowerCased;
		for (int i = 0; i < contacts.size(); i++) {
			ContactHolder contact = contacts.get(i);
			relevance = 0;
			contactNameLowerCased = contact.nameLowerCased;

			if (contactNameLowerCased.startsWith(query))
				relevance = 50;
			else if (contactNameLowerCased.contains(" " + query))
				relevance = 40;

			if (relevance > 0) {
				// Increase relevance according to number of times the contacts
				// was phoned :
				relevance += contact.timesContacted;
				// Increase relevance for starred contacts:
				if (contact.starred)
					relevance += 30;
				// Decrease for home numbers:
				if (contact.homeNumber)
					relevance -= 1;

				contact.displayName = contacts.get(i).name.replaceFirst("(?i)("
						+ Pattern.quote(query) + ")", "{$1}");
				contact.relevance = relevance;
				holders.add(contact);
			}
		}

		return holders;
	}

	public Holder findById(String id) {
		for (int i = 0; i < contacts.size(); i++) {
			if (contacts.get(i).id.equals(id)) {
				contacts.get(i).displayName = contacts.get(i).name;
				return contacts.get(i);
			}

		}

		return null;
	}
}
