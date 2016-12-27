package fr.neamar.kiss.drawablecache;

import android.graphics.drawable.BitmapDrawable;

interface DrawableCache {

    /**
     * Clears the cache
     *
     * @return True if succeeded
     */
    public boolean clear();

    /*
     * @return True if cached
     */
    public boolean isCached(String key);

    /**
     * Store a BitmapDrawable in the disk cache
     *
     * @param key      Unique identifier to build the filename from
     * @param drawable The drawable
     * @return True if succeeded
     */
    public boolean put(String key, BitmapDrawable drawable);

    /**
     * Retrieve a Drawable from the disk cache
     *
     * @param key Unique identifier to build the filename from
     * @return The drawable, null if not found or if an error occurred
     */
    public BitmapDrawable get(String key);
}
