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
public class UpdateRecords extends Searcher {

	private final int MAX_RECORDS = 15;

	public UpdateRecords(MainActivity activity) {
		super(activity);
	}

	@Override
	protected ArrayList<Pojo> doInBackground(String... queries) {
		String query = queries[0];

		// Ask for records
		final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getResults(
				activity, query);

		return pojos;
	}
}
