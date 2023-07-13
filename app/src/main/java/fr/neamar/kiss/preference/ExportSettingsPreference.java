package fr.neamar.kiss.preference;

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

import static android.content.Context.CLIPBOARD_SERVICE;

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
            // Export settings
            for (Map.Entry<String, ?> entry : defaultValues.getAll().entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() instanceof Boolean) {
                    boolean currentValue = prefs.getBoolean(key, defaultValues.getBoolean(key, true));
                    if (currentValue != defaultValues.getBoolean(key, true)) {
                        out.put(key, currentValue);
                    }
                } else if (entry.getValue() instanceof String) {
                    String currentValue = prefs.getString(key, defaultValues.getString(key, ""));
                    if (!currentValue.equals(defaultValues.getString(key, ""))) {
                        out.put(key, currentValue);
                    }
                } else if (entry.getValue() instanceof Set) {
                    Set<String> currentValue = prefs.getStringSet(key, new HashSet<String>());
                    if (!currentValue.equals(defaultValues.getStringSet(key, new HashSet<String>()))) {
                        out.put(key, new JSONArray(currentValue));
                    }
                } else {
                    Log.w(TAG, "Unknown type: " + entry.getKey() + ":" + entry.getValue());
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
            Toast.makeText(getContext(), "Settings exported to clipboard", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "Unable to export preferences", e);
            Toast.makeText(getContext(), "Unable to export preferences", Toast.LENGTH_SHORT).show();
        } finally {
            defaultValues.edit().clear().apply();
        }
    }
}
