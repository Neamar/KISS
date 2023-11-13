package fr.neamar.kiss.searcher;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ReversedNameComparator;
import fr.neamar.kiss.pojo.ShortcutPojo;

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
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, new ReversedNameComparator());
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

        Set<String> excludedFavoriteIds = KissApplication.getApplication(activity).getDataHandler().getExcludedFavorites();

        // add apps
        List<AppPojo> pojos = KissApplication.getApplication(activity).getDataHandler().getApplicationsWithoutExcluded();
        if (pojos != null) {
            this.addResults(getPojosWithoutFavorites(pojos, excludedFavoriteIds));
        }

        // add pinned shortcuts (PWA, ...)
        List<ShortcutPojo> shortcuts = KissApplication.getApplication(activity).getDataHandler().getPinnedShortcuts();
        if (shortcuts != null) {
            this.addResults(getPojosWithoutFavorites(shortcuts, excludedFavoriteIds));
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);

        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        // Build sections for fast scrolling
        activity.adapter.buildSections();
    }

    /**
     * @param pojos               list of pojos
     * @param excludedFavoriteIds ids of favorites to exclude from pojos
     * @return pojos without favorites
     */
    private <T extends Pojo> List<T> getPojosWithoutFavorites(List<T> pojos, Set<String> excludedFavoriteIds) {
        if (excludedFavoriteIds.isEmpty()) {
            return pojos;
        }
        List<T> records = new ArrayList<>(pojos.size());

        for (T pojo : pojos) {
            if (!excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                records.add(pojo);
            }
        }
        return records;
    }

}
