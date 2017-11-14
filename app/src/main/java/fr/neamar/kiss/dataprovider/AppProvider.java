package fr.neamar.kiss.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.broadcast.PackageAddedRemovedHandler;
import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.DamerauLevenshteinAlgorithm;
import fr.neamar.kiss.utils.UserHandle;

public class AppProvider extends Provider<AppPojo> {

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler itself
            final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);

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
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                        AppProvider.this.reload();
                    }
                    else if (intent.getAction().equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
                        android.os.UserHandle profile = (android.os.UserHandle) intent.getParcelableExtra(Intent.EXTRA_USER);

                        UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);

                        KissApplication.getDataHandler(context).removeFromExcluded(user);
                        KissApplication.getDataHandler(context).removeFromFavorites(user);
                        AppProvider.this.reload();
                    }
                }
            }, filter);
        }

        super.onCreate();
    }

    @Override
    public void reload() {
        this.initialize(new LoadAppPojos(this));
    }

    //The distance is the number of deletions, insertions, or substitutions required to transform str1 into str2
    public static int computeLevenshteinDistance( CharSequence str1, CharSequence str2 )
    {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for( int i = 0; i <= str1.length(); i++ )
            distance[i][0] = i;
        for( int j = 1; j <= str2.length(); j++ )
            distance[0][j] = j;

        for( int i = 1; i <= str1.length(); i++ )
            for( int j = 1; j <= str2.length(); j++ )
            {
                int addChrToStr1 = distance[i - 1][j] + 1;
                int addChrToStr2 = distance[i][j - 1] + 1;
                int replaceChr = distance[i - 1][j - 1] + ((str1.charAt( i - 1 ) == str2.charAt( j - 1 )) ? 0 : 2);
                distance[i][j] = addChrToStr1 < addChrToStr2 ? addChrToStr1 : addChrToStr2;
                distance[i][j] = distance[i][j] < replaceChr ? distance[i][j] : replaceChr;
            }

        return distance[str1.length()][str2.length()];
    }

    /**
     *
     * @param query The string to search for
     * @param searcher The receiver of results
     */

    private static int DLA_DELETE_COST = 15;
    private static int DLA_INSERT_COST = 9;
    private static int DLA_REPLACE_COST = DLA_DELETE_COST + DLA_INSERT_COST;
    private static int DLA_SWAP_COST = 13;

    @Override
    public void requestResults( String query, Searcher searcher )
    {
        String queryNormalized = StringNormalizer.normalize( query );

        DamerauLevenshteinAlgorithm algorithm = new DamerauLevenshteinAlgorithm( DLA_DELETE_COST, DLA_INSERT_COST, DLA_REPLACE_COST, DLA_SWAP_COST );

        for( AppPojo pojo : pojos )
        {
            pojo.displayName = pojo.name;
            pojo.displayTags = pojo.tags;

            // check relevance for normalized name
            {
                double distance           = algorithm.execute( queryNormalized, pojo.nameNormalized );
                double q                  = queryNormalized.length() * DLA_DELETE_COST;
                double n                  = pojo.nameNormalized.length() * DLA_INSERT_COST;
                double normalizedDistance = distance / Math.sqrt( Math.max( q, n ) * Math.sqrt( q * n ) );
                pojo.relevance = 50 - (int)(normalizedDistance * 50d);
                if( distance < (q + n) )
                    pojo.relevance = Math.max( 1, pojo.relevance );
            }

            // check relevance for tags
            if( !pojo.tagsNormalized.isEmpty() )
            {
                int    tagRelevance  = 0;
                String tagList[]     = pojo.tagsNormalized.split( "\\s+" );
                int    matchedTagIdx = -1;
                for( int tagIdx = 0; tagIdx < tagList.length; tagIdx++ )
                {
                    String tag               = tagList[tagIdx];
                    int    distance          = computeLevenshteinDistance( tag, query );
                    int    maxEditOperations = tag.length() < query.length() ? query.length() : tag.length();
                    int    relevance         = maxEditOperations - distance;
                    if( tagRelevance < relevance )
                    {
                        tagRelevance = relevance;
                        matchedTagIdx = tagIdx;
                    }
                }

                if( matchedTagIdx > -1 )
                {
                    String tag      = tagList[matchedTagIdx];
                    int    tagStart = pojo.tagsNormalized.indexOf( tag );
                    int    tagEnd   = tagStart + tag.length();
                    pojo.setTagHighlight( tagStart, tagEnd );

                    if( pojo.relevance <= 0 )
                    {
                        pojo.relevance = tagRelevance;
                    }
                }

            }

            if( pojo.relevance > 0 )
            {
                // quick and dirty name highlight
                List<Pair<Integer, Integer>> positions = new ArrayList<>( queryNormalized.length() );
                for ( int i = 0; i < queryNormalized.length(); i+= 1 )
                {
                    int normalizedAppPos = 0;
                    while ( (normalizedAppPos = pojo.nameNormalized.indexOf( queryNormalized.codePointAt( i ), normalizedAppPos)) != -1 )
                    {
                        int appPos = pojo.mapPosition(normalizedAppPos++);
                        positions.add( new Pair<>( appPos, appPos + 1 ) );
                    }
                }
                Collections.sort( positions, new Comparator<Pair<Integer, Integer>>()
                {
                    @Override
                    public int compare( Pair<Integer, Integer> lhs, Pair<Integer, Integer> rhs )
                    {
                        return lhs.first - rhs.first;
                    }
                } );
                // merge positions and remove duplicates
                for( int i = 1; i < positions.size(); i++ )
                {
                    Pair<Integer, Integer> prev = positions.get( i - 1 );
                    Pair<Integer, Integer> curr = positions.get( i );
                    if ( prev.second >= curr.first )
                    {
                        positions.remove( i-- );
                        positions.set( i, new Pair<>( prev.first, Math.max(prev.second, curr.second) ) );
                    }
                }
                try
                {
                    pojo.setDisplayNameHighlightRegion( positions );
                } catch( Exception e )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( Pair p : positions )
                        sb.append( "<" ).append( p.first ).append( "," ).append( p.second ).append( ">" );
                    Log.e("TBog", sb.toString(), e);
                    pojo.displayName = pojo.name;
                    pojo.setDisplayNameHighlightRegion( 0, pojo.name.length() );
                }

                if( !searcher.addResult( pojo ) )
                    return;
            }
        }
    }

    @Override
    public ArrayList<Pojo> getResults(String query) {
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> records = new ArrayList<>();

        int relevance;
        int queryPos;           // The position inside the query
        int normalizedAppPos;   // The position inside pojo.nameNormalized
        int appPos;             // The position inside pojo.name, updated after we increment normalizedAppPos
        int beginMatch;
        int matchedWordStarts;
        int totalWordStarts;
        ArrayList<Pair<Integer, Integer>> matchPositions;

        for (AppPojo pojo : pojos) {
            pojo.displayName = pojo.name;
            pojo.displayTags = pojo.tags;
            relevance = 0;
            queryPos = 0;
            normalizedAppPos = 0;
            appPos = pojo.mapPosition(normalizedAppPos);
            beginMatch = 0;
            matchedWordStarts = 0;
            totalWordStarts = 0;
            matchPositions = null;

            boolean match = false;
            while (normalizedAppPos < pojo.nameNormalized.length()) {
                int cApp = pojo.nameNormalized.codePointAt(normalizedAppPos);
                if (queryPos < query.length() && query.codePointAt(queryPos) == cApp) {
                    // If we aren't already matching something, let's save the beginning of the match
                    if (!match) {
                        beginMatch = normalizedAppPos;
                        match = true;
                    }

                    // If we are at the beginning of a word, add it to matchedWordStarts
                    if (appPos == 0 || normalizedAppPos == 0
                        || Character.isUpperCase(pojo.name.codePointAt(appPos))
                        || Character.isWhitespace(pojo.name.codePointBefore(appPos)))
                        matchedWordStarts += 1;

                    // Increment the position in the query
                    queryPos += Character.charCount(query.codePointAt(queryPos));
                }
                else if (match) {
                    if (matchPositions == null)
                        matchPositions = new ArrayList<>();
                    matchPositions.add(Pair.create(beginMatch, normalizedAppPos));
                    match = false;
                }

                // If we are at the beginning of a word, add it to totalWordsStarts
                if (appPos == 0 || normalizedAppPos == 0
                    || Character.isUpperCase(pojo.name.codePointAt(appPos))
                    || Character.isWhitespace(pojo.name.codePointBefore(appPos)))
                    totalWordStarts += 1;

                normalizedAppPos += Character.charCount(cApp);
                appPos = pojo.mapPosition(normalizedAppPos);
            }

            if (match) {

                if (matchPositions == null) {matchPositions = new ArrayList<>();}
                matchPositions.add(Pair.create(beginMatch, normalizedAppPos));
            }

            boolean matchedTags = false;
            int tagStart = 0;
            int tagEnd = 0;

            if (queryPos == query.length() && matchPositions != null) {
                // Add percentage of matched letters, but at a weight of 40
                relevance += (int) (((double) queryPos / pojo.nameNormalized.length()) * 40);

                // Add percentage of matched upper case letters (start of word), but at a weight of 60
                relevance += (int) (((double) matchedWordStarts / totalWordStarts) * 60);

                // The more fragmented the matches are, the less the result is important
                relevance = (int) (relevance * (0.2 + 0.8 * (1.0 / matchPositions.size())));
            }
            else {
                if (pojo.tagsNormalized.startsWith(query)) {
                    relevance = 4 + query.length();
                }
                else if (pojo.tagsNormalized.contains(query)) {
                    relevance = 3 + query.length();
                }
                if (relevance > 0) {
                    matchedTags = true;
                }
                tagStart = pojo.tagsNormalized.indexOf(query);
                tagEnd = tagStart + query.length();
            }

            if (relevance > 0) {
                if (!matchedTags) {
                    pojo.setDisplayNameHighlightRegion(matchPositions);
                }
                else {
                    pojo.setTagHighlight(tagStart, tagEnd);
                }
                pojo.relevance = relevance;
                records.add(pojo);
            }
        }

        return records;
    }

    /**
     * Return a Pojo
     *
     * @param id              we're looking for
     * @param allowSideEffect do we allow this function to have potential side effect? Set to false to ensure none.
     * @return an AppPojo, or null
     */
    public Pojo findById(String id, Boolean allowSideEffect) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                // Reset displayName to default value
                if (allowSideEffect) {
                    pojo.displayName = pojo.name;
                    if (pojo instanceof AppPojo) {
                        AppPojo appPojo = (AppPojo) pojo;
                        appPojo.displayTags = appPojo.tags;
                    }
                }
                return pojo;
            }

        }

        return null;
    }

    public Pojo findById(String id) {
        return findById(id, true);
    }

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.name.equals(name))
                return pojo;
        }
        return null;
    }

    public ArrayList<Pojo> getAllApps() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());
        records.trimToSize();

        for (AppPojo pojo : pojos) {
            pojo.displayName = pojo.name;
            pojo.displayTags = pojo.tags;
            records.add(pojo);
        }
        return records;
    }

    public void removeApp(AppPojo appPojo) {
        pojos.remove(appPojo);
    }
}
