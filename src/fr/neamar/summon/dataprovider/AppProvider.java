package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;

public class AppProvider extends Provider{
	public AppProvider() {
		super();
	}

	public ArrayList<Record> getRecords(String s)
	{
		ArrayList<Record> records = new ArrayList<Record>();
		
		Record r = new AppRecord("fr.omnilogie");
		records.add(r);
		return records;
	}
}
