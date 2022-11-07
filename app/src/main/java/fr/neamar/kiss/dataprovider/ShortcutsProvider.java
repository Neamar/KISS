package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadShortcutsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class ShortcutsProvider extends Provider<ShortcutPojo> {
    private static boolean notifiedKissNotDefaultLauncher = false;

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, android.os.UserHandle user) {
                    KissApplication.getApplication(ShortcutsProvider.this).getDataHandler().reloadShortcuts();
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
        }
        catch(IllegalStateException e) {
            if(!notifiedKissNotDefaultLauncher) {
                // Only display this message once per process
                Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
            }
            notifiedKissNotDefaultLauncher = true;
            e.printStackTrace();
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ShortcutPojo pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            // check relevance for tags
            if (pojo.getNormalizedTags() != null) {
                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    public List<ShortcutPojo> getPinnedShortcuts() {
        List<ShortcutPojo> records = new ArrayList<>(pojos.size());

        for (ShortcutPojo pojo : pojos) {
            if (!pojo.isPinned()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
