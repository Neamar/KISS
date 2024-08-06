package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.db.HistoryMode;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all results that match the specified pojo with tags.
 */
public abstract class PojoWithTagSearcher extends Searcher {

    private final SharedPreferences prefs;

    public PojoWithTagSearcher(MainActivity activity, String query) {
        super(activity, query, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
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
    public boolean addResults(List<? extends Pojo> pojos) {
        List<Pojo> filteredPojos = new ArrayList<>();
        for (Pojo pojo : pojos) {
            if (!(pojo instanceof PojoWithTags)) {
                continue;
            }
            PojoWithTags pojoWithTags = (PojoWithTags) pojo;
            if (acceptPojo(pojoWithTags)) {
                filteredPojos.add(pojoWithTags);
            }
        }

        MainActivity activity = activityWeakReference.get();
        if (activity == null) {
            return false;
        }

        KissApplication.getApplication(activity).getDataHandler().applyRelevanceFromHistory(filteredPojos, getTaggedResultSortMode());

        return super.addResults(filteredPojos);
    }

    @NonNull
    private HistoryMode getTaggedResultSortMode() {
        String sortMode = prefs.getString("tagged-result-sort-mode", "default");
        if ("default".equals(sortMode)) {
            return KissApplication.getApplication(activityWeakReference.get()).getDataHandler().getHistoryMode();
        }
        return HistoryMode.valueById(sortMode);

    }

    protected int getMaxResultCount() {
        return Integer.MAX_VALUE;
    }

    abstract protected boolean acceptPojo(PojoWithTags pojoWithTags);
}
