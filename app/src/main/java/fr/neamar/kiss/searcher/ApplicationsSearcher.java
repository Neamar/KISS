package fr.neamar.kiss.searcher;

import android.content.Context;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;

/**
 * Returns the list of all applications on the system
 */
public class ApplicationsSearcher extends Searcher {
    public ApplicationsSearcher(MainActivity activity) {
        super(activity, "<application>");
    }

    @Override
    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        // Sort from A to Z, so reverse (last item needs to be A, listview starts at the bottom)
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, Collections.reverseOrder(new PojoComparator()));
    }

    @Override
    protected int getMaxResultCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        List<AppPojo> pojos = KissApplication.getApplication(activity).getDataHandler().getApplicationsWithoutExcluded();

        if (pojos != null)
           this.addResult(pojos.toArray(new Pojo[0]));
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);
        // Build sections for fast scrolling
        activityWeakReference.get().adapter.buildSections();
    }
}
