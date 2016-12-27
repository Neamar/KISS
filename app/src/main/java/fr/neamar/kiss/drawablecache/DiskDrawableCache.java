package fr.neamar.kiss.drawablecache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class DiskDrawableCache implements DrawableCache {
    private static final String TAG = "DiskDrawableCache";

    private final Context context;

    public DiskDrawableCache(Context context) {
        this.context = context;
    }

    /**
     * Returns the directory for the cached icons
     *
     * @return {cacheDir}/icons/
     */
    private File getIconsCacheDir() {
        return new File(context.getCacheDir().getPath() + "/icons/");
    }

    /**
     * create path for icons cache like this
     *
     * @return getIconsCacheDir()/{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        return new File(getIconsCacheDir().getPath() + key.hashCode() + ".png");
    }

    /**
     * @see DrawableCache#clear()
     */
    @Override
    public boolean clear() {
        File cacheDir = getIconsCacheDir();

        if (!cacheDir.isDirectory()) {
            return false;
        }

        for (File item : cacheDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
            }
        }
        return true;
    }

    /**
     * @see DrawableCache#isCached(String)
     */
    @Override
    public boolean isCached(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    /**
     * @see DrawableCache#put(String, BitmapDrawable)
     */
    @Override
    public boolean put(String key, BitmapDrawable drawable) {
        File drawableFile = cacheGetFileName(key);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(drawableFile);
            ((BitmapDrawable) drawable).getBitmap().compress(Bitmap.CompressFormat.PNG, 80, fos);
            fos.flush();
            fos.close();
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Unable to store drawable in cache " + e);
        }
        return false;
    }

    /**
     * @see DrawableCache#get(String)
     */
    public BitmapDrawable get(String key) {
        if (!isCached(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        }
        catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }
}
