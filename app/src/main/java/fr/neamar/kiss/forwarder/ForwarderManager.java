package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final Widgets widgetsForwarder;
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaks;
    private final ExperienceTweaks experienceTweaks;
    private final Favorites favoritesForwarder;
    private final Permission permissionForwarder;
    private final OreoShortcuts shortcutsForwarder;
    private final Notification notificationForwarder;


    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetsForwarder = new Widgets(mainActivity);
        this.interfaceTweaks = new InterfaceTweaks(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.experienceTweaks = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
        this.permissionForwarder = new Permission(mainActivity);
        this.shortcutsForwarder = new OreoShortcuts(mainActivity);
        this.notificationForwarder = new Notification(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        widgetsForwarder.onCreate();
        interfaceTweaks.onCreate();
        experienceTweaks.onCreate();
        shortcutsForwarder.onCreate();
    }

    public void onStart() {
        widgetsForwarder.onStart();
    }

    public void onResume() {
        interfaceTweaks.onResume();
        experienceTweaks.onResume();
        notificationForwarder.onResume();
    }

    public void onPause() {
        notificationForwarder.onPause();
    }

    public void onStop() {
        widgetsForwarder.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        widgetsForwarder.onActivityResult(requestCode, resultCode, data);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionForwarder.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetsForwarder.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        widgetsForwarder.onCreateContextMenu(menu);
    }

    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaks.onTouch(view, event); // always return false anyway
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        experienceTweaks.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetsForwarder.onDataSetChanged();
        favoritesForwarder.onDataSetChanged();
    }

    public void updateSearchRecords(boolean isRefresh, String query) {
        favoritesForwarder.updateSearchRecords(query);
        experienceTweaks.updateSearchRecords(isRefresh, query);
    }

    public void onFavoriteChange() {
        favoritesForwarder.onFavoriteChange();
    }

    public void onDisplayKissBar(Boolean display) {
        experienceTweaks.onDisplayKissBar(display);
    }
}
