package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.ToggleHolder;
import fr.neamar.summon.record.AppRecord;
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
			ToggleHolder wifiHolder = new ToggleHolder();
			wifiHolder.id = "toggle://wifi";
			wifiHolder.name = "Toggle Wifi";
			wifiHolder.nameLowerCased = wifiHolder.name.toLowerCase();
			wifiHolder.settingName = "wifi";
			toggles.add(wifiHolder);
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
				toggles.get(i).displayName = toggles.get(i).name.replaceFirst("(?i)("
						+ Pattern.quote(query) + ")", "{$1}");
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
				toggles.get(i).displayName = toggles.get(i).name;
				return new ToggleRecord(toggles.get(i));
			}

		}

		return null;
	}
}
