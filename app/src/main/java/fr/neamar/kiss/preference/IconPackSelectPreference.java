package fr.neamar.kiss.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import java.util.Map;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class IconPackSelectPreference extends ListPreference {

    public IconPackSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public IconPackSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public IconPackSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public IconPackSelectPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    private void setEntries() {
        Map<String, String> iconsPacks = KissApplication.getApplication(getContext()).getIconsHandler().getIconsPacks();

        CharSequence[] entries = new CharSequence[iconsPacks.size() + 1];
        CharSequence[] entryValues = new CharSequence[iconsPacks.size() + 1];

        // add default entry first
        entries[0] = getContext().getString(R.string.icons_pack_default_name);
        entryValues[0] = "default";

        // add all other icons packs
        int i = 1;
        for (String packageIconsPack : iconsPacks.keySet()) {
            entries[i] = iconsPacks.get(packageIconsPack);
            entryValues[i] = packageIconsPack;
            i++;
        }

        setEntries(entries);
        setEntryValues(entryValues);
        setDefaultValue("default");
    }
}
