package fr.neamar.summon.record;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.summon.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RecordAdapter extends ArrayAdapter<Record> {

	/**
	 * Array list containing all the records currently displayed
	 */
	private ArrayList<Record> records = new ArrayList<Record>();
	
	
	public RecordAdapter(Context context,  int textViewResourceId,
			ArrayList<Record> records) {
		super(context, textViewResourceId, records);
		this.records = records;
	}

	public ArrayList<Record> getRecords() {
		return records;
	}

	public void setRecords(ArrayList<Record> records) {
		this.records = records;
	}



	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.item_app, null);
		}

		Record user = records.get(position);
		if (user != null) {
			TextView username = (TextView) v.findViewById(R.id.item_app_text);

			if (username != null) {
				username.setText(user.username);
			}

		}
		return v;
	}
}
