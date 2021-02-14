package fr.neamar.kiss.preference;

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

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;

import static android.content.Context.CLIPBOARD_SERVICE;

public class ImportSettingsPreference extends DialogPreference {

    public ImportSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // Apply changes
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
            String clipboardText = clipboard.getPrimaryClip().getItemAt(0).coerceToText(getContext()).toString();
            try {
                JSONObject o = new JSONObject(clipboardText);

                // Reset everything to default (after validating JSON is valid)
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
                        Log.e("WTF", id + ":" + tags.getString(id));
                        tagHandler.setTags(id, tags.getString(id).toLowerCase());
                    }
                    AppProvider appProvider = dataHandler.getAppProvider();
                    if (appProvider != null) {
                        appProvider.reload();
                    }
                    ShortcutsProvider shortcutsProvider = dataHandler.getShortcutsProvider();
                    if(shortcutsProvider != null) {
                        shortcutsProvider.reload();
                    }
                }

                Toast.makeText(getContext(), "Preferences imported!", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Unable to import preferences", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
