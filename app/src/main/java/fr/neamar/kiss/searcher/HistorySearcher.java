package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Retrieve pojos from history
 */
public class HistorySearcher extends Searcher {
    private final int MAX_RECORDS = 20;

    public HistorySearcher(MainActivity activity) {
        super(activity);
    }

    @Override
    protected List<Pojo> doInBackground(Void... voids) {
        // Ask for records
        final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getHistory(activity, MAX_RECORDS);

        return pojos;
    }
}
