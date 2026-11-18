package fr.neamar.kiss.forwarder;

import android.content.res.Configuration;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaks;
    private final ExperienceTweaks experienceTweaks;
    private final Favorites favoritesForwarder;
    private final OreoShortcuts shortcutsForwarder;
    private final Notification notificationForwarder;

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.interfaceTweaks = new InterfaceTweaks(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.experienceTweaks = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
        this.shortcutsForwarder = new OreoShortcuts(mainActivity);
        this.notificationForwarder = new Notification(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        interfaceTweaks.onCreate();
        experienceTweaks.onCreate();
        shortcutsForwarder.onCreate();
    }

    public void onResume() {
        interfaceTweaks.onResume();
        experienceTweaks.onResume();
        notificationForwarder.onResume();
    }

    public void onPause() {
        experienceTweaks.onPause();
        notificationForwarder.onPause();
    }


    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaks.onTouch(event);
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void updateSearchRecords(String query) {
        experienceTweaks.updateSearchRecords(query);
    }

    public void onFavoriteChange() {
        favoritesForwarder.onFavoriteChange();
        experienceTweaks.onFavoriteChange();
    }


    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        interfaceTweaks.onConfigurationChanged(newConfig);
        favoritesForwarder.onConfigurationChanged(newConfig);
    }
}
