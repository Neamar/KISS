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
import java.util.Map;

import fr.neamar.kiss.db.AppRecord;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.icons.IconPackXML;
import fr.neamar.kiss.icons.SystemIconPack;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;

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
    private final SystemIconPack mSystemPack = new SystemIconPack();
    private boolean mForceAdaptive = false;
    private boolean mContactPackMask = false;
    private int mContactsShape = DrawableUtils.SHAPE_SYSTEM;
    private boolean mForceShape = false;
    private Utilities.AsyncRun mLoadIconsPackTask = null;
    private Map<String, Long> customIconIds = null;

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
        onPrefChanged(prefs, "icons-pack");
    }

    /**
     * Set values from preferences
     */
    public void onPrefChanged(SharedPreferences pref, String key) {
        if (key.equalsIgnoreCase("icons-pack") ||
                key.equalsIgnoreCase("adaptive-shape") ||
                key.equalsIgnoreCase("force-adaptive") ||
                key.equalsIgnoreCase("force-shape") ||
                key.equalsIgnoreCase("contact-pack-mask") ||
                key.equalsIgnoreCase("contacts-shape")) {
            cacheClear();
            loadIconsPack(pref.getString("icons-pack", null));
            mSystemPack.setAdaptiveShape(getAdaptiveShape(pref, "adaptive-shape"));
            mForceAdaptive = pref.getBoolean("force-adaptive", true);
            mForceShape = pref.getBoolean("force-shape", true);

            mContactPackMask = pref.getBoolean("contact-pack-mask", true);
            mContactsShape = getAdaptiveShape(pref, "contacts-shape");
        }
    }

    private static int getAdaptiveShape(SharedPreferences pref, String key) {
        try {
            return Integer.parseInt(pref.getString(key, String.valueOf(DrawableUtils.SHAPE_SYSTEM)));
        } catch (Exception ignored) {
        }
        return DrawableUtils.SHAPE_SYSTEM;
    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    private void loadIconsPack(String packageName) {
        // system icons, nothing to do
        if (packageName == null || packageName.equalsIgnoreCase("default")) {
            cacheClear();
            mIconPack = null;
            return;
        }

        // don't reload the icon pack
        if (mIconPack == null || !mIconPack.getPackPackageName().equals(packageName)) {
            cacheClear();
            if (mLoadIconsPackTask != null)
                mLoadIconsPackTask.cancel();
            final IconPackXML iconPack = KissApplication.iconPackCache(ctx).getIconPack(packageName);
            // set the current icon pack
            mIconPack = iconPack;
            // start async loading
            mLoadIconsPackTask = Utilities.runAsync((task) -> {
                if (task == mLoadIconsPackTask)
                    iconPack.load(ctx.getPackageManager());
            }, (task) -> {
                if (!task.isCancelled() && task == mLoadIconsPackTask) {
                    mLoadIconsPackTask = null;
                }
            });
        }
    }


    /**
     * Get or generate icon for an app
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandle userHandle) {
        final String cacheKey = AppPojo.getComponentName(componentName.getPackageName(), componentName.getClassName(), userHandle);

        // Search in cache
        {
            Drawable cacheIcon = cacheGetDrawable(cacheKey);
            if (cacheIcon != null)
                return cacheIcon;
        }

        Drawable drawable = null;

        // search for custom icon
        Map<String, Long> customIconIds = getCustomIconIds();
        if (customIconIds.containsKey(cacheKey)) {
            drawable = getCustomIcon(cacheKey, customIconIds.get(cacheKey));
        }

        // check the icon pack for a resource
        if (drawable == null && mIconPack != null && userHandle.isCurrentUser()) {
            // just checking will make this thread wait for the icon pack to load
            if (!mIconPack.isLoaded())
                return null;
            drawable = mIconPack.getComponentDrawable(ctx, componentName, userHandle);
        }

        if (drawable == null) {
            // if icon pack doesn't have the drawable, use system drawable
            drawable = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
        }
        if (drawable == null)
            return null;

        Drawable drawableWithBackgroundAndMask = applyIconMask(ctx, drawable, userHandle);
        storeDrawable(cacheGetFileName(cacheKey), drawableWithBackgroundAndMask);
        return drawableWithBackgroundAndMask;
    }

    public Drawable applyIconMask(@NonNull Context ctx, @NonNull Drawable drawable, @NonNull UserHandle userHandle) {
        if (mIconPack != null && mIconPack.hasMask() && userHandle.isCurrentUser()) {
            // if the icon pack has a mask, use that instead of the adaptive shape
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        } else if (DrawableUtils.isAdaptiveIconDrawable(drawable) || mForceAdaptive) {
            // use adaptive shape
            return mSystemPack.applyBackgroundAndMask(ctx, drawable, true);
        } else if (mForceShape) {
            // use adaptive shape
            return mSystemPack.applyBackgroundAndMask(ctx, drawable, false);
        } else {
            return drawable;
        }
    }

    public Drawable applyContactMask(@NonNull Context ctx, @NonNull Drawable drawable) {
        final int shape = getContactsShape();

        if (mContactPackMask && mIconPack != null && mIconPack.hasMask()) {
            // if the icon pack has a mask, use that instead of the adaptive shape
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        } else if (DrawableUtils.isAdaptiveIconDrawable(drawable)) {
            // use adaptive shape
            return DrawableUtils.applyIconMaskShape(ctx, drawable, shape, true);
        } else {
            // use adaptive shape
            return DrawableUtils.applyIconMaskShape(ctx, drawable, shape, false);
        }
    }

    /**
     * Get shape used for contact icons with fallbacks.
     * If contacts shape is {@link DrawableUtils#SHAPE_SYSTEM} app shape is used.
     * If app shape is {@link DrawableUtils#SHAPE_SYSTEM} too, used shape is a circle.
     *
     * @return shape
     */
    private int getContactsShape() {
        int shape = mContactsShape;
        if (shape == DrawableUtils.SHAPE_SYSTEM) {
            shape = mSystemPack.getAdaptiveShape();
        }
        if (shape == DrawableUtils.SHAPE_SYSTEM) {
            shape = DrawableUtils.SHAPE_CIRCLE;
        }
        return shape;
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
        clearCustomIconIdCache();

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
        cacheClear();
    }

    public void restoreAppIcon(AppResult appResult) {
        long customIconId = KissApplication.getApplication(ctx).getDataHandler().removeCustomAppIcon(appResult.getComponentName());
        removeStoredDrawable(customIconFileName(appResult.getComponentName(), customIconId));
        appResult.clearCustomIcon();
        cacheClear();
    }

    /**
     * clears cache for custom icon ids
     */
    private void clearCustomIconIdCache() {
        customIconIds = null;
    }

    /**
     * Cache for custom icon ids, maps from component name to custom icon id.
     * Cache is built only if null.
     *
     * @return cache for custom icon ids
     */
    private Map<String, Long> getCustomIconIds() {
        if (customIconIds == null) {
            customIconIds = new HashMap<>();
            Map<String, AppRecord> appData = DBHelper.getCustomAppData(ctx);
            for (Map.Entry<String, AppRecord> entry : appData.entrySet()) {
                if (entry.getValue().hasCustomIcon()) {
                    customIconIds.put(entry.getKey(), entry.getValue().dbId);
                }
            }
        }
        return customIconIds;
    }

}
