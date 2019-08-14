package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.shortcut.SaveOreoShortcutAsync;

public class ResetShortcutsPreference extends DialogPreference {

    public ResetShortcutsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {

            Toast.makeText(getContext(), R.string.shortcuts_reset_done_desc, Toast.LENGTH_LONG).show();

            DBHelper.removeAllShortcuts(getContext());
            new SaveOreoShortcutAsync(getContext()).execute();
        }
    }
}
