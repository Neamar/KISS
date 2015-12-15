package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AppProvider extends Provider<AppPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadAppPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> records = new ArrayList<>();

        int relevance;
        int matchPositionStart;
        int matchPositionEnd;
        String appNameNormalized;

        final String queryWithSpace = " " + query;
        for (Pojo pojo : pojos) {
            relevance = 0;
            appNameNormalized = pojo.nameNormalized;

            matchPositionEnd = 0;
            if (appNameNormalized.startsWith(query)) {
                relevance = 100;
                matchPositionStart = 0;
                matchPositionEnd = query.length();
            } else if ((matchPositionStart = appNameNormalized.indexOf(queryWithSpace)) > -1) {
                relevance = 50;
                matchPositionEnd = matchPositionStart + queryWithSpace.length();
            } else if ((matchPositionStart = appNameNormalized.indexOf(query)) > -1) {
                relevance = 1;
                matchPositionEnd = matchPositionStart + query.length();
            }

            if (relevance > 0) {
                pojo.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
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
     * @return an apppojo, or null
     */
    public Pojo findById(String id, Boolean allowSideEffect) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                // Reset displayName to default value
                if (allowSideEffect) {
                    pojo.displayName = pojo.name;
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

        for (Pojo pojo : pojos) {
            pojo.displayName = pojo.name;
            records.add(pojo);
        }
        return records;
    }

    public void removeApp(AppPojo appPojo) {
        pojos.remove(appPojo);
    }
}
