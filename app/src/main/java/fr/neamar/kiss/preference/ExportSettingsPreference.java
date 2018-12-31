package fr.neamar.kiss.preference;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import fr.neamar.kiss.handlers.ImportExportHandler;
import fr.neamar.kiss.forwarder.Permission;

import static fr.neamar.kiss.forwarder.Permission.askExternalStorageWritePermissionForSettings;

public class ExportSettingsPreference extends DialogPreference {

    public ExportSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (Permission.checkExternalStorageWritePermission(getContext())) {
                ImportExportHandler.saveSharedPreferencesToFile(getContext(), getSharedPreferences());
            }
            else {
                askExternalStorageWritePermissionForSettings((Activity)getContext());
            }
        }
    }
}
