package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.R;

public class FreezeHistoryPreference extends CheckBoxPreference {

    public FreezeHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            //show dialog
            new AlertDialog.Builder(getContext()).setMessage(R.string.freeze_history_warn)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FreezeHistoryPreference.super.onClick();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // does nothing
                        }
                    }).show();
        }
        else {
            super.onClick();
        }
    }
}
