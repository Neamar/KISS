package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.summon.lite.R;
import fr.neamar.summon.holder.ToggleHolder;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.ToggleRecord;

public class ToggleProvider extends Provider {
	private ArrayList<ToggleHolder> toggles = new ArrayList<ToggleHolder>();

	public ToggleProvider(Context context) {
		super(context);

		Thread thread = new Thread(null, initTogglesList);
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.start();
	}

	protected Runnable initTogglesList = new Runnable() {
		public void run() {
			toggles.add(createHolder("Wifi", "wifi", R.drawable.toggle_wifi));
			//toggles.add(createHolder("GPS", "gps", R.drawable.toggle_gps));
			toggles.add(createHolder("Bluetooth", "bluetooth", R.drawable.toggle_bluetooth));
			toggles.add(createHolder("Silent", "silent", R.drawable.toggle_silent));
			//toggles.add(createHolder("Mobile network data", "data", R.drawable.toggle_data));
		}

		private ToggleHolder createHolder(String name, String settingName, int resId) {
			ToggleHolder holder = new ToggleHolder();
			holder.id = "toggle://" + name.toLowerCase();
			holder.name = "Toggle: " + name;
			holder.nameLowerCased = holder.name.toLowerCase();
			holder.settingName = settingName;
			holder.icon = resId;
			
			return holder;
		}
	};

	public ArrayList<Record> getRecords(String query) {
		query = query.toLowerCase();

		ArrayList<Record> records = new ArrayList<Record>();

		int relevance;
		String toggleNameLowerCased;
		for (int i = 0; i < toggles.size(); i++) {
			relevance = 0;
			toggleNameLowerCased = toggles.get(i).nameLowerCased;
			if (toggleNameLowerCased.startsWith(query))
				relevance = 100;
			else if (toggleNameLowerCased.contains(" " + query))
				relevance = 50;
			else if (toggleNameLowerCased.contains(query))
				relevance = 1;

			if (relevance > 0) {
				toggles.get(i).displayName = toggles.get(i).name.replace(
						"Toggle:", "<small><small>Toggle:</small></small>").replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");

				Record r = new ToggleRecord(toggles.get(i));
				r.relevance = relevance;
				records.add(r);
			}
		}

		return records;
	}

	public Record findById(String id) {
		for (int i = 0; i < toggles.size(); i++) {
			if (toggles.get(i).id.equals(id)) {
				toggles.get(i).displayName = toggles.get(i).name.replace(
						"Toggle:", "<small><small>Toggle:</small></small>");
				return new ToggleRecord(toggles.get(i));
			}

		}

		return null;
	}
}
