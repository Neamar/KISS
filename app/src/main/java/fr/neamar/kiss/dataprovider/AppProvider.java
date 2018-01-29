package fr.neamar.kiss.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.broadcast.PackageAddedRemovedHandler;
import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;
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

	/**
	 * @param query    The string to search for
	 * @param searcher The receiver of results
	 */

	@Override
	public void requestResults( String query, Searcher searcher )
	{
		StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult( query, false );

		FuzzyScore           fuzzyScore = new FuzzyScore( queryNormalized.codePoints );
		FuzzyScore.MatchInfo matchInfo  = new FuzzyScore.MatchInfo();

		for( AppPojo pojo : pojos )
		{
		    boolean bDisplayNameSet = false;
		    boolean bDisplayTagsSet = false;
			boolean match = fuzzyScore.match( pojo.normalizedName.codePoints, matchInfo );
			pojo.relevance = matchInfo.score;

			if ( match )
			{
				List<Pair<Integer, Integer>> positions = matchInfo.getMatchedSequences();
				try
				{
					pojo.setDisplayNameHighlightRegion( positions );
				} catch( Exception e )
				{
					pojo.setDisplayNameHighlightRegion( 0, pojo.normalizedName.length() );
				}
                bDisplayNameSet = true;
			}

			// check relevance for tags
			if( pojo.normalizedTags != null )
			{
				if( fuzzyScore.match( pojo.normalizedTags.codePoints, matchInfo ) )
				{
					if( !match || (matchInfo.score > pojo.relevance) )
					{
						match = true;
						pojo.relevance = matchInfo.score;
						pojo.setTagHighlight( matchInfo.matchedIndices );
						bDisplayTagsSet = true;
					}
				}
			}

			if( match )
			{
			    if ( !bDisplayNameSet )
                    pojo.displayName = pojo.getName();
			    if ( !bDisplayTagsSet )
                    pojo.displayTags = pojo.getTags();
				if( !searcher.addResult( pojo ) )
					return;
			}
		}
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
                    pojo.displayName = pojo.getName();
                    if (pojo instanceof PojoWithTags) {
						PojoWithTags tagsPojo = (PojoWithTags) pojo;
                        tagsPojo.displayTags = tagsPojo.getTags();
                    }
                }
                return pojo;
            }

        }

        return null;
    }

    @Override
    public Pojo findById(String id) {
        return findById(id, true);
    }

    public ArrayList<Pojo> getAllApps() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());
        records.trimToSize();

        for (AppPojo pojo : pojos) {
            pojo.displayName = pojo.getName();
            pojo.displayTags = pojo.getTags();
            records.add(pojo);
        }
        return records;
    }

    public void removeApp(AppPojo appPojo) {
        pojos.remove(appPojo);
    }
}
