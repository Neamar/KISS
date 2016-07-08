package fr.neamar.kiss.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.R;


public class AppSwitcherModeSwitch extends SwitchPreference {

    public AppSwitcherModeSwitch(Context context) {
        this(context, null);
    }

    public AppSwitcherModeSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public AppSwitcherModeSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
