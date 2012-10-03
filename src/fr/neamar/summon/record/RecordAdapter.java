package fr.neamar.summon.record;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class RecordAdapter extends ArrayAdapter<Record> {

	/**
	 * Array list containing all the records currently displayed
	 */
	private ArrayList<Record> records = new ArrayList<Record>();

	private Context context;

	public RecordAdapter(Context context, int textViewResourceId,
			ArrayList<Record> records) {
		super(context, textViewResourceId, records);
		this.context = context;
		this.records = records;
	}

	public int getViewTypeCount() {
		return 3;
	}

	public int getItemViewType(int position) {
		if (records.get(position) instanceof AppRecord)
			return 0;
		else if (records.get(position) instanceof SearchRecord)
			return 1;
		else if (records.get(position) instanceof ContactRecord)
			return 2;
		else
			return -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Requires optimization ! View
		// http://stackoverflow.com/questions/10270252/why-does-the-android-view-api-care-about-an-arrayadapters-getviewtypecount

		return records.get(position).display(getContext(), convertView);
	}

	public void onClick(int position) {
		Log.i("log", "Launching " + records.get(position).holder.id);
		
		// Save in history
		// Move every item one step down
		SharedPreferences prefs = context.getSharedPreferences("history",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		for (int k = 30; k >= 0; k--) {
			String id = prefs.getString(Integer.toString(k), "(none)");
			if (!id.equals("(none)"))
				ed.putString(Integer.toString(k + 1), id);
		}
		ed.putString("0", records.get(position).holder.id);
		ed.commit();
		
		records.get(position).launch(getContext());
	}
}
