package fr.neamar.kiss.loader;

import java.util.ArrayList;

import android.os.AsyncTask;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;

/**
 * AsyncTask retrieving data from the providers and updating the view
 * 
 * @author dorvaryn
 * 
 */
public class UpdateRecords extends AsyncTask<String, Void, ArrayList<Pojo>> {

	private final int MAX_RECORDS = 15;
	private MainActivity activity;

	public UpdateRecords(MainActivity activity) {
		super();
		this.activity = activity;
	}

	@Override
	protected ArrayList<Pojo> doInBackground(String... queries) {
		String workingOnQuery = queries[0];

		// Ask for records
		final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getResults(
				activity, workingOnQuery);

		return pojos;
	}

	@Override
	protected void onPostExecute(ArrayList<Pojo> pojos) {
		super.onPostExecute(pojos);
		activity.adapter.clear();

		if (pojos != null) {
			for (int i = Math.min(MAX_RECORDS, pojos.size()) - 1; i >= 0; i--) {
				activity.adapter.add(Result.fromPojo(activity, pojos.get(i)));
			}
		}
		activity.resetTask();
	}
}
