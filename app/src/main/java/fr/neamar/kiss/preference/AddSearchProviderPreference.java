package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.SearchProvider;

public class AddSearchProviderPreference extends DialogPreference {

    //Layout Fields
    private final LinearLayout layout = new LinearLayout(this.getContext());
    private final EditText providerName = new EditText(this.getContext());
    private final EditText providerUrl = new EditText(this.getContext());

    SharedPreferences prefs;

    //Called when addPreferencesFromResource() is called. Initializes basic paramaters
    public AddSearchProviderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        layout.setOrientation(LinearLayout.VERTICAL);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    //Create the Dialog view
    @Override
    protected View onCreateDialogView() {
        providerName.setHint(R.string.search_provider_name);
        providerUrl.setHint(R.string.search_provider_url);
        layout.addView(providerName);
        layout.addView(providerUrl);

        return layout;
    }

    private boolean closeDialog(DialogInterface dialog, boolean close, int which) {
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, close);
            super.onClick(dialog, which);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean invalidData = false;
        boolean invalidName = false;

        if (which == DialogInterface.BUTTON_POSITIVE) {
            // check input
            if (providerName.getText().toString().contains("|") || providerUrl.getText().toString().contains("|")) {
                invalidData = true;
            }

            //check if custom provider
            Set<String> availableSearchProviders = prefs.getStringSet("available-search-providers", SearchProvider.getSearchProviders());
            for (String searchProvider : availableSearchProviders) {
                String[] nameAndUrl = searchProvider.split("\\|");
                if (nameAndUrl.length == 2) {
                    if (nameAndUrl[0].equals(providerName.getText().toString())) {
                        invalidName = true;
                    }
                }
            }
            if (invalidData) {
                //cancel close dialog
                closeDialog(dialog, false, which);
                //show tip
                Toast.makeText(this.getContext(), R.string.search_provider_error_char, Toast.LENGTH_SHORT).show();
            } else if (invalidName) {
                //cancel close dialog
                closeDialog(dialog, false, which);
                //show tip
                Toast.makeText(this.getContext(), R.string.search_provider_error_exists, Toast.LENGTH_SHORT).show();
            } else if ((providerName.getText().toString().isEmpty()) || (providerUrl.getText().toString().isEmpty())) {
                // do not close - empty strings
                closeDialog(dialog, false, which);
            }
            else {
                closeDialog(dialog, true, which);
            }
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            // now close
            closeDialog(dialog, true, which);
        }
    }

    //Attach persisted values to Dialog
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        providerName.setText("");
        providerUrl.setText("");
    }

    //persist values and disassemble views
    @Override
    protected void onDialogClosed(boolean positiveresult) {
        super.onDialogClosed(positiveresult);
        if (positiveresult && shouldPersist()) {
            //persistString(providerName.getText().toString());
            Set<String> availableProviders = new HashSet<String>(prefs.getStringSet("available-search-providers", SearchProvider.getSearchProviders()));
            availableProviders.add(providerName.getText().toString()+"|"+providerUrl.getText().toString().toLowerCase());
            prefs.edit().putStringSet("available-search-providers", availableProviders).commit();
            prefs.edit().putStringSet("deleting-search-providers-names", availableProviders).commit();

            Toast.makeText(getContext(), R.string.search_provider_added, Toast.LENGTH_LONG).show();
        }

        ((ViewGroup) providerName.getParent()).removeView(providerName);
        ((ViewGroup) providerUrl.getParent()).removeView(providerUrl);
        ((ViewGroup) layout.getParent()).removeView(layout);

        notifyChanged();
    }
}