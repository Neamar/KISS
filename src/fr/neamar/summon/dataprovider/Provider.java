package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.task.LoadHolders;

public abstract class Provider<T> {
	/**
	 * Scheme used to build ids for the holders created by this provider
	 */
	public String holderScheme = "(none)://";
	
	protected LoadHolders<T> loader = null;
	protected ArrayList<T> holders = new ArrayList<T>();
	
	public Provider(LoadHolders<T> loader) {
		super();
		this.loader = loader;
		this.loader.setProvider(this);
		this.holderScheme = loader.getHolderScheme();
		loader.execute();
	}

	public abstract ArrayList<Holder> getResults(String s);
	
	public void loadOver(ArrayList<T> results){
		holders = results;
	}
	/**
	 * Tells whether or not this provider may be able to find the holder with
	 * specified id
	 * 
	 * @param id
	 * @return true if the provider can handle the query ; does not guarantee it
	 *         will!
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
