package fr.neamar.summon.record;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class RecordAdapter extends ArrayAdapter<Record> {

	/**
	 * Array list containing all the records currently displayed
	 */
	private ArrayList<Record> records = new ArrayList<Record>();

	public RecordAdapter(Context context, int textViewResourceId,
			ArrayList<Record> records) {
		super(context, textViewResourceId, records);
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
		if(position < records.size())
			records.get(position).launch(getContext());
		else
		{
			//Click on "beta notification"
			Intent i = new Intent(Intent.ACTION_SENDTO);
			i.setType("text/plain");
			i.setData(Uri.parse("mailto:summon@neamar.fr"));
			i.putExtra(Intent.EXTRA_SUBJECT, "Summon");
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getContext().startActivity(i);
		}
	}
}
