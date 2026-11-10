package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.MimeTypeUtils;

/**
 * Select the action of a contact button: nothing, SMS, call, or any contact data from other apps (WhatsApp, Signal, ...)
 */
public class ContactActionPreference extends ListPreference {

    public ContactActionPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public ContactActionPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public ContactActionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public ContactActionPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    private void setEntries() {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        entries.add(getContext().getString(R.string.contact_action_none));
        entryValues.add(MimeTypeUtils.CONTACT_ACTION_NONE);

        Map<String, String> labels = MimeTypeUtils.getContactActionLabels(getContext(), MimeTypeUtils.getSupportedMimeTypes(getContext()));
        for (Map.Entry<String, String> label : labels.entrySet()) {
            entries.add(label.getValue());
            entryValues.add(label.getKey());
        }

        setEntries(entries.toArray(new String[0]));
        setEntryValues(entryValues.toArray(new String[0]));
    }
}
