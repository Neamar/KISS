package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

import java.util.HashSet;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class ResetExcludedAppsPreference extends DialogPreference {

    public ResetExcludedAppsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putStringSet("excluded-apps", new HashSet<String>()).apply();
            KissApplication.getApplication(getContext()).getDataHandler().getAppProvider().reload();
            Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
        }

    }

}
