package fr.neamar.kiss.preference;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

public class ExportSettingsPreference extends Preference {

    private static final String TAG = ExportSettingsPreference.class.getSimpleName();

    public ExportSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Get default values from XML, to only write changed data
        SharedPreferences defaultValues = getContext().getSharedPreferences("__default__", Context.MODE_PRIVATE);
        PreferenceManager.setDefaultValues(getContext(), "__default__", Context.MODE_PRIVATE, R.xml.preferences, true);
        JSONObject out = new JSONObject();
        try {
            // Min version required to read those settings
            out.put("__v", 183);

            Set<String> keys = new HashSet<>();
            keys.addAll(defaultValues.getAll().keySet());
            keys.addAll(prefs.getAll().keySet());

            // Export settings
            for (String key : keys) {
                Object value = prefs.getAll().get(key);
                if (value instanceof Boolean) {
                    if (defaultValues.contains(key)) {
                        boolean defaultValue = defaultValues.getBoolean(key, true);
                        boolean currentValue = prefs.getBoolean(key, defaultValue);
                        if (currentValue != defaultValue) {
                            out.put(key, currentValue);
                        }
                    } else {
                        out.put(key, value);
                    }
                } else if (value instanceof String) {
                    if (defaultValues.contains(key)) {
                        String defaultValue = defaultValues.getString(key, "");
                        String currentValue = prefs.getString(key, defaultValue);
                        if (!currentValue.equals(defaultValue)) {
                            out.put(key, currentValue);
                        }
                    } else {
                        out.put(key, value);
                    }
                } else if (value instanceof Set) {
                    if (defaultValues.contains(key)) {
                        Set<String> defaultValue = defaultValues.getStringSet(key, new HashSet<>());
                        Set<String> currentValue = prefs.getStringSet(key, new HashSet<>());
                        if (!currentValue.equals(defaultValue)) {
                            out.put(key, new JSONArray(currentValue));
                        }
                    } else {
                        out.put(key, new JSONArray((Set<?>) value));
                    }
                } else {
                    Log.w(TAG, "Unknown type: " + key + ":" + value);
                }
            }

            // Export tags
            Map<String, String> tags = ((KissApplication) getContext().getApplicationContext()).getDataHandler().getTagsHandler().getTags();
            JSONObject jsonTags = new JSONObject();
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                jsonTags.put(entry.getKey(), entry.getValue());
            }
            out.put("__tags", jsonTags);

            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("kiss", out.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), R.string.export_settings_done, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "Unable to export settings", e);
            Toast.makeText(getContext(), R.string.export_settings_error, Toast.LENGTH_SHORT).show();
        } finally {
            defaultValues.edit().clear().apply();
        }
    }
}
