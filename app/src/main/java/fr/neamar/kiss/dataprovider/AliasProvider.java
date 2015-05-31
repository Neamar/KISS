package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadAliasPojos;
import fr.neamar.kiss.pojo.AliasPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AliasProvider extends Provider<AliasPojo> {
    private final ArrayList<Provider> providers;

    public AliasProvider(final Context context, ArrayList<Provider> providers) {
        super(new LoadAliasPojos(context));
        this.providers = providers;
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        for (AliasPojo entry : pojos) {
            if (entry.alias.startsWith(query)) {
                for (int i = 0; i < providers.size(); i++) {
                    if (providers.get(i).mayFindById(entry.app)) {
                        Pojo pojo = providers.get(i).findById(entry.app);

                        if (pojo != null) {
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
            }
        }

        return results;
    }
}
