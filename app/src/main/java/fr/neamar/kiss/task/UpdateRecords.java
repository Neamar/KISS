package fr.neamar.kiss.task;

import java.util.ArrayList;

import android.os.AsyncTask;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.holder.Holder;
import fr.neamar.kiss.record.Record;

/**
 * AsyncTask retrieving data from the providers and updating the view
 * 
 * @author dorvaryn
 * 
 */
public class UpdateRecords extends AsyncTask<String, Void, ArrayList<Holder>> {

	private final int MAX_RECORDS = 15;
	private MainActivity activity;

	public UpdateRecords(MainActivity activity) {
		super();
		this.activity = activity;
	}

	@Override
	protected ArrayList<Holder> doInBackground(String... queries) {
		String workingOnQuery = queries[0];

		// Ask for records
		final ArrayList<Holder> holders = KissApplication.getDataHandler(activity).getResults(
				activity, workingOnQuery);

		return holders;
	}

	@Override
	protected void onPostExecute(ArrayList<Holder> holders) {
		super.onPostExecute(holders);
		activity.adapter.clear();

		if (holders != null) {
			for (int i = Math.min(MAX_RECORDS, holders.size()) - 1; i >= 0; i--) {
				activity.adapter.add(Record.fromHolder(activity, holders.get(i)));
			}
		}
		activity.resetTask();
	}
}
