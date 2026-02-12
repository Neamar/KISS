package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SelectCustomSearchProvidersPreference extends CustomSearchProvidersPreference {
    public SelectCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SelectCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectCustomSearchProvidersPreference(@NonNull Context context) {
        super(context);
    }

    public void refresh() {
        setEntries();
        notifyChanged();
    }

}
