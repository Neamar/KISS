package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;

public class ResetSearchProvidersPreference extends DialogPreference {

    public ResetSearchProvidersPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .remove("available-search-providers").apply();
            Toast.makeText(getContext(), R.string.search_provider_reset_done_desc, Toast.LENGTH_LONG).show();
            SearchProvider searchProvider = KissApplication.getApplication(getContext()).getDataHandler().getSearchProvider();
            if (searchProvider != null) {
                searchProvider.reload();
            }
        }
    }
}
