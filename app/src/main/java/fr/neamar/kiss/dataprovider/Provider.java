package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadPojos;
import fr.neamar.kiss.pojo.Pojo;

public abstract class Provider<T extends Pojo> {
    ArrayList<T> pojos = new ArrayList<>();
    /**
     * Scheme used to build ids for the pojos created by this provider
     */
    private String pojoScheme = "(none)://";

    Provider(LoadPojos<T> loader) {
        super();
        loader.setProvider(this);
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
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    public Boolean mayFindById(String id) {
        return id.startsWith(pojoScheme);
    }

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    public Pojo findById(String id) {
        return null;
    }
}
