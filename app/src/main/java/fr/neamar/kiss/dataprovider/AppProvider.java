package fr.neamar.kiss.dataprovider;

import android.util.Pair;
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
        String appNameNormalized;
        ArrayList<Pair<Integer, Integer>> matchPositions;

        for (Pojo pojo : pojos) {
            relevance = 0;
            appNameNormalized = pojo.nameNormalized;
            matchPositions = null;

            int queryPos = 0;
            int appPos = 0;
            int beginMatch = 0;
            int matchedWordStarts = 0;
            int totalWordStarts = 0;

            boolean match = false;
            for (char cApp : appNameNormalized.toCharArray()) {
                if (queryPos < query.length() && query.charAt(queryPos) == cApp) {
                    // If we aren't already matching something, let's save the beginning of the match
                    if (!match) {
                        beginMatch = appPos;
                        match = true;
                    }

                    // If we are at the beginning of a word, add it to matchedWordStarts
                    if (Character.isUpperCase(pojo.name.charAt(appPos)) || appPos == 0 || Character.isWhitespace(pojo.name.charAt(appPos - 1)))
                        matchedWordStarts += 1;

                    // Increment the position in the query
                    queryPos++;
                }
                else if (match) {
                    if (matchPositions == null)
                        matchPositions = new ArrayList<>();
                    matchPositions.add(Pair.create(beginMatch, appPos));
                    match = false;
                }

                // If we are at the beginning of a word, add it to totalWordsStarts
                if (Character.isUpperCase(pojo.name.charAt(appPos)) || appPos == 0 || Character.isWhitespace(pojo.name.charAt(appPos - 1)))
                    totalWordStarts += 1;

                appPos++;
            }

            if (match) {
                if (matchPositions == null)
                    matchPositions = new ArrayList<>();
                matchPositions.add(Pair.create(beginMatch, appPos));
            }

            if (queryPos == query.length() && matchPositions != null) {
                // Base score for all matching apps of 20
                relevance += 20;

                // Add percentage of matched letters, but at a weight of 30
                relevance += (int)(((double)queryPos / appNameNormalized.length()) * 30);

                // Add percentage of matched upper case letters (start of word), but at a weight of 50
                relevance += (int)(((double)matchedWordStarts / totalWordStarts) * 50);

                // The more fragmented the matches are, the less the result is important
                relevance *= (0.2 + 0.8 * (1.0 / matchPositions.size()));
            }

            if (relevance > 0) {
                pojo.setDisplayNameHighlightRegion(matchPositions);
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
