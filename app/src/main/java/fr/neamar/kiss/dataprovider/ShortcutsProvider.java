package fr.neamar.kiss.dataprovider;

import android.util.Pair;

import java.util.List;

import fr.neamar.kiss.loader.LoadShortcutsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class ShortcutsProvider extends Provider<ShortcutsPojo> {

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadShortcutsPojos(this));
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();

        for (ShortcutsPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            boolean bDisplayNameSet = false;
            boolean bDisplayTagsSet = false;
            pojo.relevance = matchInfo.score;

            if (match) {
                List<Pair<Integer, Integer>> positions = matchInfo.getMatchedSequences();
                try {
                    pojo.setDisplayNameHighlightRegion(positions);
                } catch (Exception e) {
                    pojo.setDisplayNameHighlightRegion(0, pojo.normalizedName.length());
                }
                bDisplayNameSet = true;
            }

            // check relevance for tags
            if (pojo.normalizedTags != null) {
                if (fuzzyScore.match(pojo.normalizedTags.codePoints, matchInfo)) {
                    if (!match || (matchInfo.score > pojo.relevance)) {
                        match = true;
                        pojo.relevance = matchInfo.score;
                        pojo.setTagHighlight(matchInfo.matchedIndices);
                        bDisplayTagsSet = true;
                    }
                }
            }

            if (match) {
                if (!bDisplayNameSet)
                    pojo.displayName = pojo.getName();
                if (!bDisplayTagsSet)
                    pojo.displayTags = pojo.getTags();
                if (!searcher.addResult(pojo))
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
    private Pojo findById(String id, Boolean allowSideEffect) {
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

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.getName().equals(name))
                return pojo;
        }
        return null;
    }


}
