package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadShortcutsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.fuzzy.FuzzyFactory;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;

public class ShortcutsProvider extends Provider<ShortcutPojo> {
    private static boolean notifiedKissNotDefaultLauncher = false;
    private static final String TAG = ShortcutsProvider.class.getSimpleName();

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, android.os.UserHandle user) {
                    if (isAnyShortcutVisible(shortcuts)) {
                        Log.d(TAG, "Shortcuts changed for " + packageName);
                        KissApplication.getApplication(ShortcutsProvider.this).getDataHandler().reloadShortcuts();
                    }
                }

                private boolean isAnyShortcutVisible(List<ShortcutInfo> shortcuts) {
                    DataHandler dataHandler = KissApplication.getApplication(ShortcutsProvider.this).getDataHandler();
                    Set<String> excludedApps = dataHandler.getExcluded();
                    Set<String> excludedShortcutApps = dataHandler.getExcludedShortcutApps();

                    for (ShortcutInfo shortcutInfo : shortcuts) {
                        if (ShortcutUtil.isShortcutVisible(ShortcutsProvider.this, shortcutInfo, excludedApps, excludedShortcutApps)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        super.onCreate();
    }

    @Override
    public void reload() {
        super.reload();
        // If the user tries to add a new shortcut, but KISS isn't the default launcher
        // AND the services are not running (low memory), then we won't be able to
        // spawn a new service on Android 8.1+.

        try {
            this.initialize(new LoadShortcutsPojos(this));
        } catch (IllegalStateException e) {
            if (!notifiedKissNotDefaultLauncher) {
                // Only display this message once per process
                Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
            }
            notifiedKissNotDefaultLauncher = true;
            Log.i(TAG, "Unable to initialize shortcuts", e);
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        Set<String> excludedFavoriteIds = KissApplication.getApplication(this).getDataHandler().getExcludedFavorites();

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = FuzzyFactory.createFuzzyScore(this, queryNormalized.codePoints);

        for (ShortcutPojo pojo : getPojos()) {
            // exclude favorites from results
            if (excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                continue;
            }

            MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            boolean match = pojo.updateMatchingRelevance(matchInfo, false);

            // check relevance for tags
            if (pojo.getNormalizedTags() != null) {
                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    public List<ShortcutPojo> getPinnedShortcuts() {
        List<ShortcutPojo> pojos = getPojos();
        List<ShortcutPojo> records = new ArrayList<>(pojos.size());

        for (ShortcutPojo pojo : pojos) {
            if (!pojo.isPinned()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
