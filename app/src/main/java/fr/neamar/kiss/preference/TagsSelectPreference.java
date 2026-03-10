package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;

public class TagsSelectPreference extends MultiSelectListPreference {

    public TagsSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public TagsSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public TagsSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public TagsSelectPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    private void setEntries() {
        // get all possible tags
        Set<String> tagsSet = getDataHandler()
                .getTagsHandler()
                .getAllTagsAsSet();

        // make sure we can toggle off the tags that are in the favs now
        Set<String> selectedTags = getSelectedTags();
        tagsSet.addAll(selectedTags);

        String[] tagArray = tagsSet.toArray(new String[0]);
        Arrays.sort(tagArray);
        setEntries(tagArray);
        setEntryValues(tagArray);

        setEnabled(!tagsSet.isEmpty());
    }

    private Set<String> getSelectedTags() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getStringSet(getKey(), Collections.emptySet());
    }

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(getContext()).getDataHandler();
    }
}
