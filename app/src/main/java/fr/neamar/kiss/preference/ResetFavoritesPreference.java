package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class ResetFavoritesPreference extends DialogPreference {

    private static final String TAG = ResetFavoritesPreference.class.getSimpleName();

    public ResetFavoritesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putString("favorite-apps-list", "").apply();

            try {
                KissApplication.getApplication(getContext()).getDataHandler().reloadApps();
            } catch (NullPointerException e) {
                Log.e(TAG, "Unable to reset favorites", e);
            }

            Toast.makeText(getContext(), R.string.favorites_erased, Toast.LENGTH_LONG).show();
        }

    }

}
