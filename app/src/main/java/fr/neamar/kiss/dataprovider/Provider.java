package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.loader.LoadPojos;

public abstract class Provider<T> {
	/**
	 * Scheme used to build ids for the pojos created by this provider
	 */
	public String pojoScheme = "(none)://";

	protected LoadPojos<T> loader = null;
	protected ArrayList<T> pojos = new ArrayList<T>();

	public Provider(LoadPojos<T> loader) {
		super();
		this.loader = loader;
		this.loader.setProvider(this);
		this.pojoScheme = loader.getPojoScheme();
		loader.execute();
	}

	public abstract ArrayList<Pojo> getResults(String s);

	public void loadOver(ArrayList<T> results) {
		pojos = results;
	}

	/**
	 * Tells whether or not this provider may be able to find the pojo with
	 * specified id
	 * 
	 * @param id
	 * @return true if the provider can handle the query ; does not guarantee it
	 *         will!
	 */
	public Boolean mayFindById(String id) {
		return id.startsWith(pojoScheme);
	}

	/**
	 * Try to find a record by its id
	 * 
	 * @param id
	 * @return null if not found
	 */
	public Pojo findById(String id) {
		return null;
	}
}
