package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AppProvider extends Provider<AppPojo> {

    public AppProvider(Context context) {
        super(new LoadAppPojos(context));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> records = new ArrayList<>();

        int relevance;
        String appNameLowerCased;

        final String highlightRegexp = "(?i)(" + StringNormalizer.unNormalize(query) + ")";
        for (int i = 0; i < pojos.size(); i++) {
            relevance = 0;
            appNameLowerCased = pojos.get(i).nameLowerCased;
            if (appNameLowerCased.startsWith(query))
                relevance = 100;
            else if (appNameLowerCased.contains(" " + query))
                relevance = 50;
            else if (appNameLowerCased.contains(query))
                relevance = 1;

            if (relevance > 0) {
                pojos.get(i).displayName = pojos.get(i).name.replaceFirst(
                        highlightRegexp, "{$1}");
                pojos.get(i).relevance = relevance;
                records.add(pojos.get(i));
            }
        }

        return records;
    }

    /**
     * Return a Pojo
     *
     * @param id              we're looking for
     * @param allowSideEffect do we allow this function to have potential side effect? Set to false to ensure none.
     * @return an apppojo, or null
     */
    public Pojo findById(String id, Boolean allowSideEffect) {
        for (int i = 0; i < pojos.size(); i++) {
            if (pojos.get(i).id.equals(id)) {
                // Reset displayName to default value
                if (allowSideEffect) {
                    pojos.get(i).displayName = pojos.get(i).name;
                }
                return pojos.get(i);
            }

        }

        return null;
    }

    public Pojo findById(String id) {
        return findById(id, true);
    }

    public ArrayList<Pojo> getAllApps() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());
        records.trimToSize();

        for (int i = 0; i < pojos.size(); i++) {
            pojos.get(i).displayName = pojos.get(i).name;
            records.add(pojos.get(i));
        }
        return records;
    }
}
