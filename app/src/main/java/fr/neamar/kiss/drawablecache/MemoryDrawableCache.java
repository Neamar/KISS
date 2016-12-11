package fr.neamar.kiss.drawablecache;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Shamelessly inspired from https://github.com/thest1/LazyList 's implementation
 *
 * @see <a href="https://github.com/thest1/LazyList/blob/master/src/com/fedorvlasov/lazylist/MemoryCache.java">Original code</a>
 */
public class MemoryDrawableCache implements DrawableCache {

    private static final String TAG = "MemoryCache";
    private Map<String, BitmapDrawable> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, BitmapDrawable>(10, 1.5f, true)); // Last argument true for LRU ordering
    private long size = 0; // Current allocated size
    private long limit = 1000000; // Max memory in bytes

    public MemoryDrawableCache() {
        // Use 25% of available heap size
        setLimit(Runtime.getRuntime().maxMemory() / 4);
    }

    @SuppressWarnings("WeakerAccess")
    public void setLimit(long limit) {
        this.limit = limit;
        Log.i(TAG, "MemoryDrawableCache will use up to " + this.limit / 1024. / 1024. + "MB");
    }

    /**
     * @see DrawableCache#clear()
     */
    @Override
    public boolean clear() {
        try {
            // NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78
            cache.clear();
            size = 0;
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @see DrawableCache#isCached(String)
     */
    @Override
    public boolean isCached(String key) {
        return cache.containsKey(key);
    }

    /**
     * @see DrawableCache#put(String, BitmapDrawable)
     */
    @Override
    public boolean put(String key, BitmapDrawable bitmap) {
        try {
            if (cache.containsKey(key))
                size -= getSizeInBytes(cache.get(key));
            cache.put(key, bitmap);
            size += getSizeInBytes(bitmap);
            checkSize();
        }
        catch (Throwable th) {
            th.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @see DrawableCache#get(String)
     */
    @Override
    public BitmapDrawable get(String key) {
        try {
            if (!cache.containsKey(key)) {
                return null;
            }
            // NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78
            return cache.get(key);
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void checkSize() {
        if (size > limit) {
            // Least recently accessed item will be the first one iterated
            Iterator<Entry<String, BitmapDrawable>> iter = cache.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, BitmapDrawable> entry = iter.next();
                size -= getSizeInBytes(entry.getValue());
                iter.remove();
                if (size <= limit) {
                    break;
                }
            }
        }
    }

    private long getSizeInBytes(BitmapDrawable drawable) {
        if (drawable == null) {
            return 0;
        }
        return drawable.getBitmap().getWidth() * drawable.getBitmap().getHeight();
    }
}
