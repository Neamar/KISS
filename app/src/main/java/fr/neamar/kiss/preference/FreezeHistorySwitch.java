package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.SwitchPreference;

import fr.neamar.kiss.R;


public class FreezeHistorySwitch extends SwitchPreference {

    public FreezeHistorySwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FreezeHistorySwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FreezeHistorySwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FreezeHistorySwitch(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            //show dialog
            new AlertDialog.Builder(getContext()).setMessage(R.string.freeze_history_warn)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> FreezeHistorySwitch.super.onClick())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        // does nothing
                    }).show();
        } else {
            super.onClick();
        }
    }
}
