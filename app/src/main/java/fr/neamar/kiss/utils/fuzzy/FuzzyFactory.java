package fr.neamar.kiss.utils.fuzzy;

public class FuzzyFactory {

    public static FuzzyScore createFuzzyScore(int[] pattern) {
        return createFuzzyScore(pattern, false);
    }

    public static FuzzyScore createFuzzyScore(int[] pattern, boolean detailedMatchIndices) {
        return new FuzzyScoreV2(pattern, detailedMatchIndices);
    }

}
