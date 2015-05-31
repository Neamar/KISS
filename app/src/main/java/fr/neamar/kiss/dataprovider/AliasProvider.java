package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadAliasPojos;
import fr.neamar.kiss.pojo.AliasPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AliasProvider extends Provider<AliasPojo> {
    private final AppProvider appProvider;

    public AliasProvider(final Context context, AppProvider appProvider) {
        super(new LoadAliasPojos(context));
        this.appProvider = appProvider;
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        for (AliasPojo entry : pojos) {
            if (entry.alias.startsWith(query)) {
                Pojo pojo = appProvider.findById(entry.app);

                if (pojo != null && !pojo.nameLowerCased.contains(query)) {
                    pojo.displayName = pojo.name
                            + " <small>("
                            + entry.alias.replaceFirst(
                            "(?i)(" + Pattern.quote(query) + ")", "{$1}")
                            + ")</small>";
                    pojo.relevance = 10;
                    results.add(pojo);
                }
            }
        }

        return results;
    }
}
