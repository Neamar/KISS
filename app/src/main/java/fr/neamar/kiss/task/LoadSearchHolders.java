package fr.neamar.kiss.task;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadSearchHolders extends LoadHolders<SearchPojo> {

	public LoadSearchHolders(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<SearchPojo> doInBackground(Void... params) {
		return null;
	}
}
