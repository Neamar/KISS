package fr.neamar.kiss.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ShortcutsPojo;

@TargetApi(Build.VERSION_CODES.O)
public class SaveOreoShortcutAsync extends AsyncTask<Void, Void, Boolean> {
    final static private String TAG = "SaveOreoShortcutAsync";
    private final WeakReference<Context> context;
    private final WeakReference<DataHandler> dataHandler;
    private Intent data;
    private final WeakReference<LauncherApps> launcherApps;

    public SaveOreoShortcutAsync(@NonNull Context context, @NonNull Intent data) {
        this.context = new WeakReference<>(context);
        this.dataHandler = new WeakReference<>(KissApplication.getApplication(context).getDataHandler());
        this.data = data;

        launcherApps = new WeakReference<>((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected void onPreExecute() {
        // Skipping mostly redundant null check for this.dataHandler
        if (dataHandler.get().getShortcutsProvider() == null) {
            Log.e(TAG, "Shortcuts disabled.");
            // Skipping mostly redundant null check for this.context
            Toast.makeText(context.get(), R.string.unable_add_shortcut, Toast.LENGTH_LONG).show();
            cancel(true);
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        final LauncherApps.PinItemRequest pinItemRequest = data.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();
        assert shortcutInfo != null;

        Log.d(TAG, "Shortcut: " + shortcutInfo.getPackage() + " " + shortcutInfo.getId());

        final LauncherApps launcherApps = this.launcherApps.get();
        if (launcherApps == null) {
            cancel(true);
            return null;
        }

        // id isn't used after being saved in the DB.
        String id = ShortcutsPojo.SCHEME + ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();

        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
        ShortcutsPojo pojo = new ShortcutsPojo(id, shortcutInfo.getPackage(), shortcutInfo.getId(),
                drawableToBitmap(iconDrawable));

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
        boolean success = dataHandler.addShortcut(pojo);

        if (success) {
            try {
                pinItemRequest.accept();
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
                Context c = context.get();
                if(c != null) {
                    Toast.makeText(c, R.string.cant_pin_shortcut, Toast.LENGTH_SHORT).show();
                }
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        final Context context = this.context.get();
        if (context != null && success) {
            Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
        }
    }

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
