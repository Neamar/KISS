package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

public class DialogShowingPreference extends DialogPreference {
    private OnDialogClosedListener onDialogClosedListener = null;

    public DialogShowingPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DialogShowingPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DialogShowingPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogShowingPreference(@NonNull Context context) {
        super(context);
    }

    /**
     * Set by the fragment displaying this preference, so it is restored with the fragment
     * (unlike a callback stored on the dialog fragment, which is lost when the dialog is recreated)
     */
    public void setOnDialogClosedListener(@Nullable OnDialogClosedListener onDialogClosedListener) {
        this.onDialogClosedListener = onDialogClosedListener;
    }

    void onDialogClosed(boolean positiveResult) {
        if (onDialogClosedListener != null) {
            onDialogClosedListener.onDialogClosed(this, positiveResult);
        }
    }

    @FunctionalInterface
    public interface OnDialogClosedListener {
        void onDialogClosed(@NonNull DialogShowingPreference preference, boolean positiveResult);
    }
}
