package fr.neamar.kiss.preference;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.handlers.ImportExportHandler;

import static fr.neamar.kiss.forwarder.Permission.askExternalStorageWritePermissionForSettings;

public class ExportTagsPreference extends DialogPreference {

    public ExportTagsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (Permission.checkExternalStorageWritePermission(getContext())) {
                ImportExportHandler.saveTagsToFile(getContext());
            }
            else {
                askExternalStorageWritePermissionForSettings((Activity)getContext());
            }
        }
    }
}
