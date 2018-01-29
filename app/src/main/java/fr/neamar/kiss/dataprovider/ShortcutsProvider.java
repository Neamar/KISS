package fr.neamar.kiss.dataprovider;

import fr.neamar.kiss.loader.LoadShortcutsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class ShortcutsProvider extends Provider<ShortcutsPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadShortcutsPojos(this));
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();

        for (ShortcutsPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            pojo.relevance = matchInfo.score;

            if (match) {
                pojo.setDisplayNameHighlightRegion(matchInfo.getMatchedSequences());
                if (!searcher.addResult(pojo))
                    return;
            }
        }
    }

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.getName().equals(name))
                return pojo;
        }
        return null;
    }


}
