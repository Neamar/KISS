package fr.neamar.kiss.utils.fuzzy;

import android.util.Pair;

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

    public List<Pair<Integer, Integer>> getMatchedSequences() {
        assert this.matchedIndices != null;
        // compute pair match indices
        List<Pair<Integer, Integer>> positions = new ArrayList<>(this.matchedIndices.size());
        int start = this.matchedIndices.get(0);
        int end = start + 1;
        for (int i = 1; i < this.matchedIndices.size(); i += 1) {
            if (end == this.matchedIndices.get(i)) {
                end += 1;
            } else {
                positions.add(new Pair<>(start, end));
                start = this.matchedIndices.get(i);
                end = start + 1;
            }
        }
        positions.add(new Pair<>(start, end));
        return positions;
    }
}
