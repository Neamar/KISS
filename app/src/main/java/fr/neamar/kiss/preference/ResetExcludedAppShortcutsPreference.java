package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class ResetExcludedAppShortcutsPreference extends DialogPreference {

    public ResetExcludedAppShortcutsPreference(Context context) {
        super(context, null);
    }

    public ResetExcludedAppShortcutsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, null).apply();
            DataHandler dataHandler = KissApplication.getApplication(getContext()).getDataHandler();
            // Reload shortcuts to refresh the shortcuts shown in KISS
            dataHandler.reloadShortcuts();
            // Reload apps since the `AppPojo.isExcludedShortcuts` value also needs to be refreshed
            dataHandler.reloadApps();
            Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
        }

    }

}
