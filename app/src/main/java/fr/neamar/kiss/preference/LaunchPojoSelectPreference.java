package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.NameComparator;

public class LaunchPojoSelectPreference extends ListPreference {
    public LaunchPojoSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public LaunchPojoSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public LaunchPojoSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public LaunchPojoSelectPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    private void setEntries() {
        List<AppPojo> appPojoList = getDataHandler().getApplications();
        if (appPojoList == null)
            appPojoList = Collections.emptyList();

        // appPojoList is a copy of the original list; we can sort it in place
        Collections.sort(appPojoList, new NameComparator());

        // generate entry names and entry values
        final int appCount = appPojoList.size();
        CharSequence[] entries = new CharSequence[appCount];
        CharSequence[] entryValues = new CharSequence[appCount];
        for (int idx = 0; idx < appCount; idx++) {
            AppPojo appEntry = appPojoList.get(idx);
            entries[idx] = appEntry.getName();
            entryValues[idx] = appEntry.id;
        }

        setEntries(entries);
        setEntryValues(entryValues);

        setEnabled(!appPojoList.isEmpty());
    }

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(getContext()).getDataHandler();
    }

}