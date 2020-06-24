package fr.neamar.kiss.dataprovider;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

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
    @SuppressLint("NewApi")
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler itself
            final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
            assert manager != null;

            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherApps.Callback() {
                @Override
                public void onPackageAdded(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_ADDED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackageChanged(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_ADDED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), true
                        );
                    }
                }

                @Override
                public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_REMOVED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.MEDIA_MOUNTED",
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.MEDIA_UNMOUNTED",
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }
            });

            // Try to clean up app-related data when profile is removed
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
            this.registerReceiver(new BroadcastReceiver() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Objects.equals(intent.getAction(), Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                        AppProvider.this.reload();
                    } else if (Objects.equals(intent.getAction(), Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
                        android.os.UserHandle profile = intent.getParcelableExtra(Intent.EXTRA_USER);

                        UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);

                        KissApplication.getApplication(context).getDataHandler().removeFromExcluded(user);
                        KissApplication.getApplication(context).getDataHandler().removeFromFavorites(user);
                        AppProvider.this.reload();
                    }
                }
            }, filter);
        }

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
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
     * Translates an all- Russian string, for example, ЙЦУКЕН, into an English string QWERTY and the other way round
     * This is meant to handle cases when user forgot to switch keyboard layout and types blindly.
     * @param query - query to translate
     * @return null if it should not be translated, translated query otherwise.
     */
    private String swapKeyboardLayoutIfNeeded(String query) {
        String currentLanguage = Locale.getDefault().getDisplayLanguage();
        switch(currentLanguage) {
            case "русский":
                String englishLetters = "qwertyuiop[]asdfghjkl;'zxcvbnm,.";
                String russianLetters = "йцукенгшщзхъфывапролджэячсмитьбю";
                String res = query;
                boolean containsRussianLetters = Pattern.matches(".*\\p{InCyrillic}.*", query);

                for(int i = 0; i < englishLetters.length() - 1; i++) {
                    if(containsRussianLetters) {
                        res = res.replace(russianLetters.charAt(i), englishLetters.charAt(i));
                    } else {
                        res = res.replace(englishLetters.charAt(i), russianLetters.charAt(i));
                    }
                }
                return res;
            default:
                return null;
        }
    }

    private FuzzyScore getFuzzyScore(String query) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        if (queryNormalized.codePoints.length == 0) {
            return null;
        }
        return new FuzzyScore(queryNormalized.codePoints);
    }

    /**
     * @param query    The string to search for
     * @param searcher The receiver of results
     */

    @Override
    public void requestResults(String query, Searcher searcher) {
        FuzzyScore mainFuzzyScore = getFuzzyScore(query);

        if (mainFuzzyScore == null) {
            return;
        }
        String keyboardSwappedQuery = swapKeyboardLayoutIfNeeded(query);
        FuzzyScore keyboardSwappedFuzzyScore = null;
        if(keyboardSwappedQuery != null) {
            keyboardSwappedFuzzyScore = getFuzzyScore(keyboardSwappedQuery);
        }

        //FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        //FuzzyScore fuzzyScoreKeyboardSwapped = null;

        //String swappedQuery = swapKeyboardLayoutIfNeeded(query);


        //FuzzyScore.MatchInfo matchInfo;

        for (AppPojo pojo : pojos) {
            if(pojo.isExcluded()) {
                continue;
            }

            boolean match = checkMatch(mainFuzzyScore, pojo);
            if(!match) {
                match = checkMatch(keyboardSwappedFuzzyScore, pojo);
            }
            if (match && !searcher.addResult(pojo)) {
                return;
            }
            //boolean translatedMatch = false;
        }
    }

    private boolean checkMatch(FuzzyScore mainFuzzyScore, AppPojo pojo) {
        FuzzyScore.MatchInfo matchInfo;
        boolean match;
        matchInfo = mainFuzzyScore.match(pojo.normalizedName.codePoints);
        match = matchInfo.match;
        pojo.relevance = matchInfo.score;

        // check relevance for tags
        if (pojo.getNormalizedTags() != null) {
            matchInfo = mainFuzzyScore.match(pojo.getNormalizedTags().codePoints);
            if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                match = true;
                pojo.relevance = matchInfo.score;
            }
        }
        return match;
    }

    /**
     * Return a Pojo
     *
     * @param id              we're looking for
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

    public ArrayList<AppPojo> getAllApps() {
        ArrayList<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }

    public ArrayList<AppPojo> getAllAppsWithoutExcluded() {
        ArrayList<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            if(pojo.isExcluded()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
