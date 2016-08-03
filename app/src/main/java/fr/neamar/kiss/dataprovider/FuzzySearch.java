package fr.neamar.kiss.dataprovider;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;

public class FuzzySearch {
    public interface CallBack {
        Pojo notRelevant(String query, Pojo pojo);
    }
    protected static ArrayList<Pojo> fuzzySearch(String query, List<? extends Pojo> pojos) {
        return  fuzzySearch(query, pojos, null);
    }
    protected static ArrayList<Pojo> fuzzySearch(String query, List<? extends Pojo> pojos, CallBack cb) {
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> records = new ArrayList<>();

        int relevance;
        int queryPos;           // The position inside the query
        int normalizedAppPos;   // The position inside pojo.nameNormalized
        int appPos;             // The position inside pojo.name, updated after we increment normalizedAppPos
        int beginMatch ;
        int matchedWordStarts;
        int totalWordStarts;
        ArrayList<Pair<Integer, Integer>> matchPositions;

        for (Pojo pojo : pojos) {
            relevance = 0;
            pojo.relevance = 0;
            queryPos = 0;
            normalizedAppPos = 0;
            appPos = pojo.mapPosition(normalizedAppPos);
            beginMatch = 0;
            matchedWordStarts = 0;
            totalWordStarts = 0;
            matchPositions = null;

            boolean match = false;
            int inputLength = pojo.nameNormalized.length();
            int nInputLength = pojo.name.length();
            while (normalizedAppPos < inputLength && appPos < nInputLength ) {
                int cApp = pojo.nameNormalized.codePointAt(normalizedAppPos);
                if (queryPos < query.length() && query.codePointAt(queryPos) == cApp) {
                    // If we aren't already matching something, let's save the beginning of the match
                    if (!match) {
                        beginMatch = normalizedAppPos;
                        match = true;
                    }

                    // If we are at the beginning of a word, add it to matchedWordStarts
                    if (appPos == 0 || normalizedAppPos == 0
                            || Character.isUpperCase(pojo.name.codePointAt(appPos))
                            || Character.isWhitespace(pojo.name.codePointBefore(appPos)))
                        matchedWordStarts += 1;

                    // Increment the position in the query
                    queryPos += Character.charCount(query.codePointAt(queryPos));
                }
                else if (match) {
                    if (matchPositions == null)
                        matchPositions = new ArrayList<>();
                    matchPositions.add(Pair.create(beginMatch, normalizedAppPos));
                    match = false;
                }

                // If we are at the beginning of a word, add it to totalWordsStarts
                if (appPos == 0 || normalizedAppPos == 0
                        || Character.isUpperCase(pojo.name.codePointAt(appPos))
                        || Character.isWhitespace(pojo.name.codePointBefore(appPos)))
                    totalWordStarts += 1;

                normalizedAppPos += Character.charCount(cApp);
                appPos = pojo.mapPosition(normalizedAppPos);
            }

            if (match) {
                if (matchPositions == null)
                    matchPositions = new ArrayList<>();
                matchPositions.add(Pair.create(beginMatch, normalizedAppPos));
            }

            if (queryPos == query.length() && matchPositions != null) {
                // Add percentage of matched letters, but at a weight of 40
                relevance += (int)(((double)queryPos / pojo.nameNormalized.length()) * 40);

                // Add percentage of matched upper case letters (start of word), but at a weight of 60
                relevance += (int)(((double)matchedWordStarts / totalWordStarts) * 60);

                // The more fragmented the matches are, the less the result is important
                relevance *= (0.2 + 0.8 * (1.0 / matchPositions.size()));
            }

            if (relevance <= 0 && cb != null ) {
                pojo = cb.notRelevant(query, pojo);
            }

            if (relevance > 0 || pojo.relevance > 0) {
                if (relevance > 0)
                    pojo.setDisplayNameHighlightRegion(matchPositions);
                pojo.relevance = Math.max(relevance, pojo.relevance);
                records.add(pojo);
            }
        }

        return records;
    }
}
