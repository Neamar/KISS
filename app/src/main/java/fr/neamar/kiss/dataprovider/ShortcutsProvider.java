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

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();

        for (ShortcutsPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            pojo.relevance = matchInfo.score;

            // check relevance for tags
            if (pojo.normalizedTags != null) {
                if (fuzzyScore.match(pojo.normalizedTags.codePoints, matchInfo)) {
                    if (!match || (matchInfo.score > pojo.relevance)) {
                        match = true;
                        pojo.relevance = matchInfo.score;
                    }
                }
            }

            if (match && !searcher.addResult(pojo)) {
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
