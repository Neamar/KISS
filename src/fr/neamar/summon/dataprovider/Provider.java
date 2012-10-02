package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import fr.neamar.summon.record.Record;

public abstract class Provider {	
	public Provider() {
	}

	public abstract ArrayList<Record> getRecords(String s);
}
