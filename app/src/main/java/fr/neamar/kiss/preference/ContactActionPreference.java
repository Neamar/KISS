package fr.neamar.kiss.preference;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MimeTypeCache;
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
        entries.add(getContext().getString(R.string.contact_action_sms));
        entryValues.add(MimeTypeUtils.CONTACT_ACTION_SMS);
        entries.add(getContext().getString(R.string.ui_item_contact_hint_call));
        entryValues.add(MimeTypeUtils.CONTACT_ACTION_CALL);

        // get all supported mime types, phone is already handled by SMS and call
        Set<String> supportedMimeTypes = MimeTypeUtils.getSupportedMimeTypes(getContext());
        supportedMimeTypes.remove(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        // get all labels, sorted
        MimeTypeCache mimeTypeCache = KissApplication.getMimeTypeCache(getContext());
        Map<String, String> actionLabels = mimeTypeCache.getActionLabels(getContext(), supportedMimeTypes);
        List<String> sortedMimeTypes = new ArrayList<>(supportedMimeTypes);
        Collections.sort(sortedMimeTypes, (a, b) -> actionLabels.get(a).compareToIgnoreCase(actionLabels.get(b)));

        for (String mimeType : sortedMimeTypes) {
            entries.add(actionLabels.get(mimeType));
            entryValues.add(mimeType);
        }

        setEntries(entries.toArray(new String[0]));
        setEntryValues(entryValues.toArray(new String[0]));
    }
}
