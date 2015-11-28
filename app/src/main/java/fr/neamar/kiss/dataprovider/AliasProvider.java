package fr.neamar.kiss.dataprovider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadAliasPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AliasPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AliasProvider extends Provider<AliasPojo> {
    private AppProvider appProvider = null;

    /**
     * Handler for connection to the required application provider
     */
    private final ServiceConnection appConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Provider.LocalBinder binder = (Provider.LocalBinder) service;
            appProvider = (AppProvider) binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };


    @Override
    public void onCreate() {
        // Connect to the required application provider
        this.bindService(
                new Intent(this, AppProvider.class),
                this.appConnection,
                Context.BIND_AUTO_CREATE
        );

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // Disconnect from application provider
        this.unbindService(this.appConnection);

        super.onDestroy();
    }

    @Override
    public void reload() {
        this.initialize(new LoadAliasPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {
        query = StringNormalizer.normalize(query);

        ArrayList<Pojo> results = new ArrayList<>();

        for (AliasPojo entry : pojos) {
            if (entry.alias.startsWith(query)) {
                // Retrieve the AppPojo from AppProvider, being careful not to create any side effect
                // (default behavior is to alter displayName, which is not what we want)
                Pojo appPojo = appProvider.findById(entry.app, false);
                // Only add if default AppProvider is not already displaying it
                if (appPojo != null && !appPojo.nameNormalized.contains(query)) {
                    appPojo.displayName = appPojo.name
                            + " <small>("
                            + entry.alias.replaceFirst(
                            "(?i)(" + Pattern.quote(query) + ")", "{$1}")
                            + ")</small>";
                    appPojo.relevance = 10;
                    results.add(appPojo);
                }
            }
        }

        return results;
    }
}
