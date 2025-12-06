package fr.neamar.kiss.utils.fuzzy;

import java.util.ArrayList;
import java.util.List;

public class MatchInfo {
    public static final MatchInfo UNMATCHED = new MatchInfo(false, 0);

    /**
     * higher is better match. Value has no intrinsic meaning. Range varies with pattern.
     * Can only compare scores with same search pattern.
     */
    public int score;
    public boolean match;
    final List<Integer> matchedIndices;

    MatchInfo(boolean match, int score) {
        this();
        this.match = match;
        this.score = score;
    }

    MatchInfo() {
        matchedIndices = null;
    }

    MatchInfo(int patternLength) {
        matchedIndices = new ArrayList<>(patternLength);
    }

    public List<Integer> getMatchedIndices() {
        return matchedIndices;
    }
}
