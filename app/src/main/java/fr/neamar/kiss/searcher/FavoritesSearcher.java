package fr.neamar.kiss.searcher;

import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Retrieve pojos from history
 */
public class FavoritesSearcher extends Searcher {
    private static final int MAX_RECORDS = 25;

    public FavoritesSearcher(MainActivity activity) {
        super(activity);
    }

    @Override
    protected List<Pojo> doInBackground(Void... voids) {
        // Ask for records

        return KissApplication.getDataHandler(activity).getFavorites(activity);
    }
}
