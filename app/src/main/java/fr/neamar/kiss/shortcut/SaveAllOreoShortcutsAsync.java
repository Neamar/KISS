package fr.neamar.kiss.shortcut;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.utils.ShortcutUtil;

@RequiresApi(Build.VERSION_CODES.O)
public class SaveAllOreoShortcutsAsync extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = SaveAllOreoShortcutsAsync.class.getSimpleName();
    private final WeakReference<Context> context;

    public SaveAllOreoShortcutsAsync(@NonNull Context context) {
        this.context = new WeakReference<>(context);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        Context context = this.context.get();
        if (context == null) {
            cancel(true);
            return null;
        }

        List<ShortcutInfo> shortcuts;
        try {
            // Fetch list of all shortcuts
            shortcuts = ShortcutUtil.getAllShortcuts(context);
        } catch (SecurityException e) {
            e.printStackTrace();

            // Publish progress (display toast)
            publishProgress(-1);

            // Set flag to true, so we can rerun this class
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putBoolean("first-run-shortcuts", true).apply();

            cancel(true);
            return null;
        }

        final DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();

        boolean shortcutsUpdated = false;
        for (ShortcutInfo shortcutInfo : shortcuts) {
            // Add shortcut to the DataHandler
            shortcutsUpdated |= dataHandler.updateShortcut(shortcutInfo);
        }

        return shortcutsUpdated;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            Context context = this.context.get();
            if (context != null) {
                Toast.makeText(context, R.string.cant_pin_shortcut, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        if (success) {
            Log.i(TAG, "Shortcuts added to KISS");

            Context context = this.context.get();
            if (context != null) {
                DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
                ShortcutsProvider provider = dataHandler.getShortcutsProvider();
                if (provider != null) {
                    provider.reload();
                }
            }
        }
    }

}
