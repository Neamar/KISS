package fr.neamar.kiss.utils.fuzzy;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

public class FuzzyFactory {

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern) {
        return createFuzzyScore(context, pattern, false);
    }

    public static FuzzyScore createFuzzyScore(@NonNull Context context, int[] pattern, boolean detailedMatchIndices) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("use-fuzzy-score-v2", false)) {
            return new FuzzyScoreV2(pattern, detailedMatchIndices);
        } else {
            return new FuzzyScoreV1(pattern, detailedMatchIndices);
        }
    }

}
