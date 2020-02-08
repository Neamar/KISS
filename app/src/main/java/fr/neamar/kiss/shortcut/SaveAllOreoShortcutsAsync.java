package fr.neamar.kiss.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

@TargetApi(Build.VERSION_CODES.O)
public class SaveAllOreoShortcutsAsync extends AsyncTask<Void, Integer, Boolean> {

    private static String TAG = "SaveAllOreoShortcutsAsync";
    private final WeakReference<Context> context;
    private final WeakReference<DataHandler> dataHandler;

    public SaveAllOreoShortcutsAsync(@NonNull Context context) {
        this.context = new WeakReference<>(context);
        this.dataHandler = new WeakReference<>(KissApplication.getApplication(context).getDataHandler());
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

        final DataHandler dataHandler = this.dataHandler.get();
        if (dataHandler == null) {
            cancel(true);
            return null;
        }

        Set<String> excludedAppList = dataHandler.getExcluded();
        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        for (ShortcutInfo shortcutInfo : shortcuts) {

            UserHandle user = new UserHandle(manager.getSerialNumberForUser(shortcutInfo.getUserHandle()), shortcutInfo.getUserHandle());
            boolean isExcluded = excludedAppList.contains(AppPojo.getComponentName(shortcutInfo.getPackage(),
                    shortcutInfo.getActivity().getClassName(), user));

            // Skip shortcut if app is excluded
            if (!excludedAppList.isEmpty() &&
                    isExcluded) {
                continue;
            }

            // Create Pojo
            ShortcutRecord record = ShortcutUtil.createShortcutRecord(context, shortcutInfo, !shortcutInfo.isPinned());
            if (record == null) {
                continue;
            }

            // Add shortcut to the DataHandler
            dataHandler.addShortcut(record);
        }

        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            Toast.makeText(context.get(), R.string.cant_save_shortcuts, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        if (success) {
            Log.i(TAG, "Shortcuts added to KISS");

            if (this.dataHandler.get().getShortcutsProvider() != null) {
                this.dataHandler.get().getShortcutsProvider().reload();
            }
        }
    }

}
