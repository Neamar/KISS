package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.SearchHolder;
import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.SearchRecord;

public class SearchProvider extends Provider {

	public SearchProvider(Context context) {
		super(context);
	}

	public ArrayList<Record> getRecords(String query) {
		ArrayList<Record> records = new ArrayList<Record>();

		SearchHolder holder = new SearchHolder();
		holder.query = query;
		Record r = new SearchRecord(holder);
		records.add(r);
		return records;
	}
}
