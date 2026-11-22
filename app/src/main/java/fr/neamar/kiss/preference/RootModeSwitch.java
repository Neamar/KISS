package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.SwitchPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        // No root check here: it runs su and can block the main thread (ANR). onClick checks instead.
        super.onSetInitialValue(defaultValue);
    }

    @Override
    protected void onClick() {
        if (!isChecked() && !KissApplication.getApplication(getContext()).getRootHandler().isRootAvailable()) {
            //show error dialog
            new MaterialAlertDialogBuilder(getContext()).setMessage(R.string.root_mode_error)
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
