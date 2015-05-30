package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.loader.LoadSearchHolders;

public class SearchProvider extends Provider<SearchPojo> {

	public SearchProvider(Context context) {
		super(new LoadSearchHolders(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> pojos = new ArrayList<Pojo>();

		SearchPojo holder = new SearchPojo();
		holder.query = query;
		holder.relevance = 10;
		pojos.add(holder);
		return pojos;
	}
}
