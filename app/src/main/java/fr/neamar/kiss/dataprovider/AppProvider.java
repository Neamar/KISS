package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.broadcast.PackageAddedRemovedHandler;
import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.fuzzy.FuzzyFactory;
import fr.neamar.kiss.utils.fuzzy.FuzzyScoreV1;
import fr.neamar.kiss.utils.fuzzy.FuzzyScoreV2;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;


public class AppProvider extends Provider<AppPojo> {

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onPackageAdded(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_ADDED, new String[]{packageName}, user, false);
                }

                @Override
                public void onPackageChanged(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_CHANGED, new String[]{packageName}, user, true);
                }

                @Override
                public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_REMOVED, new String[]{packageName}, user, false);
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    handleEvent(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE, packageNames, user, replacing);
                }

                @Override
                public void onPackagesSuspended(String[] packageNames, android.os.UserHandle user) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        handleEvent(Intent.ACTION_PACKAGES_SUSPENDED, packageNames, user, false);
                    }
                }

                @Override
                public void onPackagesUnsuspended(String[] packageNames, android.os.UserHandle user) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        handleEvent(Intent.ACTION_PACKAGES_UNSUSPENDED, packageNames, user, false);
                    }
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    handleEvent(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE, packageNames, user, replacing);
                }

                private void handleEvent(String action, String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                            action,
                            packageNames, new UserHandle(AppProvider.this, user), replacing
                    );
                }
            });
        } else {
            // Get notified when app changes on standard user profile
            PackageAddedRemovedHandler packageAddedRemovedHandler = new PackageAddedRemovedHandler();

            IntentFilter appChangedFilter = new IntentFilter();
            appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            appChangedFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            appChangedFilter.addDataScheme("package");
            this.registerReceiver(packageAddedRemovedHandler, appChangedFilter);

            IntentFilter mediaChangedFilter = new IntentFilter();
            mediaChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            mediaChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
            mediaChangedFilter.addDataScheme("file");
            this.registerReceiver(packageAddedRemovedHandler, mediaChangedFilter);

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            intentFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            this.registerReceiver(packageAddedRemovedHandler, intentFilter);
        }

        super.onCreate();
    }

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadAppPojos(this));
    }

    /**
     * @param query    The string to search for
     * @param searcher The receiver of results
     */

    @Override
    public void requestResults(String query, Searcher searcher) {
        Set<String> excludedFavoriteIds = KissApplication.getApplication(this).getDataHandler().getExcludedFavorites();

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        boolean flexibleFuzzy = prefs.getBoolean("enable-fuzzy-search", false);

        if (flexibleFuzzy) {
            // New flexible fuzzy search logic for partial, misspelled queries
            for (AppPojo pojo : getPojos()) {
                // exclude apps from results
                if (pojo.isExcluded() && !prefs.getBoolean("enable-excluded-apps", false)) {
                    continue;
                }
                // exclude favorites from results
                if (excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                    continue;
                }

                // Match against app name
                int distance = substringLevenshteinDistance(queryNormalized.codePoints, pojo.normalizedName.codePoints);
                double similarity = 1.0 - (double) distance / queryNormalized.codePoints.length;
                boolean nameMatch = similarity > 0.6;
                int relevance = nameMatch ? (int) (similarity * 100) : 0;
                MatchInfo nameMatchInfo = new MatchInfo(nameMatch, relevance);
                boolean match = pojo.updateMatchingRelevance(nameMatchInfo, false);

                // Match against tags
                if (pojo.getNormalizedTags() != null) {
                    int distanceTags = substringLevenshteinDistance(queryNormalized.codePoints, pojo.getNormalizedTags().codePoints);
                    double similarityTags = 1.0 - (double) distanceTags / queryNormalized.codePoints.length;
                    boolean tagsMatch = similarityTags > 0.6;
                    int relevanceTags = tagsMatch ? (int) (similarityTags * 100) : 0;
                    MatchInfo tagsMatchInfo = new MatchInfo(tagsMatch, relevanceTags);
                    match = pojo.updateMatchingRelevance(tagsMatchInfo, match);
                }

                if (match && !searcher.addResult(pojo)) {
                    return;
                }
            }
        } else {
            // Original fuzzy search logic
            FuzzyScore fuzzyScore = FuzzyFactory.createFuzzyScore(this, queryNormalized.codePoints);

            for (AppPojo pojo : getPojos()) {
                // exclude apps from results
                if (pojo.isExcluded() && !prefs.getBoolean("enable-excluded-apps", false)) {
                    continue;
                }
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
    }

    private int substringLevenshteinDistance(int[] s1, int[] s2) {
        // s1 is the query, s2 is the text to search within
        int s1Len = s1.length;
        int s2Len = s2.length;

        if (s1Len == 0) {
            return 0;
        }

        int[][] dp = new int[s1Len + 1][s2Len + 1];

        // Initialize first column (cost of deleting query characters)
        for (int i = 0; i <= s1Len; i++) {
            dp[i][0] = i;
        }

        // Initialize first row to 0 (no cost for starting match anywhere in s2)
        // This is the key difference for substring matching.
        for (int j = 0; j <= s2Len; j++) {
            dp[0][j] = 0;
        }

        // Fill the rest of the matrix
        for (int i = 1; i <= s1Len; i++) {
            for (int j = 1; j <= s2Len; j++) {
                int cost = (s1[i - 1] == s2[j - 1]) ? 0 : 1;
                dp[i][j] = min(
                    dp[i - 1][j - 1] + cost,      // Substitution/Match
                    dp[i - 1][j] + 1,             // Deletion from s1
                    dp[i][j - 1] + 1              // Insertion into s1
                );
            }
        }

        // The result is the minimum value in the last row
        int minDistance = s1Len; // Max possible distance
        for (int j = 0; j <= s2Len; j++) {
            if (dp[s1Len][j] < minDistance) {
                minDistance = dp[s1Len][j];
            }
        }

        return minDistance;
    }

    private int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public List<AppPojo> getAllApps() {
        List<AppPojo> pojos = getPojos();
        List<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }

    public List<AppPojo> getAllAppsWithoutExcluded() {
        List<AppPojo> pojos = getPojos();
        List<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            if (pojo.isExcluded()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
