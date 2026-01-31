package fr.neamar.kiss.preference;

import static fr.neamar.kiss.utils.URIUtils.URIValidity.INVALID_NAME_EXISTS;
import static fr.neamar.kiss.utils.URIUtils.URIValidity.INVALID_PIPE_CHAR;
import static fr.neamar.kiss.utils.URIUtils.URIValidity.NOT_AN_URI;
import static fr.neamar.kiss.utils.URIUtils.URIValidity.NO_PLACEHOLDER;
import static fr.neamar.kiss.utils.URIUtils.URIValidity.VALID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.utils.TrimmingTextChangedListener;
import fr.neamar.kiss.utils.URIUtils;
import fr.neamar.kiss.utils.URLUtils;

public class AddSearchProviderPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    //Layout Fields
    private EditText providerName;
    private EditText providerUri;

    public static DialogFragment newInstance(String key) {
        AddSearchProviderPreferenceDialogFragment fragment = new AddSearchProviderPreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        final View view = super.onCreateDialogView(context);

        providerName = view.findViewById(R.id.search_provider_name);
        providerName.addTextChangedListener(new TrimmingTextChangedListener(changedText -> enablePositiveButton(), true));
        providerUri = view.findViewById(R.id.search_provider_url);
        providerUri.addTextChangedListener(new TrimmingTextChangedListener(changedText -> enablePositiveButton(), true));

        return view;
    }

    private void enablePositiveButton() {
        URIUtils.URIValidity result = validate();
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result.isValid());
        providerUri.setError(result.getErrorMessage(requireContext()));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            URIUtils.URIValidity result = validate();
            if (result.isValid()) {
                save();
            } else {
                Toast.makeText(this.getContext(), result.getErrorMessage(requireContext()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validatePipes() {
        return !(providerName.getText().toString().contains("|") || providerUri.getText().toString().contains("|"));
    }

    private boolean isPlaceholder() {
        return providerUri.getText().toString().equals("%s");
    }

    private boolean validateQueryPlaceholder() {
        return providerUri.getText().toString().contains("%s");
    }

    private boolean validateNameExists() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> availableSearchProviders = prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(requireContext()));
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
        return !TextUtils.isEmpty(providerName.getText()) && !TextUtils.isEmpty(providerUri.getText());
    }

    private boolean validateUrl() {
        return URLUtils.matchesUrlPattern(providerUri.getText().toString());
    }

    private URIUtils.URIValidity validateUri() {
        return URIUtils.isValidUri(providerUri.getText().toString(), requireContext());
    }

    private URIUtils.URIValidity validate() {

        if (!validateEmpty()) {
            // do not close - empty strings
            return NOT_AN_URI;
        }
        // check if input contains |
        if (!validatePipes()) {
            //cancel close dialog
            return INVALID_PIPE_CHAR;
        }
        //check if custom provider
        if (!validateNameExists()) {
            //cancel close dialog
            return INVALID_NAME_EXISTS;
        }
        // check input
        if (!validateQueryPlaceholder()) {
            //cancel close dialog
            return NO_PLACEHOLDER;
        }
        //if all validates are correct, then close dialog with close flag = true

        // placeholder alone is valid too
        if (isPlaceholder()) {
            return VALID;
        }

        // If provider submitted is submitted not more check is need
        if (validateUrl()) {
            return VALID;
        }

        //check if a valid uri is given instead valid url
        return validateUri();
    }

    //persist values and disassemble views
    protected void save() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> availableProviders = new HashSet<>(prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(requireContext())));
        availableProviders.add(providerName.getText().toString() + "|" + providerUri.getText().toString());
        prefs.edit().putStringSet("available-search-providers", availableProviders).apply();

        Toast.makeText(getContext(), R.string.search_provider_added, Toast.LENGTH_LONG).show();
    }

}