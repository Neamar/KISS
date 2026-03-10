package fr.neamar.kiss.preference;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;

public class ImportSettingsPreference {

    private static final String TAG = ImportSettingsPreference.class.getSimpleName();

    public void onDialogClosed(Context context, boolean positiveResult) {
        if (positiveResult) {
            try {
                // Apply changes
                ClipboardManager clipboard = ContextCompat.getSystemService(context, ClipboardManager.class);
                // Can throw NullPointerException if the application doesn't have focus. Display a toast if this happens
                String clipboardText = clipboard.getPrimaryClip().getItemAt(0).coerceToText(context).toString();

                // Validate JSON
                JSONObject jsonObject = new JSONObject(clipboardText);
                int minVersion = jsonObject.optInt("__v", -1);
                if (minVersion < 0) {
                    Toast.makeText(context, R.string.import_settings_version_missing, Toast.LENGTH_LONG).show();
                    return;
                } else if (minVersion > BuildConfig.VERSION_CODE) {
                    Toast.makeText(context, R.string.import_settings_upgrade_kiss, Toast.LENGTH_LONG).show();
                    return;
                }

                // Reset everything to default
                SharedPreferences oldPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (oldPrefs.edit().clear().commit()) {
                    PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
                }

                // Set imported values
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                Iterator<?> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.startsWith("__")) {
                        continue;
                    }

                    Object newValue = jsonObject.get(key);
                    Object currentValue = prefs.getAll().get(key);
                    if (newValue instanceof Boolean) {
                        if (hasMatchingType(key, currentValue, Boolean.class)) {
                            editor.putBoolean(key, (Boolean) newValue);
                        }
                    } else if (newValue instanceof String) {
                        if (hasMatchingType(key, currentValue, String.class)) {
                            editor.putString(key, (String) newValue);
                        }
                    } else if (newValue instanceof JSONArray) {
                        if (hasMatchingType(key, currentValue, Set.class)) {
                            JSONArray newValues = (JSONArray) newValue;
                            Set<String> unwrappedValues = new HashSet<>(newValues.length());
                            for (int i = 0; i < newValues.length(); i++) {
                                unwrappedValues.add(newValues.getString(i));
                            }
                            editor.putStringSet(key, unwrappedValues);
                        }
                    } else {
                        Log.w(TAG, "Unknown type: " + key + ":" + newValue);
                    }
                }
                // always commit preferences to ensure that changes are saved synchronously before continuing
                if (!editor.commit()) {
                    Toast.makeText(context, R.string.import_settings_save_not_possible, Toast.LENGTH_SHORT).show();
                }

                DataHandler dataHandler = ((KissApplication) context.getApplicationContext()).getDataHandler();

                // Import tags
                if (jsonObject.has("__tags")) {
                    TagsHandler tagHandler = dataHandler.getTagsHandler();
                    tagHandler.clearTags();
                    JSONObject tags = jsonObject.getJSONObject("__tags");
                    Iterator<?> tagKeys = tags.keys();
                    while (tagKeys.hasNext()) {
                        String id = (String) tagKeys.next();
                        tagHandler.setTags(id, tags.getString(id));
                    }
                }

                dataHandler.reloadApps();
                dataHandler.reloadShortcuts();
                dataHandler.reloadSearchProvider();
                dataHandler.reloadContactsProvider();

                Toast.makeText(context, R.string.import_settings_done, Toast.LENGTH_SHORT).show();
            } catch (JSONException | NullPointerException e) {
                Log.e(TAG, "Unable to import preferences", e);
                Toast.makeText(context, R.string.import_settings_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @param key          preference key
     * @param currentValue current value of preference
     * @param expectedType expected type of preference
     * @return true, if preference value is null or preference value matches expected type
     */
    private boolean hasMatchingType(String key, Object currentValue, Class<?> expectedType) {
        boolean isValid = currentValue == null || expectedType.isAssignableFrom(currentValue.getClass());
        if (!isValid) {
            Log.w(TAG, "Invalid type for " + key + ": expected" + currentValue.getClass().getSimpleName() + " but was " + expectedType.getSimpleName());
        }
        return isValid;
    }

}
