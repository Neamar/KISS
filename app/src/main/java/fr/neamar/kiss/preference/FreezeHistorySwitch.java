package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.R;


public class FreezeHistorySwitch extends SwitchPreference {

    public FreezeHistorySwitch(Context context) {
        this(context, null);
    }

    public FreezeHistorySwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public FreezeHistorySwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            //show dialog
            new AlertDialog.Builder(getContext()).setMessage(R.string.freeze_history_warn)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FreezeHistorySwitch.super.onClick();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // does nothing
                        }
                    }).show();
        } else {
            super.onClick();
        }
    }
}
