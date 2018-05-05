package fr.neamar.kiss.broadcast;

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

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutsPojo;


public class InstallShortcutOreoHandler {
    private static final String TAG = "ShortcutOreoHandler";

    @TargetApi(Build.VERSION_CODES.O)
    public static void handleIntent(Context context, Intent intent) {
        DataHandler dh = KissApplication.getApplication(context).getDataHandler();
        ShortcutsProvider sp = dh.getShortcutsProvider();

        if (sp == null) {
            Log.e(TAG, "Shortcuts disabled.");
            return;
        }


        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        // Only the default launcher is allowed to accept permission
        // we've been called, so we can assume that we're the default launcher,
        // but we might as well confirm.
        // Documentation isn't super clear about this anyway.
        if (!launcherApps.hasShortcutHostPermission()) {
            return;
        }

        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();
        assert shortcutInfo != null;

        Log.d(TAG, "Shortcut: " + shortcutInfo.getPackage() + " " + shortcutInfo.getId());

        if (!shortcutInfo.isEnabled()) {
            return;
        }

        ShortcutsPojo pojo = new ShortcutsPojo();

        pojo.id = ShortcutsPojo.SCHEME + ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();
        pojo.packageName = shortcutInfo.getPackage();

        if (shortcutInfo.getShortLabel() != null) {
            pojo.setName(shortcutInfo.getShortLabel().toString());
        } else if (shortcutInfo.getLongLabel() != null) {
            pojo.setName(shortcutInfo.getLongLabel().toString());
        } else {
            Log.d(TAG, "Invalid shortcut " + pojo.id + ", ignoring");
            return;
        }

        final Drawable iconDrawable = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0);
        pojo.icon = drawableToBitmap(iconDrawable);

        pojo.intentUri =  ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();

        dh.addShortcut(pojo);

        // Notify we accepted the shortcut
        pinItemRequest.accept();
    }

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
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
