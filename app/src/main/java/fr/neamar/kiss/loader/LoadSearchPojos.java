package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadSearchPojos extends LoadPojos<SearchPojo> {

    private static String KISS_PACKAGE_NAME;
    /**
     * Search activities that we know work when given a query
     */
    private static List<String> knownSearchActivities = Arrays.asList(
            "com.amazon.mShop.search.SearchActivity",
            "com.andrew.apollo.ui.activities.SearchActivity",
            "com.android.calendar.SearchActivity",
            "com.android.contacts.activities.PeopleActivity",
            "com.android.mms.ui.SearchResultBoxActivity",
            "com.android.music.QueryBrowserActivity",
            "com.android.vending.AssetBrowserActivity",
            "com.boardgamegeek.ui.SearchResultsActivity",
            "com.github.mobile.ui.search.SearchActivity",
            "com.google.android.apps.chromecast.app.search.SearchResultsActivity",
            "com.google.android.apps.youtube.app.WatchWhileActivity",
            "com.google.android.finsky.activities.MainActivity",
            "com.lge.music.QueryBrowserActivity",
            "com.netflix.mediaclient.ui.search.SearchActivity",
            "com.Prismatic.android.ui.SearchActivity",
            "com.soundcloud.android.search.SearchActivity",
            "com.ted.android.view.activity.SearchActivity",
            "com.twitter.android.SearchActivity",
            "org.fdroid.fdroid.SearchResults"
    );

    public LoadSearchPojos(Context context) {
        super(context, "search://");

        KISS_PACKAGE_NAME = context.getPackageName();
    }

    @Override
    protected ArrayList<SearchPojo> doInBackground(Void... params) {
        List<ResolveInfo> appsWithSearch = context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_SEARCH),
                PackageManager.GET_INTENT_FILTERS);
        ArrayList<SearchPojo> searchPojos = new ArrayList<>();
        Map<String, String> added = new HashMap<>();
        PackageManager manager = context.getPackageManager();

        for (ResolveInfo info : appsWithSearch) {
            String packageName = info.activityInfo.applicationInfo.packageName;
            String activityName = info.activityInfo.name;

            if (!KISS_PACKAGE_NAME.equals(packageName) &&
                    // thanks http://stackoverflow.com/a/11411881
                    info.activityInfo.exported &&
                    !added.containsKey(packageName)) {

                // Use a whitelist of known-good search activities.
                // Unfortunately, choosing a random activity that can fulfil Intent.ACTION_SEARCH
                // isn't foolproof, as some of them:
                // - Require more data than just a query
                // - Crash
                // - Open the search activity, then don't preform a search
                // See the discussion around https://github.com/Neamar/KISS/pull/288 for more info
                if (!knownSearchActivities.contains(activityName)) {
                    continue;
                }

                SearchPojo app = new SearchPojo();

                app.id = pojoScheme + packageName + "/" + activityName;

                try {
                    app.name = manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    // fall back to this activity's name
                    // sometimes app devs name their search activities "Search"
                    // which isn't useful in our case because our search activities
                    // are displayed as 'App Name: "query"'
                    app.name = info.loadLabel(manager).toString();
                }

                //Ugly hack to remove accented characters.
                //Note Java 5 provides a Normalizer method, unavailable for Android :\
                app.nameNormalized = StringNormalizer.normalize(app.name);

                app.packageName = packageName;
                app.activityName = activityName;

                added.put(packageName, activityName);
                searchPojos.add(app);
            }
        }

        return searchPojos;
    }
}
