package fr.neamar.kiss.task;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.holder.SearchHolder;

public class LoadSearchHolders extends LoadHolders<SearchHolder> {

	public LoadSearchHolders(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<SearchHolder> doInBackground(Void... params) {
		return null;
	}
}
