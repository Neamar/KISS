package fr.neamar.kiss.preference;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;

public class ImportSettingsPreference extends DialogPreference {

    private static final String TAG = ImportSettingsPreference.class.getSimpleName();

    public ImportSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            try {
                // Apply changes
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                // Can throw NullPointerException if the application doesn't have focus. Display a toast if this happens
                String clipboardText = clipboard.getPrimaryClip().getItemAt(0).coerceToText(getContext()).toString();

                // Validate JSON
                JSONObject o = new JSONObject(clipboardText);
                if (o.getInt("__v") > BuildConfig.VERSION_CODE) {
                    Toast.makeText(getContext(), "Please upgrade your KISS version before importing those settings.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Reset everything to default
                PreferenceManager.setDefaultValues(getContext(), R.xml.preferences, true);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();

                Iterator<?> keys = o.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.startsWith("__")) {
                        continue;
                    }

                    if (o.get(key) instanceof Boolean) {
                        editor.putBoolean(key, o.getBoolean(key));
                    } else if (o.get(key) instanceof String) {
                        editor.putString(key, o.getString(key));
                    } else if (o.get(key) instanceof JSONArray) {
                        JSONArray a = o.getJSONArray(key);
                        HashSet<String> s = new HashSet<>(a.length());
                        for (int i = 0; i < a.length(); i++) {
                            s.add(a.getString(i));
                        }
                        editor.putStringSet(key, s);
                    }
                }
                editor.apply();

                // Import tags
                if (o.has("__tags")) {
                    DataHandler dataHandler = ((KissApplication) getContext().getApplicationContext()).getDataHandler();
                    TagsHandler tagHandler = dataHandler.getTagsHandler();
                    tagHandler.clearTags();
                    JSONObject tags = o.getJSONObject("__tags");
                    Iterator<?> tagKeys = tags.keys();
                    while (tagKeys.hasNext()) {
                        String id = (String) tagKeys.next();
                        tagHandler.setTags(id, tags.getString(id));
                    }
                    dataHandler.reloadApps();
                    dataHandler.reloadShortcuts();
                }

                Toast.makeText(getContext(), "Preferences imported!", Toast.LENGTH_SHORT).show();
            } catch (JSONException | NullPointerException e) {
                Log.e(TAG, "Unable to import preferences", e);
                Toast.makeText(getContext(), "Unable to import preferences", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
