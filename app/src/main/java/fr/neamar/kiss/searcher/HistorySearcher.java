package fr.neamar.kiss.searcher;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Retrieve pojos from history
 */
public class HistorySearcher extends Searcher {
    public HistorySearcher(MainActivity activity) {
        super(activity);
    }

    @Override
    protected ArrayList<Pojo> doInBackground(Void... voids) {
        // Ask for records
        final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getHistory(activity);

        return pojos;
    }
}
