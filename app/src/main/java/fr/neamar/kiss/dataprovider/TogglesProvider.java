package fr.neamar.kiss.dataprovider;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadTogglesPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.TogglesPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class TogglesProvider extends Provider<TogglesPojo> {
    private String toggleName;

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadTogglesPojos(this));

        toggleName = this.getString(R.string.toggles_prefix).toLowerCase();
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();

        for (TogglesPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            pojo.relevance = matchInfo.score;

            if (match) {
                pojo.setDisplayNameHighlightRegion(matchInfo.getMatchedSequences());
            } else if (fuzzyScore.match(toggleName, matchInfo)) {
                match = true;
                pojo.relevance = matchInfo.score;
                pojo.setDisplayNameHighlightRegion(0, 0);
            }

            if (match) {
                if (!searcher.addResult(pojo))
                    return;
            }
        }
    }
}
