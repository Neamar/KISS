package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
            KissApplication.getApplication(getContext()).getDataHandler().clearHistory();

            // We'll have to redraw the list, so add a flag for MainActivity to restart
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            // SettingsActivity will have to restart (hides the summary with history count after reset)
            // Nothing to do for MainActivity, since updateSearchRecords() is called onResume().
            editor.putBoolean("require-settings-update", true);
            editor.apply();

            Toast.makeText(getContext(), R.string.history_erased, Toast.LENGTH_LONG).show();
        }

    }

}
