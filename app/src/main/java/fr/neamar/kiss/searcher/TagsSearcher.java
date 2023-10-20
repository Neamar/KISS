package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all applications that match the specified tag
 */

public class TagsSearcher extends Searcher {
    public TagsSearcher(MainActivity activity, String query) {
        super(activity, query == null ? "<tags>" : query);
    }

    @Override
    public boolean addResults(List<? extends Pojo> pojos) {
        List<Pojo> filteredPojos = new ArrayList<>();
        for (Pojo pojo : pojos) {
            if (!(pojo instanceof PojoWithTags)) {
                continue;
            }
            PojoWithTags pojoWithTags = (PojoWithTags) pojo;
            if (pojoWithTags.getTags() == null || pojoWithTags.getTags().isEmpty()) {
                continue;
            }

            if (!pojoWithTags.getTags().contains(query)) {
                continue;
            }

            filteredPojos.add(pojoWithTags);
        }
        return super.addResults(filteredPojos);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        KissApplication.getApplication(activity).getDataHandler().requestAllRecords(this);

        return null;
    }

    @Override
    int getMaxResultCount() {
        return 250;
    }
}
