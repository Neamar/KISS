package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
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


    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        loadAvailableIconsPacks();
        loadIconsPack();
    }

    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        loadIconsPack(prefs.getString("icons-pack", "default"));

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
        if (packageName.equalsIgnoreCase("default") || DrawableUtils.isIconsPackAdaptive(packageName)) {
            if (!packageName.equals(mSystemPack.getPackPackageName()))
                mSystemPack = new SystemIconPack(packageName);
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
        // system icons, nothing to do
        if (mIconPack == null) {
            Drawable drawable = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
            if (drawable == null)
                return null;
            return mSystemPack.applyBackgroundAndMask(ctx, drawable);
        }

        // Check the icon pack for a resource
        {
            Drawable drawable = mIconPack.getComponentDrawable(componentName.toString());
            if (drawable != null)
                return drawable;
        }

        // Search in cache
        {
            Drawable cacheIcon = cacheGetDrawable(componentName.toString());
            if (cacheIcon != null)
                return cacheIcon;
        }

        // apply icon pack back, mask and front over the system drawable
        Drawable systemIcon = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
        if (systemIcon == null)
            return null;
        BitmapDrawable generated = mIconPack.applyBackgroundAndMask(ctx, systemIcon);
        storeDrawable(cacheGetFileName(componentName.toString()), generated);
        return generated;
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
    public IconPack getCustomIconPack() {
        return mIconPack;
    }

    @NonNull
    public IconPack getSystemIconPack() {
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
        String iconsPackPackageName = mIconPack != null ? mIconPack.getPackPackageName() : mSystemPack.getPackPackageName();
        return new File(getIconsCacheDir(), iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        return new File(this.ctx.getCacheDir().getPath() + "/icons/");
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

        if (!cacheDir.isDirectory())
            return;

        for (File item : cacheDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
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
