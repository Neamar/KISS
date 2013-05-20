package fr.neamar.summon.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.SearchHolder;
import fr.neamar.summon.task.LoadSearchHolders;
import fr.neamar.summon.task.LoadSearchHoldersFromDB;

public class SearchProvider extends Provider<SearchHolder> {

	public SearchProvider(Context context) {
		super(new LoadSearchHolders(context));
	}

    private SearchProvider(LoadSearchHoldersFromDB loader) {
        super(loader);
    }

    public static SearchProvider fromDB(Context context){
        return new SearchProvider(new LoadSearchHoldersFromDB(context));
    }

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		SearchHolder holder = new SearchHolder();
		holder.query = query;
		holder.relevance = 10;
		holders.add(holder);
		return holders;
	}

    @Override
    public void saveProvider(Context context) {
        //Nothing to save here
    }
}
