package fr.neamar.kiss.utils.fuzzy;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class FuzzyFactory {

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern) {
        return createFuzzyScore(context, pattern, false);
    }

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern, boolean detailedMatchIndices) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("use-fuzzy-score-v1", false)) {
            return new FuzzyScoreV1(pattern, detailedMatchIndices);
        } else {
            return new FuzzyScoreV2(pattern, detailedMatchIndices);
        }
    }

}
