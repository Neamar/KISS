package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.broadcast.PackageAddedRemovedHandler;
import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.UserHandle;

public class AppProvider extends Provider<AppPojo> {

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler itself
            final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
            assert manager != null;

            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onPackageAdded(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_ADDED, packageName, user, false);
                }

                @Override
                public void onPackageChanged(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_CHANGED, packageName, user, true);
                }

                @Override
                public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                    handleEvent(Intent.ACTION_PACKAGE_REMOVED, packageName, user, false);
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    handleEvent(Intent.ACTION_MEDIA_MOUNTED, null, user, replacing);
                }

                @Override
                public void onPackagesSuspended(String[] packageNames, android.os.UserHandle user) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        handleEvent(Intent.ACTION_PACKAGES_SUSPENDED, null, user, false);
                    }
                }

                @Override
                public void onPackagesUnsuspended(String[] packageNames, android.os.UserHandle user) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        handleEvent(Intent.ACTION_PACKAGES_UNSUSPENDED, null, user, false);
                    }
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    handleEvent(Intent.ACTION_MEDIA_UNMOUNTED, null, user, replacing);
                }

                private void handleEvent(String action, String packageName, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                action,
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), replacing
                        );
                    }
                }
            });
        }

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        appChangedFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appChangedFilter.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
            appChangedFilter.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        }
        appChangedFilter.addDataScheme("package");
        appChangedFilter.addDataScheme("file");
        this.registerReceiver(new PackageAddedRemovedHandler(), appChangedFilter);

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

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        for (AppPojo pojo : pojos) {
            // exclude apps from results
            if (pojo.isExcluded() && !prefs.getBoolean("enable-excluded-apps", false)) {
                continue;
            }
            // exclude favorites from results
            if (excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                continue;
            }

            FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
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

    /**
     * Return a Pojo
     *
     * @param id we're looking for
     * @return an AppPojo, or null
     */
    @Override
    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    public List<AppPojo> getAllApps() {
        List<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }

    public List<AppPojo> getAllAppsWithoutExcluded() {
        List<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            if (pojo.isExcluded()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
