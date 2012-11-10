package fr.neamar.summon.record;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import fr.neamar.summon.lite.QueryInterface;

public class RecordAdapter extends ArrayAdapter<Record> {

	/**
	 * Array list containing all the records currently displayed
	 */
	private ArrayList<Record> records = new ArrayList<Record>();
	
	private QueryInterface parent;

	public RecordAdapter(Context context, QueryInterface parent, int textViewResourceId,
			ArrayList<Record> records) {
		super(context, textViewResourceId, records);
		
		this.parent = parent;
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
		records.get(position).launch(getContext(), v);
		
		parent.launchOccured();
	}
}
