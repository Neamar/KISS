package fr.neamar.kiss.forwarder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class OreoShortcuts extends Forwarder {
    private static final String TAG = "OreoShortcuts";

    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = mainActivity.getIntent();
            if (intent != null) {
                final String action = intent.getAction();
                if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                    createOreoShortcut(mainActivity, intent);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createOreoShortcut(Context context, Intent intent) {
        DataHandler dh = KissApplication.getApplication(context).getDataHandler();
        ShortcutsProvider sp = dh.getShortcutsProvider();

        if (sp == null) {
            Log.e(TAG, "Shortcuts disabled.");
            Toast.makeText(mainActivity, R.string.unable_add_shortcut, Toast.LENGTH_LONG).show();
            return;
        }


        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();
        assert shortcutInfo != null;

        Log.d(TAG, "Shortcut: " + shortcutInfo.getPackage() + " " + shortcutInfo.getId());


        ShortcutsPojo pojo = new ShortcutsPojo();
        // id isn't used after being saved in the DB.
        pojo.id = ShortcutsPojo.SCHEME + ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();
        pojo.packageName = shortcutInfo.getPackage();

        // Name can be either in shortLabel or longLabel
        if (shortcutInfo.getShortLabel() != null) {
            pojo.setName(shortcutInfo.getShortLabel().toString());
        } else if (shortcutInfo.getLongLabel() != null) {
            pojo.setName(shortcutInfo.getLongLabel().toString());
        } else {
            Log.d(TAG, "Invalid shortcut " + pojo.id + ", ignoring");
            return;
        }

        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
        pojo.icon = drawableToBitmap(iconDrawable);

        pojo.setOreoId(shortcutInfo.getId());

        // Add shortcut to the DataHandler
        dh.addShortcut(pojo);

        // Notify we accepted the shortcut
        pinItemRequest.accept();
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
