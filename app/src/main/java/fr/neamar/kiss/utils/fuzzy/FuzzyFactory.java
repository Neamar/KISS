package fr.neamar.kiss.utils.fuzzy;

import android.content.Context;

import androidx.annotation.NonNull;

public class FuzzyFactory {

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern) {
        return createFuzzyScore(context, pattern, false);
    }

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern, boolean detailedMatchIndices) {
        return new FuzzyScoreV1(pattern, detailedMatchIndices);
    }

}
