package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class ResetPreference extends DialogPreference {

    public ResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            KissApplication.getDataHandler(getContext()).clearHistory();
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("require-layout-update", true).apply();

            Toast.makeText(getContext(), R.string.history_erased, Toast.LENGTH_LONG).show();
        }

    }

}
