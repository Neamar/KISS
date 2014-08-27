package fr.neamar.summon.task;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.SearchHolder;

public class LoadSearchHolders extends LoadHolders<SearchHolder> {

	public LoadSearchHolders(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<SearchHolder> doInBackground(Void... params) {
		return null;
	}
}
