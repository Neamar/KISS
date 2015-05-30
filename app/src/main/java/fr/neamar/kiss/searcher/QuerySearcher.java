package fr.neamar.kiss.searcher;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 *
 */
public class QuerySearcher extends Searcher {
	public final String query;

	public QuerySearcher(MainActivity activity, String query) {
		super(activity);
		this.query = query;
	}


	@Override
	protected ArrayList<Pojo> doInBackground(Void... voids) {
		// Ask for records
		final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getResults(
				activity, query);

		return pojos;
	}
}
