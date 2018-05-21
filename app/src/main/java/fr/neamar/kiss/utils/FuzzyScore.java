package fr.neamar.kiss.utils;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A Sublime Text inspired fuzzy match algorithm
 * https://github.com/forrestthewoods/lib_fts/blob/master/docs/fuzzy_match.md
 * <p>
 * match("otw", "Power of the Wild", info) = true, info.score = 14
 * match("otw", "Druid of the Claw", info) = true, info.score = -3
 * match("otw", "Frostwolf Grunt", info) = true, info.score = -13
 */
public class FuzzyScore {
    private final int patternLength;
    private final int[] patternChar;
    private final int[] patternLower;
    /**
     * bonus for adjacent matches
     */
    private int adjacency_bonus;
    /**
     * bonus if match occurs after a separator
     */
    private int separator_bonus;
    /**
     * bonus if match is uppercase and prev is lower
     */
    private int camel_bonus;
    /**
     * penalty applied for every letter in str before the first match
     */
    private int leading_letter_penalty;
    /**
     * maximum penalty for leading letters
     */
    private int max_leading_letter_penalty;
    /**
     * penalty for every letter that doesn't matter
     */
    private int unmatched_letter_penalty;

    private final MatchInfo matchInfo;

    public FuzzyScore(int[] pattern, boolean detailedMatchIndices) {
        super();
        patternLength = pattern.length;
        patternChar = new int[patternLength];
        patternLower = new int[patternLength];
        for (int i = 0; i < patternLower.length; i += 1) {
            patternChar[i] = pattern[i];
            patternLower[i] = Character.toLowerCase(pattern[i]);
        }
        adjacency_bonus = 10;
        separator_bonus = 5;
        camel_bonus = 10;
        leading_letter_penalty = -3;
        max_leading_letter_penalty = -9;
        unmatched_letter_penalty = -1;
        if (detailedMatchIndices) {
            matchInfo = new MatchInfo(patternLength);
        } else {
            matchInfo = new MatchInfo();
        }
    }

    public FuzzyScore(int[] pattern) {
        this(pattern, false);
    }

    public void setAdjacencyBonus(int adjacency_bonus) {
        this.adjacency_bonus = adjacency_bonus;
    }

    public void setSeparatorBonus(int separator_bonus) {
        this.separator_bonus = separator_bonus;
    }

    public void setCamelBonus(int camel_bonus) {
        this.camel_bonus = camel_bonus;
    }

    public void setLeadingLetterPenalty(int leading_letter_penalty) {
        this.leading_letter_penalty = leading_letter_penalty;
    }

    public void setMaxLeadingLetterPenalty(int max_leading_letter_penalty) {
        this.max_leading_letter_penalty = max_leading_letter_penalty;
    }

    public void setUnmatchedLetterPenalty(int unmatched_letter_penalty) {
        this.unmatched_letter_penalty = unmatched_letter_penalty;
    }

    /**
     * @param text string where to search
     * @param info will hold matching results
     * @return true if each character in pattern is found sequentially within text
     */
    public MatchInfo match(CharSequence text) {
        int idx = 0;
        int idxCodepoint = 0;
        int textLength = text.length();
        int[] codepoints = new int[Character.codePointCount(text, 0, textLength)];
        while (idx < textLength) {
            int codepoint = Character.codePointAt(text, idx);
            codepoints[idxCodepoint] = codepoint;
            idx += Character.charCount(codepoint);
            idxCodepoint += 1;
        }
        return match(codepoints);
    }

    /**
     * @param text string converted to codepoints
     * @return true if each character in pattern is found sequentially within text
     */
    public MatchInfo match(int[] text) {
        // Loop variables
        int score = 0;
        int patternIdx = 0;
        int strIdx = 0;
        int strLength = text.length;
        boolean prevMatched = false;
        boolean prevLower = false;
        boolean prevSeparator = true;       // true so if first letter match gets separator bonus

        // Use "best" matched letter if multiple string letters match the pattern
        Integer bestLetter = null;
        Integer bestLower = null;
        Integer bestLetterIdx = null;
        int bestLetterScore = 0;

        if (matchInfo.matchedIndices != null) {
            matchInfo.matchedIndices.clear();
        }

        // Loop over strings
        while (strIdx != strLength) {
            Integer patternChar = null;
            Integer patternLower = null;
            if (patternIdx != patternLength) {
                patternChar = this.patternChar[patternIdx];
                patternLower = this.patternLower[patternIdx];
            }
            int strChar = text[strIdx];
            int strLower = Character.toLowerCase(strChar);
            int strUpper = Character.toUpperCase(strChar);

            boolean nextMatch = patternChar != null && patternLower == strLower;
            boolean rematch = bestLetter != null && bestLower == strLower;

            boolean advanced = nextMatch && bestLetter != null;
            boolean patternRepeat = bestLetter != null && patternChar != null && patternLower.equals(bestLower);
            if (advanced || patternRepeat) {
                score += bestLetterScore;
                if (matchInfo.matchedIndices != null) {
                    matchInfo.matchedIndices.add(bestLetterIdx);
                }
                bestLetter = null;
                bestLower = null;
                bestLetterIdx = null;
                bestLetterScore = 0;
            }

            if (nextMatch || rematch) {
                int newScore = 0;

                // Apply penalty for each letter before the first pattern match
                // Note: std::max because penalties are negative values. So max is smallest penalty.
                if (patternIdx == 0) {
                    int penalty = Math.max(strIdx * leading_letter_penalty, max_leading_letter_penalty);
                    score += penalty;
                }

                // Apply bonus for consecutive bonuses
                if (prevMatched && !rematch)
                    newScore += adjacency_bonus;

                // Apply bonus for matches after a separator
                if (prevSeparator)
                    newScore += separator_bonus;

                // Apply bonus across camel case boundaries. Includes "clever" isLetter check.
                if (prevLower && strChar == strUpper && strLower != strUpper)
                    newScore += camel_bonus;

                // Update pattern index IF the next pattern letter was matched
                if (nextMatch)
                    ++patternIdx;

                // Update best letter in text which may be for a "next" letter or a "rematch"
                if (newScore >= bestLetterScore) {

                    // Apply penalty for now skipped letter
                    if (bestLetter != null)
                        score += unmatched_letter_penalty;

                    bestLetter = strChar;
                    bestLower = strLower;
                    bestLetterIdx = strIdx;
                    bestLetterScore = newScore;
                }

                prevMatched = true;
            } else {
                score += unmatched_letter_penalty;
                prevMatched = false;
            }

            // Includes "clever" isLetter check.
            prevLower = strChar == strLower && strLower != strUpper;
            prevSeparator = Character.isWhitespace(strChar);

            ++strIdx;
        }

        // Apply score for last match
        if (bestLetter != null) {
            score += bestLetterScore;
            if (matchInfo.matchedIndices != null) {
                matchInfo.matchedIndices.add(bestLetterIdx);
            }
        }

        matchInfo.match = patternIdx == patternLength;
        if (matchInfo.match) {
            matchInfo.score = score;
        }
        return matchInfo;
    }

    public static class MatchInfo {
        /**
         * higher is better match. Value has no intrinsic meaning. Range varies with pattern.
         * Can only compare scores with same search pattern.
         */
        public int score;
        public boolean match;
        public final ArrayList<Integer> matchedIndices;

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
}
