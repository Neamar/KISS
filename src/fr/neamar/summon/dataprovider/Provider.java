package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.Holder;

public abstract class Provider {
	/**
	 * Scheme used to build ids for the holders created by this provider
	 */
	public String holderScheme = "(none)://";
	
	protected Context context;

	public Provider(Context context) {
		this.context = context;
	}

	public abstract ArrayList<Holder> getResults(String s);

	/**
	 * Tells whether or not this provider may be able to find the holder with specified id
	 * 
	 * @param id
	 * @return true if the provider can handle the query ; does not guarantee it will!
	 */
	public Boolean mayFindById(String id) {
		return id.startsWith(holderScheme);
	}
	
	/**
	 * Try to find a record by its id
	 * 
	 * @param id
	 * @return null if not found
	 */
	public Holder findById(String id) {
		return null;
	}
}
