package fr.neamar.summon.lite.task;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.lite.holder.SearchHolder;

public class LoadSearchHolders extends LoadHolders<SearchHolder> {

	public LoadSearchHolders(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<SearchHolder> doInBackground(Void... params) {
		return null;
	}
}
