package fr.neamar.kiss.searcher;

import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Returns the list of all applications on the system
 */
public class ApplicationsSearcher extends Searcher {
    public ApplicationsSearcher(MainActivity activity) {
        super(activity, "<application>");
    }

    @Override
    protected Void doInBackground( Void... voids )
    {
        MainActivity activity = activityWeakReference.get();
        if( activity == null )
            return null;

        List<Pojo> pojos = KissApplication.getDataHandler(activity).getApplications();
        this.addResult( pojos.toArray(new Pojo[0]) );
        return null;
    }
}
