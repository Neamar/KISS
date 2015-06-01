package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {
    private static final int MAX_RECORDS = 15;

    private final String query;

    public QuerySearcher(MainActivity activity, String query) {
        super(activity);
        this.query = query;
    }


    @Override
    protected List<Pojo> doInBackground(Void... voids) {
        // Ask for records
        final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getResults(
                activity, query);

        // Trim items
        if (pojos.size() > MAX_RECORDS) {
            return pojos.subList(0, MAX_RECORDS);
        }

        return pojos;
    }
}
