package fr.neamar.kiss.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

public class ImportExportHandler {

    private static final String KISS_SETTINGS_FILE = "kiss.exported.settings.xml";
    private static final String KISS_TAGS_FILE = "kiss.exported.tags.csv";

    public static boolean saveSharedPreferencesToFile(Context context, SharedPreferences prefs) {
        File destinationFile = new File(Environment.getExternalStorageDirectory(), KISS_SETTINGS_FILE);

        //TODO: try-with-resources when min api >=19
        ObjectOutputStream output = null;
        try {
            Log.i("Exporting", "Saving at: " +destinationFile.getAbsolutePath());

            //create if not exist
            destinationFile.createNewFile();
            output = new ObjectOutputStream(new FileOutputStream(destinationFile));
            output.writeObject(prefs.getAll());
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

    public static boolean saveTagsToFile(Context context) {
        Map<String, String> tags = DBHelper.loadTags(context);
        File destinationFile = new File(Environment.getExternalStorageDirectory(), KISS_TAGS_FILE);
        Log.i("Exporting", "Saving at: " +destinationFile.getAbsolutePath());
        try
        {
            //create if not exist
            destinationFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            for (Map.Entry<String, String> tagPair : tags.entrySet()) {
                writer.append(tagPair.getKey() + "," + tagPair.getValue() + "\n");
            }
            writer.close();

            fileOutputStream.flush();
            fileOutputStream.close();
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_LONG).show();
            return true;
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
            e.printStackTrace();
            Toast.makeText(context, R.string.export_error, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private static Map<String, String> loadFromFile(Context context) throws IOException {
        Map<String, String> tags = new HashMap<>();

        File destinationFile = new File(Environment.getExternalStorageDirectory(), KISS_TAGS_FILE);
        Log.i("Importing", "Load from: " +destinationFile.getAbsolutePath());

        FileInputStream fileInputStream = new FileInputStream(destinationFile);
        InputStreamReader readerStream = new InputStreamReader(fileInputStream);
        BufferedReader reader = new BufferedReader(readerStream);
        String line;
        while ((line = reader.readLine()) != null) {
            int pos = line.indexOf(",");
            String id = line.substring(0, pos);
            String tag = line.substring(pos + 1);
            tags.put(id, tag);
        }
        return tags;
    }

    public static boolean loadTagsFromFile(Context context) {
        try {
            Log.i("Importing", "Load tags ");
            TagsHandler handler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
            Map<String, String> existingTags = handler.getTagsCache();
            Map<String, String> foundTags = loadFromFile(context);
            if (handler!=null) {
//                handler.deleteAllTags();
                for (Map.Entry<String, String> tagPair : foundTags.entrySet()) {
                    String loadedTag = tagPair.getValue();
                    String app = tagPair.getKey();
                    if (existingTags.containsKey(app)) {
                        //merge tags
                        String[] loadedTagsForApp = loadedTag.split(" ");
                        String[] existingTagsForApp = existingTags.get(app).split(" ");
                        Set<String> mergedTags = new HashSet<>();
                        for (String tag : loadedTagsForApp) {
                            mergedTags.add(tag);
                        }
                        for (String tag : existingTagsForApp) {
                            mergedTags.add(tag);
                        }
                        //TODO: String.join when minApi >26
                        StringBuffer mergedTag = new StringBuffer();
                        for (String s : mergedTags) {
                            mergedTag.append(s + " ");
                        }
                        handler.setTags(app, mergedTag.toString());
                    }
                    else {
                        //add
                        handler.setTags(app, loadedTag);
                    }
                }
            }
            handler.reload();
            if (KissApplication.getApplication(context).getDataHandler().getAppProvider() != null) {
                KissApplication.getApplication(context).getDataHandler().getAppProvider().reload();
            }
            Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show();
        }

        return false;
    }

    public static boolean loadSharedPreferencesFromFile(Context context, SharedPreferences sharedPreferences) {

        try {
            File settingsFile = new File(Environment.getExternalStorageDirectory(), KISS_SETTINGS_FILE);
            Log.i("Importing", "Load shared prefs from: " +settingsFile.getAbsolutePath());

            ObjectInputStream input = new ObjectInputStream(new FileInputStream(settingsFile));

            Map<String, ?> entries = (Map<String, ?>) input.readObject();

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
            Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show();
            return true;

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.import_error, Toast.LENGTH_LONG).show();
        }

        return false;
    }
}
