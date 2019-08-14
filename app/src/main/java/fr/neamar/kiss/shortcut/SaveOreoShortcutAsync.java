package fr.neamar.kiss.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.utils.DrawableUtils;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

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

        final LauncherApps launcherApps = this.launcherApps.get();
        if (launcherApps == null) {
            cancel(true);
            return null;
        }

        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

        List<ShortcutInfo> shortcuts;

        try {
            shortcuts = launcherApps.getShortcuts(shortcutQuery, UserHandle.getUserHandleForUid(applicationInfo.get().uid));
        } catch (SecurityException e) {
            e.printStackTrace();
            MainActivity mainActivity = (MainActivity) context.get();
            if (mainActivity != null) {
                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity, R.string.cant_pin_shortcut, Toast.LENGTH_SHORT).show());
            }
            cancel(true);
            return null;
        }

        for (ShortcutInfo shortcutInfo : shortcuts) {

            // id isn't used after being saved in the DB.
            String id = ShortcutsPojo.SCHEME + ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();

            final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
            ShortcutsPojo pojo = new ShortcutsPojo(id, shortcutInfo.getPackage(), shortcutInfo.getId(),
                    DrawableUtils.drawableToBitmap(iconDrawable));

            // Name can be either in shortLabel or longLabel
            if (shortcutInfo.getShortLabel() != null) {
                pojo.setName(shortcutInfo.getShortLabel().toString());
            } else if (shortcutInfo.getLongLabel() != null) {
                pojo.setName(shortcutInfo.getLongLabel().toString());
            } else {
                Log.d(TAG, "Invalid shortcut " + pojo.id + ", ignoring");
                cancel(true);
                return null;
            }

            final DataHandler dataHandler = this.dataHandler.get();
            if (dataHandler == null) {
                cancel(true);
                return null;
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
            Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_SHORT).show();

            if (this.dataHandler.get().getShortcutsProvider() != null) {
                this.dataHandler.get().getShortcutsProvider().reload();
            }
        }
    }

}
