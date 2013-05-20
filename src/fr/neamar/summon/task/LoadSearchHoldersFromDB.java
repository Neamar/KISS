package fr.neamar.summon.task;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.summon.holder.SearchHolder;

public class LoadSearchHoldersFromDB extends LoadHoldersFromDB<SearchHolder> {

	public LoadSearchHoldersFromDB(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<SearchHolder> doInBackground(Void... params) {
		return null;
	}
}
