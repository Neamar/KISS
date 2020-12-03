package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.icons.IconPackXML;
import fr.neamar.kiss.icons.SystemIconPack;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;

/**
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

public class IconsHandler {

    private static final String TAG = "IconsHandler";
    // map with available icons packs
    private final HashMap<String, String> iconsPacks = new HashMap<>();

    private final PackageManager pm;
    private final Context ctx;
    private IconPackXML mIconPack = null;
    private SystemIconPack mSystemPack = new SystemIconPack();
    private boolean mForceAdaptive = false;
    private boolean mContactPackMask = false;
    private int mContactsShape = DrawableUtils.SHAPE_SYSTEM;
    private boolean mForceShape = false;


    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        clearOldCache();
        loadAvailableIconsPacks();
        loadIconsPack();
    }

    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        onPrefChanged(prefs);
        
    }

    /**
     * Set values from preferences
     */
    public void onPrefChanged(SharedPreferences pref) {
        loadIconsPack(pref.getString("icons-pack", null));
        mSystemPack.setAdaptiveShape(getAdaptiveShape(pref, "adaptive-shape"));
        mForceAdaptive = pref.getBoolean("force-adaptive", true);
        mForceShape = pref.getBoolean("force-shape", true);

        mContactPackMask = pref.getBoolean("contact-pack-mask", true);
        mContactsShape = getAdaptiveShape(pref, "contacts-shape");

        //mShortcutPackMask = pref.getBoolean("shortcut-pack-mask", true);
        //mShortcutsShape = getAdaptiveShape(pref, "shortcut-shape");

        //mShortcutBadgePackMask = pref.getBoolean("shortcut-pack-badge-mask", true);
    }

    private static int getAdaptiveShape(SharedPreferences pref, String key) {
        try {
            return Integer.parseInt(pref.getString(key, null));
        } catch (Exception ignored) {
        }
        return DrawableUtils.SHAPE_SYSTEM;
    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    void loadIconsPack(String packageName) {

        //clear icons pack
        mIconPack = null;
        cacheClear();

        // system icons, nothing to do
        if (packageName == null || packageName.equalsIgnoreCase("default")) {
            return;
        }

        mIconPack = new IconPackXML(packageName);
        mIconPack.load(ctx.getPackageManager());
    }


    /**
     * Get or generate icon for an app
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandle userHandle) {
        final String componentString = componentName.toString();

        // Search in cache
        {
            Drawable cacheIcon = cacheGetDrawable(componentString);
            if (cacheIcon != null)
                return cacheIcon;
        }

        // check the icon pack for a resource
        if (mIconPack != null) {
            // just checking will make this thread wait for the icon pack to load
            if (!mIconPack.isLoaded())
                return null;
            Drawable iconPackDrawable = mIconPack.getComponentDrawable(componentString);
            if (iconPackDrawable != null) {
                Drawable drawable;

                if (DrawableUtils.isAdaptiveIconDrawable(iconPackDrawable) || mForceAdaptive) {
                    int shape = mSystemPack.getAdaptiveShape();
                    drawable = DrawableUtils.applyIconMaskShape(ctx, iconPackDrawable, shape, true);
                } else
                    drawable = mIconPack.applyBackgroundAndMask(ctx, iconPackDrawable, false);
                storeDrawable(cacheGetFileName(componentString), drawable);
                return drawable;
            }
        }

        // if icon pack doesn't have the drawable, use system drawable
        Drawable systemIcon = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
        if (systemIcon == null)
            return null;

        // if the icon pack has a mask, use that instead of the adaptive shape
        if (mIconPack != null && mIconPack.hasMask()) {
            Drawable drawable = mIconPack.applyBackgroundAndMask(ctx, systemIcon, false);
            storeDrawable(cacheGetFileName(componentString), drawable);
            return drawable;
        }

        Drawable drawable;
        // use adaptive shape
        if (DrawableUtils.isAdaptiveIconDrawable(systemIcon) || mForceAdaptive)
            drawable = mSystemPack.applyBackgroundAndMask(ctx, systemIcon, true);
        else if (mForceShape)
            drawable = mSystemPack.applyBackgroundAndMask(ctx, systemIcon, false);
        else
            drawable = systemIcon;

        storeDrawable(cacheGetFileName(componentString), drawable);
        return drawable;
    }

    public Drawable applyContactMask(@NonNull Context ctx, @NonNull Drawable drawable) {
        if (!mContactPackMask)
            return DrawableUtils.applyIconMaskShape(ctx, drawable, mContactsShape, false);
        if (mIconPack != null && mIconPack.hasMask())
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        // if pack has no mask, make it a circle
        int size = ctx.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Path path = new Path();
        int h = size / 2;
        path.addCircle(h, h, h, Path.Direction.CCW);
        c.clipPath(path);
        drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        drawable.draw(c);
        return new BitmapDrawable(ctx.getResources(), b);
    }

    /**
     * Scan for installed icons packs
     */
    private void loadAvailableIconsPacks() {

        List<ResolveInfo> launcherthemes = pm.queryIntentActivities(new Intent("fr.neamar.kiss.THEMES"), PackageManager.GET_META_DATA);
        List<ResolveInfo> adwlauncherthemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

        launcherthemes.addAll(adwlauncherthemes);

        for (ResolveInfo ri : launcherthemes) {
            String packageName = ri.activityInfo.packageName;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String name = pm.getApplicationLabel(ai).toString();
                iconsPacks.put(packageName, name);
            } catch (PackageManager.NameNotFoundException e) {
                // shouldn't happen
                Log.e(TAG, "Unable to found package " + packageName + e);
            }
        }
    }

    HashMap<String, String> getIconsPacks() {
        return iconsPacks;
    }

    @Nullable
    public IconPackXML getCustomIconPack() {
        return mIconPack;
    }

    @NonNull
    public SystemIconPack getSystemIconPack() {
        return mSystemPack;
    }

    @NonNull
    public IconPack getIconPack() {
        return mIconPack != null ? mIconPack : mSystemPack;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    private void storeDrawable(File drawableFile, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable in cache " + e);
            }
        }
    }

    private Drawable cacheGetDrawable(String key) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        String iconsPackPackageName = getIconPack().getPackPackageName();
        return new File(getIconsCacheDir(), iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        File dir = new File(this.ctx.getCacheDir(), "icons");
        if (!dir.exists() && !dir.mkdir())
            throw new IllegalStateException("failed to create path " + dir.getPath());
        return dir;
    }

    private File customIconFileName(String componentName, long customIcon) {
        return new File(getCustomIconsDir(), customIcon + "_" + componentName.hashCode() + ".png");
    }

    private File getCustomIconsDir() {
        File dir = new File(this.ctx.getCacheDir(), "custom_icons");
        if (!dir.exists() && !dir.mkdir())
            throw new IllegalStateException("failed to create path " + dir.getPath());
        return dir;
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        File[] fileList = cacheDir.listFiles();
        if (fileList != null) {
            for (File item : fileList) {
                if (!item.delete()) {
                    Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
                }
            }
        }
    }

    // Before we fixed the cache path actually returning a folder, a lot of icons got dumped
    // directly in ctx.getCacheDir() so we need to clean it
    private void clearOldCache() {
        File newCacheDir = new File(this.ctx.getCacheDir(), "icons");

        if (!newCacheDir.isDirectory()) {
            File[] fileList = ctx.getCacheDir().listFiles();
            if (fileList != null) {
                int count = 0;
                for (File file : fileList) {
                    if (file.isFile())
                        count += file.delete() ? 1 : 0;
                }
                Log.i(TAG, "Removed " + count + " cache file(s) from the old path");
            }
        }
    }

    public Drawable getCustomIcon(String componentName, long customIcon) {
        if (customIcon == 0)
            return null;

        try {
            FileInputStream fis = new FileInputStream(customIconFileName(componentName, customIcon));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get custom icon " + e);
        }

        return null;
    }

    private void removeStoredDrawable(@NonNull File drawableFile) {
        try {
            //noinspection ResultOfMethodCallIgnored
            drawableFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "stored drawable " + drawableFile + " can't be deleted!", e);
        }
    }

    public void changeAppIcon(AppResult appResult, Drawable drawable) {
        long customIconId = KissApplication.getApplication(ctx).getDataHandler().setCustomAppIcon(appResult.getComponentName());
        storeDrawable(customIconFileName(appResult.getComponentName(), customIconId), drawable);
        appResult.setCustomIcon(customIconId, drawable);
    }

    public void restoreAppIcon(AppResult appResult) {
        long customIconId = KissApplication.getApplication(ctx).getDataHandler().removeCustomAppIcon(appResult.getComponentName());
        removeStoredDrawable(customIconFileName(appResult.getComponentName(), customIconId));
        appResult.clearCustomIcon();
    }
}
