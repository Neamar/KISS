package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.SearchHolder;
import fr.neamar.summon.task.LoadSearchHolders;

public class SearchProvider extends Provider<SearchHolder> {

	public SearchProvider(Context context) {
		super(new LoadSearchHolders(context));
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		SearchHolder holder = new SearchHolder();
		holder.query = query;
		holder.relevance = 10;
		holders.add(holder);
		return holders;
	}
}
