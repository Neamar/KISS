package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final Widget widgetForwarder;
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaksProvider;
    private final ExperienceTweaks experienceTweaksProvider;
    private final Favorites favoritesForwarder;

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetForwarder = new Widget(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.interfaceTweaksProvider = new InterfaceTweaks(mainActivity);
        this.experienceTweaksProvider = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        widgetForwarder.onCreate();
        interfaceTweaksProvider.onCreate();
        experienceTweaksProvider.onCreate();
    }

    public void onResume() {
        interfaceTweaksProvider.onResume();
        experienceTweaksProvider.onResume();
    }

    public void onStart() {
        widgetForwarder.onStart();
    }

    public void onStop() {
        widgetForwarder.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        widgetForwarder.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetForwarder.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        widgetForwarder.onCreateContextMenu(menu, v, menuInfo);
    }

    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaksProvider.onTouch(view, event); // always return false anyway
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        experienceTweaksProvider.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
    }

    public void updateRecords(String query) {
        favoritesForwarder.updateRecords(query);
        experienceTweaksProvider.updateRecords(query);
    }

    public void onFavoriteChange() {
        favoritesForwarder.onFavoriteChange();
    }

    public void onDisplayKissBar(Boolean display) {
        experienceTweaksProvider.onDisplayKissBar(display);
    }
}
