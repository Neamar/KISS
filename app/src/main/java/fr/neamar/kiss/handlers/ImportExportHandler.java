package fr.neamar.kiss.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.R;

public class ImportExportHandler {
    private static final String KISS_SETTINGS_FILE = "kiss.exported.settings.xml";

    public static boolean saveSharedPreferencesToFile(Context context, SharedPreferences prefs) {
        File settingsFile = new File(Environment.getExternalStorageDirectory(), KISS_SETTINGS_FILE);
        //TODO: try-with-resources when min api >=19
        ObjectOutputStream output = null;
        try {
            Log.i("export settings", "Saving at: " +settingsFile.getAbsolutePath());

            //create if not exist
            settingsFile.createNewFile();
            output = new ObjectOutputStream(new FileOutputStream(settingsFile));
            output.writeObject(prefs.getAll());
            output.flush();
            Toast.makeText(context, R.string.export_settings_success, Toast.LENGTH_LONG).show();
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.export_settings_error, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.export_settings_error, Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static boolean loadSharedPreferencesFromFile(Context context, SharedPreferences sharedPreferences) {
        File settingsFile = new File(Environment.getExternalStorageDirectory(), KISS_SETTINGS_FILE);

        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(settingsFile));
            SharedPreferences.Editor prefEdit = sharedPreferences.edit();
            prefEdit.clear();

            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
                else if (v instanceof Set) {
                    prefEdit.putStringSet(key, ((Set<String>) v));
                }
            }
            prefEdit.commit();
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_settings_error, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_settings_error, Toast.LENGTH_LONG).show();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_settings_error, Toast.LENGTH_LONG).show();
        }

        return false;
    }
}
