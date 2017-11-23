package fr.neamar.kiss.dataprovider;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadSettingsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SettingsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class SettingsProvider extends Provider<SettingsPojo> {
    private String settingName;

    @Override
    public void reload() {
        this.initialize(new LoadSettingsPojos(this));

        settingName = this.getString(R.string.settings_prefix).toLowerCase();
    }

    @Override
    public void requestResults( String query, Searcher searcher )
    {
        String queryNormalized = StringNormalizer.normalize( query );

        FuzzyScore           fuzzyScore = new FuzzyScore();
        FuzzyScore.MatchInfo matchInfo  = new FuzzyScore.MatchInfo();

        for (SettingsPojo pojo : pojos)
        {
            boolean match = fuzzyScore.match( queryNormalized, pojo.nameNormalized, matchInfo );
            pojo.relevance = matchInfo.score;

            if ( match )
            {
                pojo.setDisplayNameHighlightRegion( matchInfo.getMatchedSequences() );
            }
            else if( fuzzyScore.match( queryNormalized, settingName, matchInfo ) )
            {
                match = true;
                pojo.relevance = matchInfo.score;
                pojo.setDisplayNameHighlightRegion(0, 0);
            }

            if( match )
            {
                if( !searcher.addResult( pojo ) )
                    return;
            }
        }
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String settingNameLowerCased;
        for (SettingsPojo setting : pojos) {
            relevance = 0;
            settingNameLowerCased = setting.nameNormalized;
            if (settingNameLowerCased.startsWith(query))
                relevance = 10;
            else if (settingNameLowerCased.contains(" " + query))
                relevance = 5;
            else if (settingName.startsWith(query)) {
                // Also display for a search on "settings" for instance
                relevance = 4;
            }

            if (relevance > 0) {
                setting.displayName = setting.name.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}");
                setting.relevance = relevance;
                results.add(setting);
            }
        }

        return results;
    }

    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                pojo.displayName = pojo.name;
                return pojo;
            }
        }

        return null;
    }
}
