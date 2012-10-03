package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.record.Record;

public abstract class Provider {	
	protected Context context;
	
	public Provider(Context context) {
		this.context = context;
	}

	public abstract ArrayList<Record> getRecords(String s);
	
	/**
	 * Try to find a record by its id
	 * @param id
	 * @return null if not found
	 */
	public Record findById(String id)
	{
		return null;
	}
}
