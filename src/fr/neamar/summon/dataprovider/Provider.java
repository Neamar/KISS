package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.Holder;

public abstract class Provider {
	protected Context context;

	public Provider(Context context) {
		this.context = context;
	}

	public abstract ArrayList<Holder> getResults(String s);

	/**
	 * Try to find a record by its id
	 * 
	 * @param id
	 * @return null if not found
	 */
	public Holder findById(String id) {
		// TODO : watch scheme to avoid for loop
		return null;
	}
}
