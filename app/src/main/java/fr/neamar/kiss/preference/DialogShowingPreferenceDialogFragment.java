package fr.neamar.kiss.preference;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class DialogShowingPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    private final OnDialogClosedCallback onDialogClosedCallback;

    public DialogShowingPreferenceDialogFragment(OnDialogClosedCallback onDialogClosedCallback) {
        this.onDialogClosedCallback = onDialogClosedCallback;
    }

    public static DialogFragment newInstance(String key, OnDialogClosedCallback onDialogClosedCallback) {
        DialogShowingPreferenceDialogFragment fragment = new DialogShowingPreferenceDialogFragment(onDialogClosedCallback);
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (onDialogClosedCallback != null) {
            onDialogClosedCallback.onDialogClosed(getPreference(), positiveResult);
        }
    }

    @FunctionalInterface
    public interface OnDialogClosedCallback {
        void onDialogClosed(Preference pref, boolean positiveResult);
    }

}
