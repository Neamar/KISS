package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import fr.neamar.kiss.R;

public class AddSearchProviderPreference extends DialogPreference {

    public AddSearchProviderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        this.setDialogLayoutResource(R.layout.pref_add_search_provider);
    }

}