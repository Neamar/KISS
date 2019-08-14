package fr.neamar.kiss.shortcut;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.utils.ShortcutUtil;

@TargetApi(Build.VERSION_CODES.O)
public class SaveOreoShortcutAsync extends AsyncTask<Void, Void, Boolean> {
    final static private String TAG = "SaveOreoShortcutAsync";
    private final WeakReference<Context> context;
    private final WeakReference<DataHandler> dataHandler;
    private final WeakReference<LauncherApps> launcherApps;
    private final WeakReference<ApplicationInfo> applicationInfo;

    public SaveOreoShortcutAsync(@NonNull Context context) {
        this.context = new WeakReference<>(context);
        this.dataHandler = new WeakReference<>(KissApplication.getApplication(context).getDataHandler());
        this.applicationInfo = new WeakReference<>(KissApplication.getApplication(context).getApplicationInfo());
        launcherApps = new WeakReference<>((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        Activity activity = (Activity) context.get();
        if(activity == null){
            cancel(true);
            return null;
        }

        List<ShortcutInfo> shortcuts;
        try {
            // Fetch list of all shortcuts
            shortcuts = ShortcutUtil.getAllShortcuts(activity);
        } catch (SecurityException e) {
            e.printStackTrace();
            activity.runOnUiThread(() -> Toast.makeText(activity, R.string.cant_pin_shortcut, Toast.LENGTH_LONG).show());

            // set flag to true, so we can rerun this class
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            prefs.edit().putBoolean("firstRunShortcuts", true).apply();

            cancel(true);
            return null;
        }

        for (ShortcutInfo shortcutInfo : shortcuts) {

            final DataHandler dataHandler = this.dataHandler.get();
            if (dataHandler == null) {
                cancel(true);
                return null;
            }

            // Create Pojo
            ShortcutsPojo pojo = ShortcutUtil.createShortcutPojo(activity, shortcutInfo);
            if(pojo == null){
                continue;
            }
            // Add shortcut to the DataHandler
            dataHandler.addShortcut(pojo);
        }

        return true;
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        final Context context = this.context.get();
        if (context != null && success) {
            Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_LONG).show();

            if (this.dataHandler.get().getShortcutsProvider() != null) {
                this.dataHandler.get().getShortcutsProvider().reload();
            }
        }
    }

}
