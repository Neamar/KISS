package fr.neamar.kiss.forwarder;

import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.utils.WallpaperUtils;

public class WallpaperForwarder extends Forwarder {
    /**
     * Wallpaper scroll
     */
    private WallpaperUtils mWallpaperUtils;

    public WallpaperForwarder(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);
    }

    @Override
    public void onCreate() {
        mWallpaperUtils = new WallpaperUtils(mainActivity);
    }

    public boolean onTouch(View view, MotionEvent event) {
        return mWallpaperUtils.onTouch(view, event);
    }
}
