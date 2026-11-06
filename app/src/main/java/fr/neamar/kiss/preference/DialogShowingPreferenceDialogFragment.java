package fr.neamar.kiss.preference;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceDialogFragmentCompat;

public class DialogShowingPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    public static DialogFragment newInstance(String key) {
        DialogShowingPreferenceDialogFragment fragment = new DialogShowingPreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        ((DialogShowingPreference) getPreference()).onDialogClosed(positiveResult);
    }

}
