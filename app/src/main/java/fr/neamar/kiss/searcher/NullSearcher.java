package fr.neamar.kiss.searcher;

import fr.neamar.kiss.MainActivity;

/**
 * Retrieve pojos from history
 */
public class NullSearcher extends Searcher {

    public NullSearcher(MainActivity activity) {
        super(activity, "<null>");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // nothing found ;)
        return null;
    }
}
