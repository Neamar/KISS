package fr.neamar.kiss.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;

public class AddSearchProviderPreference extends DialogPreference {

    //Layout Fields
    private final LinearLayout layout = new LinearLayout(this.getContext());
    private final EditText providerName = new EditText(this.getContext());
    private final EditText providerUrl = new EditText(this.getContext());

    private SharedPreferences prefs;

    //Called when addPreferencesFromResource() is called. Initializes basic parameters
    public AddSearchProviderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        layout.setOrientation(LinearLayout.VERTICAL);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    //Create the Dialog view
    @Override
    protected View onCreateDialogView() {
        removeViews();
        providerName.setHint(R.string.search_provider_name);
        providerUrl.setHint(R.string.search_provider_url);
        providerUrl.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        providerName.setText("");
        providerUrl.setText("");

        //adding margins (default is zero)
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(40, 10, 40, 0);

        //add the two text fields (with margins)
        layout.addView(providerName, layoutParams);
        layout.addView(providerUrl, layoutParams);

        // default text color is white that doesn't work well on the light themes
        String theme = prefs.getString("theme", "light");
        //if theme is light, change the text color
        if (!theme.contains("dark")) {

            int[] attrs = {android.R.attr.textColor};
            TypedArray ta = getContext().obtainStyledAttributes(R.style.AppThemeLight, attrs);

            providerName.setTextColor(ta.getColor(0, Color.TRANSPARENT));
            providerUrl.setTextColor(ta.getColor(0, Color.TRANSPARENT));
            ta.recycle();
        }

        return layout;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final AlertDialog dlg = (AlertDialog) getDialog();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    save();
                    dlg.dismiss();
                }
            }
        });
    }

    private boolean validatePipes() {
        return !(providerName.getText().toString().contains("|") || providerUrl.getText().toString().contains("|"));
    }

    private boolean validateQueryPlaceholder() {
        return providerUrl.getText().toString().contains("%s");
    }

    @SuppressWarnings("StringSplitter")
    private boolean validateNameExists() {
        Set<String> availableSearchProviders = prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(this.getContext()));
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
        return !providerName.getText().toString().isEmpty() && !providerUrl.getText().toString().isEmpty();
    }

    private boolean validateUrl() {
        Matcher m = SearchProvider.urlPattern.matcher(providerUrl.getText().toString());
        return m.find();
    }

    private boolean validate() {

        if (!validateEmpty()) {
            // do not close - empty strings
            return false;
        }
        // check if input contains |
        if (!validatePipes()) {
            //show tip
            Toast.makeText(this.getContext(), R.string.search_provider_error_char, Toast.LENGTH_SHORT).show();
            //cancel close dialog
            return false;
        }
        //check if custom provider
        if (!validateNameExists()) {
            //show tip
            Toast.makeText(this.getContext(), R.string.search_provider_error_exists, Toast.LENGTH_SHORT).show();
            //cancel close dialog
            return false;
        }
        // check input
        if (!validateQueryPlaceholder()) {
            //show tip
            Toast.makeText(this.getContext(), R.string.search_provider_error_placeholder, Toast.LENGTH_SHORT).show();
            //cancel close dialog
            return false;
        }
        //check if a valid url is given
        if (!validateUrl()) {
            Toast.makeText(this.getContext(), R.string.search_provider_error_url, Toast.LENGTH_SHORT).show();
            //not a url
            return false;
        }
        //if all validates are correct, then close dialog with close flag = true
        return true;

    }

    //persist values and disassemble views
    protected void save() {

        Set<String> availableProviders = new HashSet<>(prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(this.getContext())));
        availableProviders.add(providerName.getText().toString() + "|" + providerUrl.getText().toString());
        prefs.edit().putStringSet("available-search-providers", availableProviders).apply();
        prefs.edit().putStringSet("deleting-search-providers-names", availableProviders).apply();

        Toast.makeText(getContext(), R.string.search_provider_added, Toast.LENGTH_LONG).show();
    }

    private void removeViews() {
        if (providerName.getParent() != null) {
            ((ViewGroup) providerName.getParent()).removeView(providerName);
        }
        if (providerUrl.getParent() != null) {
            ((ViewGroup) providerUrl.getParent()).removeView(providerUrl);
        }
        if (layout.getParent() != null) {
            ((ViewGroup) layout.getParent()).removeView(layout);
        }
        notifyChanged();
    }
}