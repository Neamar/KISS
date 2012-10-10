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
		return 5;
	}

	public int getItemViewType(int position) {
		if (records.get(position) instanceof AppRecord)
			return 0;
		else if (records.get(position) instanceof SearchRecord)
			return 1;
		else if (records.get(position) instanceof ContactRecord)
			return 2;
		else if (records.get(position) instanceof ToggleRecord)
			return 3;
		else if (records.get(position) instanceof SettingRecord)
			return 4;
		else
			return -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return records.get(position).display(getContext(), convertView);
	}

	public void onClick(int position, View v) {
		if(position < records.size())
			records.get(position).launch(getContext(), v);
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
