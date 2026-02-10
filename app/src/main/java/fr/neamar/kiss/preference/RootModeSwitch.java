package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.SwitchPreference;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class RootModeSwitch extends SwitchPreference {

    public RootModeSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RootModeSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RootModeSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RootModeSwitch(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (KissApplication.getApplication(getContext()).getRootHandler().isRootAvailable()) {
            super.onSetInitialValue(defaultValue);
            setEnabled(true);
        } else {
            super.onSetInitialValue(false);
            setEnabled(false);
        }
    }

    @Override
    protected void onClick() {
        if (!isChecked() && !KissApplication.getApplication(getContext()).getRootHandler().isRootAvailable()) {
            //show error dialog
            new AlertDialog.Builder(getContext()).setMessage(R.string.root_mode_error)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // does nothing
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
