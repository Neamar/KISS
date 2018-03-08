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
    protected void displayActivityLoader() {
        // Don't display the loader for the NullSearcher
        // (otherwise, pressing home again in minimalistic mode displays the loader for no reason)
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // nothing found ;)
        return null;
    }
}
