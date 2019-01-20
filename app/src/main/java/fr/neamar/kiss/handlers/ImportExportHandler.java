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

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

public class ImportExportHandler {
    enum ExportType {
    Tags, Settings
    }

    private static final String KISS_SETTINGS_FILE = "kiss.exported.settings.xml";
    private static final String KISS_TAGS_FILE = "kiss.exported.tags.xml";

    public static boolean saveSharedPreferencesToFile(Context context, SharedPreferences prefs) {
        return exportObjectToFile(context, ExportType.Settings, prefs.getAll());
    }

    public static boolean saveTagsToFile(Context context) {
        return exportObjectToFile(context, ExportType.Settings, DBHelper.loadTags(context));
    }

    private static boolean exportObjectToFile(Context context, ExportType exportType, Object toBeExported) {
        String filename = exportType == ExportType.Tags ? KISS_TAGS_FILE: KISS_SETTINGS_FILE;
        File destinationFile = new File(Environment.getExternalStorageDirectory(), filename);

        //TODO: try-with-resources when min api >=19
        ObjectOutputStream output = null;
        try {
            Log.i("Exporting", "Saving at: " +destinationFile.getAbsolutePath());

            //create if not exist
            destinationFile.createNewFile();
            output = new ObjectOutputStream(new FileOutputStream(destinationFile));
            output.writeObject(toBeExported);
            output.flush();
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_LONG).show();
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.export_error, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.export_error, Toast.LENGTH_LONG).show();
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

    private static Map<String, ?> loadObjectFromFile(ExportType exportType) throws IOException, ClassNotFoundException {
        String filename = exportType == ExportType.Tags ? KISS_TAGS_FILE: KISS_SETTINGS_FILE;
        File settingsFile = new File(Environment.getExternalStorageDirectory(), filename);

        ObjectInputStream input = new ObjectInputStream(new FileInputStream(settingsFile));
            return (Map<String, ?>) input.readObject();
    }

    public static boolean loadTagsFromFile(Context context) {
        try {
            Map<String, ?> tags = loadObjectFromFile(ExportType.Settings);
            TagsHandler handler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();

            if (handler!=null) {
                handler.deleteAllTags();
                for (Map.Entry<String, ?> tagPair : tags.entrySet()) {
                    String tag = (String) tagPair.getValue();
                    String app = tagPair.getKey();
                    handler.setTags(app, tag);
                }
            }
            handler.reload();
            if (KissApplication.getApplication(context).getDataHandler().getAppProvider() != null) {
                KissApplication.getApplication(context).getDataHandler().getAppProvider().reload();
            }
            Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show();
            return true;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public static boolean loadSharedPreferencesFromFile(Context context, SharedPreferences sharedPreferences) {

        try {
            Map<String, ?> entries = loadObjectFromFile(ExportType.Settings);

            SharedPreferences.Editor prefEdit = sharedPreferences.edit();
            prefEdit.clear();

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

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show();
        }

        return false;
    }
}
