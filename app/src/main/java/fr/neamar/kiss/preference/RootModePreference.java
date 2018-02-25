package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

class RootModePreference extends CheckBoxPreference {

    public RootModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        if (!isChecked() && !KissApplication.getApplication(getContext()).getRootHandler().isRootAvailable()) {
            //show error dialog
            new AlertDialog.Builder(getContext()).setMessage(R.string.root_mode_error)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // does nothing
                        }
                    }).show();
        } else {
            super.onClick();
        }

        try {
            KissApplication.getApplication(getContext()).resetRootHandler(getContext());
        } catch (NullPointerException e) {
            // uninitialized roothandler.
        }
    }
}
