package fr.neamar.summon.task;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import fr.neamar.summon.db.DBHelper;
import fr.neamar.summon.holder.ContactHolder;

public class LoadContactHoldersFromDB extends LoadHoldersFromDB<ContactHolder> {

	public LoadContactHoldersFromDB(Context context) {
		super(context, "contact://");
	}

	@Override
	protected ArrayList<ContactHolder> doInBackground(Void... params) {
		long start = System.nanoTime();

		ArrayList<ContactHolder> contacts = DBHelper.getContactsHolders(context, holderScheme);

		long end = System.nanoTime();
		Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list contacts");
		return contacts;
	}
}
