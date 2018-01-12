package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
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
import java.util.regex.Matcher;

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

        // default text color is white that doesnt work well on the light themes
        String theme = prefs.getString("theme", "light");
        //if theme is light, change the text color
        if (!theme.contains("dark")) {

            int[] attrs = {android.R.attr.textColor};
            TypedArray ta = getContext().obtainStyledAttributes(R.style.AppThemeLight, attrs);

            providerName.setTextColor(ta.getColor(0, Color.TRANSPARENT));
            providerUrl.setTextColor(ta.getColor(0, Color.TRANSPARENT));
        }

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

    private boolean validatePipes() {
        if (providerName.getText().toString().contains("|") || providerUrl.getText().toString().contains("|")) {
            return false;
        }
        return true;
    }

    private boolean validateQueryPlaceholder() {
        if (!providerUrl.getText().toString().contains("{q}")) {
            return false;
        }
        return true;
    }

    private boolean validateNameExists() {
        Set<String> availableSearchProviders = prefs.getStringSet("available-search-providers", SearchProvider.getSearchProviders(this.getContext()));
        for (String searchProvider : availableSearchProviders) {
            String[] nameAndUrl = searchProvider.split("\\|");
            if (nameAndUrl.length == 2) {
                if (nameAndUrl[0].equals(providerName.getText().toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateEmpty() {
        return (!providerName.getText().toString().isEmpty()) && (!providerUrl.getText().toString().isEmpty());
    }

    private boolean validateUrl() {
        Matcher m = SearchProvider.urlPattern.matcher(providerUrl.getText().toString());
        return m.find();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which == DialogInterface.BUTTON_POSITIVE) {
            //check if empty name / url
            if (!validateEmpty()) {
                // do not close - empty strings
                closeDialog(dialog, false, which);
                return;
            }
            // check if input contains |
            if (!validatePipes()) {
                //show tip
                Toast.makeText(this.getContext(), R.string.search_provider_error_char, Toast.LENGTH_SHORT).show();
                //cancel close dialog
                closeDialog(dialog, false, which);
                return;
            }
            //check if custom provider
            if (!validateNameExists()) {
                //show tip
                Toast.makeText(this.getContext(), R.string.search_provider_error_exists, Toast.LENGTH_SHORT).show();
                //cancel close dialog
                closeDialog(dialog, false, which);
                return;
            }
            // check input
            if (!validateQueryPlaceholder()) {
                //show tip
                Toast.makeText(this.getContext(), R.string.search_provider_error_placeholder, Toast.LENGTH_SHORT).show();
                //cancel close dialog
                closeDialog(dialog, false, which);
                return;
            }
            //check if a valid url is given
            if (!validateUrl()) {
                Toast.makeText(this.getContext(), R.string.search_provider_error_url, Toast.LENGTH_SHORT).show();
                //not a url
                closeDialog(dialog, false, which);
                return;
            }
            //if all validates are correct, then close dialog with close flag = true
            closeDialog(dialog, true, which);

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
            Set<String> availableProviders = new HashSet<String>(prefs.getStringSet("available-search-providers", SearchProvider.getSearchProviders(this.getContext())));
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