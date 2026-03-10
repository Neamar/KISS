package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MimeTypeCache;
import fr.neamar.kiss.utils.MimeTypeUtils;

public class MimeTypeSelectPreference extends MultiSelectListPreference {

    public MimeTypeSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public MimeTypeSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public MimeTypeSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public MimeTypeSelectPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    private void setEntries() {
        // get all supported mime types
        Set<String> supportedMimeTypes = MimeTypeUtils.getSupportedMimeTypes(getContext());

        // get all labels
        MimeTypeCache mimeTypeCache = KissApplication.getMimeTypeCache(getContext());
        Map<String, String> uniqueLabels = mimeTypeCache.getUniqueLabels(getContext(), supportedMimeTypes);

        // get entries and values for sorted mime types
        List<String> sortedMimeTypes = new ArrayList<>(supportedMimeTypes);
        Collections.sort(sortedMimeTypes);

        String[] mimeTypeEntries = new String[supportedMimeTypes.size()];
        String[] mimeTypeEntryValues = new String[supportedMimeTypes.size()];
        int pos = 0;
        for (String mimeType : sortedMimeTypes) {
            mimeTypeEntries[pos] = uniqueLabels.get(mimeType);
            mimeTypeEntryValues[pos] = mimeType;
            pos++;
        }

        setEntries(mimeTypeEntries);
        setEntryValues(mimeTypeEntryValues);

        setEnabled(!sortedMimeTypes.isEmpty());
    }
}
