package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.Pojo;

/**
 * Public interface exposed by every KISS data provider
 */
public interface IProvider {
    /**
     * Synchronously retrieve list of search results for the given query string
     *
     * @param s Some string query (usually provided by an user)
     */
    ArrayList<Pojo> getResults(String s);

    /**
     * Reload the data stored in this provider
     *
     * `"fr.neamar.summon.LOAD_OVER"` will be emitted once the reload is complete. The data provider
     * will stay usable (using it's old data) during the reload.
     */
    void reload();

    /**
     * Indicate whether this provider has already loaded it's data
     *
     * If this method returns `false` then the client may listen for the
     * `"fr.neamar.summon.LOAD_OVER"` intent for notification of when the provider is ready.
     *
     * @return Is the provider ready to process search results?
     */
    boolean isLoaded();

    /**
     * Tells whether or not this provider may be able to find the pojo with
     * specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    boolean mayFindById(String id);

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    Pojo findById(String id);
}
