package fr.neamar.kiss.utils.fuzzy;

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
public class FuzzyScoreV2 implements FuzzyScore {
    private final int patternLength;
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
     * bonus if match is uppercase and prev is lower
     */
    private int first_letter_bonus;
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

    public FuzzyScoreV2(int[] pattern, boolean detailedMatchIndices) {
        super();
        patternLength = pattern.length;
        patternLower = new int[patternLength];
        for (int i = 0; i < patternLower.length; i += 1) {
            patternLower[i] = Character.toLowerCase(pattern[i]);
        }
        adjacency_bonus = 15;
        separator_bonus = 30;
        camel_bonus = 30;
        first_letter_bonus = 15;
        leading_letter_penalty = -5;
        max_leading_letter_penalty = -30;
        unmatched_letter_penalty = -2;
        if (detailedMatchIndices) {
            matchInfo = new MatchInfo(patternLength);
        } else {
            matchInfo = new MatchInfo();
        }
    }

    @Override
    public FuzzyScore setFullWordBonus(int full_word_bonus) {
        return this;
    }

    @Override
    public FuzzyScore setAdjacencyBonus(int adjacency_bonus) {
        this.adjacency_bonus = adjacency_bonus;
        return this;
    }

    @Override
    public FuzzyScore setSeparatorBonus(int separator_bonus) {
        this.separator_bonus = separator_bonus;
        return this;
    }

    @Override
    public FuzzyScore setCamelBonus(int camel_bonus) {
        this.camel_bonus = camel_bonus;
        return this;
    }

    @Override
    public FuzzyScore setFirstLetterBonus(int first_letter_bonus) {
        this.first_letter_bonus = first_letter_bonus;
        return this;
    }

    @Override
    public FuzzyScore setLeadingLetterPenalty(int leading_letter_penalty) {
        this.leading_letter_penalty = leading_letter_penalty;
        return this;
    }

    @Override
    public FuzzyScore setMaxLeadingLetterPenalty(int max_leading_letter_penalty) {
        this.max_leading_letter_penalty = max_leading_letter_penalty;
        return this;
    }

    @Override
    public FuzzyScore setUnmatchedLetterPenalty(int unmatched_letter_penalty) {
        this.unmatched_letter_penalty = unmatched_letter_penalty;
        return this;
    }

    /**
     * @param text string where to search
     * @return {@link MatchInfo}, with match set to true if each character in pattern is found sequentially within text
     */
    @Override
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
     * @param str string converted to codepoints
     * @return {@link MatchInfo}, with match set to true if each character in pattern is found sequentially within text
     */
    @Override
    public MatchInfo match(int[] str) {
        int recursionCount = 0;
        int recursionLimit = 10;
        int maxMatches = Math.min(patternLength, str.length);
        List<Integer> matches = new ArrayList<>();

        MatchInfo matchInfo = matchRecursive(
                str,
                0 /* patternCurIndex */,
                0 /* strCurrIndex */,
                null /* srcMatches */,
                matches,
                maxMatches,
                0 /* nextMatch */,
                recursionCount,
                recursionLimit
        );
        this.matchInfo.score = matchInfo.score;
        this.matchInfo.match = matchInfo.match;
        if (this.matchInfo.matchedIndices != null) {
            this.matchInfo.matchedIndices.clear();
            this.matchInfo.matchedIndices.addAll(matches);
        }
        return this.matchInfo;
    }

    private MatchInfo matchRecursive(
            int[] str,
            int patternCurIndex,
            int strCurrIndex,
            List<Integer> srcMatches,
            List<Integer> matches,
            int maxMatches,
            int nextMatch,
            int recursionCount,
            int recursionLimit
    ) {
        // Return if recursion limit is reached.
        if (++recursionCount >= recursionLimit) {
            return MatchInfo.UNMATCHED;
        }

        // Return if we reached ends of strings.
        if (patternCurIndex == patternLength || strCurrIndex == str.length) {
            return MatchInfo.UNMATCHED;
        }

        // Recursion params
        boolean recursiveMatch = false;
        List<Integer> bestRecursiveMatches = new ArrayList<>();
        int bestRecursiveScore = 0;

        // Loop through pattern and str looking for a match.
        boolean firstMatch = true;
        while (patternCurIndex < patternLength && strCurrIndex < str.length) {
            // Match found.
            if (patternLower[patternCurIndex] == Character.toLowerCase(str[strCurrIndex])) {
                if (nextMatch >= maxMatches) {
                    return MatchInfo.UNMATCHED;
                }

                if (firstMatch && srcMatches != null) {
                    matches.clear();
                    matches.addAll(srcMatches);
                    firstMatch = false;
                }

                List<Integer> recursiveMatches = new ArrayList<>();
                MatchInfo recursiveResult = matchRecursive(
                        str,
                        patternCurIndex,
                        strCurrIndex + 1,
                        matches,
                        recursiveMatches,
                        maxMatches,
                        nextMatch,
                        recursionCount,
                        recursionLimit
                );

                if (recursiveResult.match) {
                    // Pick best recursive score.
                    if (!recursiveMatch || recursiveResult.score > bestRecursiveScore) {
                        bestRecursiveMatches.clear();
                        bestRecursiveMatches.addAll(recursiveMatches);
                        bestRecursiveScore = recursiveResult.score;
                    }
                    recursiveMatch = true;
                }

                matches.add(strCurrIndex);
                ++nextMatch;
                ++patternCurIndex;
            }
            ++strCurrIndex;
        }

        int outScore = 0;
        boolean matched = patternCurIndex == patternLength;

        if (matched) {
            outScore = 100;

            // Apply leading letter penalty
            int penalty = Math.max(max_leading_letter_penalty, leading_letter_penalty * matches.get(0));
            outScore += penalty;

            //Apply unmatched penalty
            int unmatched = str.length - nextMatch;
            outScore += unmatched_letter_penalty * unmatched;

            // Apply ordering bonuses
            for (int i = 0; i < matches.size(); i++) {
                int currIdx = matches.get(i);

                if (i > 0) {
                    int prevIdx = matches.get(i - 1);
                    if (currIdx == prevIdx + 1) {
                        outScore += adjacency_bonus;
                    }
                }

                // Check for bonuses based on neighbor character value.
                if (currIdx > 0) {
                    // Camel case
                    int neighbor = str[currIdx - 1];
                    int curr = str[currIdx];
                    if (
                            neighbor != Character.toUpperCase(neighbor) &&
                                    curr != Character.toLowerCase(curr)
                    ) {
                        outScore += camel_bonus;
                    }
                    boolean isNeighbourSeparator = Character.isWhitespace(neighbor);
                    if (isNeighbourSeparator) {
                        outScore += separator_bonus;
                    }
                } else {
                    // First letter
                    outScore += first_letter_bonus;
                }
            }
        }
        // Return best result
        if (recursiveMatch && (!matched || bestRecursiveScore > outScore)) {
            // Recursive score is better than "this"
            matches.clear();
            matches.addAll(bestRecursiveMatches);
            outScore = bestRecursiveScore;
            return new MatchInfo(true, outScore);
        } else if (matched) {
            // "this" score is better than recursive
            return new MatchInfo(true, outScore);
        } else {
            return MatchInfo.UNMATCHED;
        }
    }

}
