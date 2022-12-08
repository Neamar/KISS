package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.utils.ShortcutUtil;

/**
 * Retrieve pojos from history
 */
public class HistorySearcher extends Searcher {
    private final SharedPreferences prefs;

    public HistorySearcher(MainActivity activity) {
        super(activity, "<history>");
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    int getMaxResultCount() {
        // Convert `"number-of-display-elements"` to double first before truncating to int to avoid
        // `java.lang.NumberFormatException` crashes for values larger than `Integer.MAX_VALUE`
        try {
            return Double.valueOf(prefs.getString("number-of-display-elements", String.valueOf(DEFAULT_MAX_RESULTS))).intValue();
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_RESULTS;
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Ask for records
        String historyMode = prefs.getString("history-mode", "recency");
        boolean excludeFavorites = prefs.getBoolean("exclude-favorites-history", false);

        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        //Gather excluded
        Set<String> excludedFromHistory = KissApplication.getApplication(activity).getDataHandler().getExcludedFromHistory();
        Set<String> excludedPojoById = new HashSet<>(excludedFromHistory);

        // add ids of shortcuts for excluded apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            for (String id : excludedFromHistory) {
                Pojo pojo = KissApplication.getApplication(activity).getDataHandler().getItemById(id);
                if (pojo instanceof AppPojo) {
                    List<ShortcutInfo> shortcutInfos = ShortcutUtil.getShortcuts(activity, ((AppPojo) pojo).packageName);
                    for (ShortcutInfo shortcutInfo : shortcutInfos) {
                        ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(activity, shortcutInfo, !shortcutInfo.isPinned());
                        if (shortcutRecord != null) {
                            excludedPojoById.add(ShortcutUtil.generateShortcutId(shortcutRecord));
                        }
                    }
                }
            }
        }

        if (excludeFavorites) {
            // Gather favorites
            for (Pojo favoritePojo : KissApplication.getApplication(activity).getDataHandler().getFavorites()) {
                excludedPojoById.add(favoritePojo.id);
            }
        }

        List<Pojo> pojos = KissApplication.getApplication(activity).getDataHandler()
                .getHistory(activity, getMaxResultCount(), historyMode, excludedPojoById);

        int size = pojos.size();
        for(int i = 0; i < size; i += 1) {
            pojos.get(i).relevance = size - i;
        }

        this.addResult(pojos.toArray(new Pojo[0]));
        return null;
    }
}
