package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Returns the list of all applications on the system
 */
public class ApplicationsSearcher extends Searcher{
    public ApplicationsSearcher(MainActivity activity) {
        super(activity);
    }

    @Override
    protected List<Pojo> doInBackground(Void... voids) {
        // Ask for records
        final ArrayList<Pojo> pojos = KissApplication.getDataHandler(activity).getApplications(activity);

        return pojos;
    }
}
