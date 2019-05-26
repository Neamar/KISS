package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

// https://code.google.com/p/android/issues/detail?id=26194
// Can be removed once we drop support for KitKat
// Forced 10 max lines in summary (different Android versions have different values)
public class SwitchPreference extends android.preference.SwitchPreference {

    public SwitchPreference(Context context) {
        this(context, null);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View summary = view.findViewById(android.R.id.summary);
        if (summary instanceof TextView)
            ((TextView) summary).setMaxLines(10);
    }
}
