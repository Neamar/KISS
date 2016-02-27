package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class FreezeHistoryPreference extends CheckBoxPreference {

    public FreezeHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            Toast.makeText(super.getContext(), super.getContext().getString(R.string.freeze_history_warn), Toast.LENGTH_SHORT).show();
        }
        super.onClick();
    }
}
