package fr.neamar.summon.lite.task;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.preference.PreferenceManager;
import fr.neamar.summon.lite.SummonActivity;
import fr.neamar.summon.lite.SummonApplication;
import fr.neamar.summon.lite.holder.Holder;
import fr.neamar.summon.lite.record.Record;

/**
 * AsyncTask retrieving data from the providers and updating the view
 * 
 * @author dorvaryn
 * 
 */
public class UpdateRecords extends AsyncTask<String, Void, ArrayList<Holder>> {

	private final int MAX_RECORDS = 15;
	private SummonActivity activity;

	public UpdateRecords(SummonActivity activity) {
		super();
		this.activity = activity;
	}

	@Override
	protected ArrayList<Holder> doInBackground(String... queries) {
		String workingOnQuery = queries[0];

		// Ask for records
		final ArrayList<Holder> holders = SummonApplication.getDataHandler(
				activity).getResults(activity, workingOnQuery);

		return holders;
	}

	@Override
	protected void onPostExecute(ArrayList<Holder> holders) {
		super.onPostExecute(holders);
		activity.adapter.clear();
		
		if (holders != null) {
			if (PreferenceManager.getDefaultSharedPreferences(activity)
					.getBoolean("invert-ui", false)) {
				for (int i = 0; i < Math.min(MAX_RECORDS, holders.size()); i++) {
					activity.adapter.add(Record.fromHolder(activity,
							holders.get(i)));
				}
			} else {
				for (int i = Math.min(MAX_RECORDS, holders.size()) - 1; i >= 0; i--) {
					activity.adapter.add(Record.fromHolder(activity,
							holders.get(i)));
				}
			}
		}
		activity.resetTask();
	}
}
