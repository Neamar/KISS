package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.SearchHolder;
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
		r.relevance = 10;
		records.add(r);
		return records;
	}
}
