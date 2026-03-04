package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.res.Configuration;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final Widgets widgetsForwarder;
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaks;
    private final ExperienceTweaks experienceTweaks;
    private final Favorites favoritesForwarder;
    private final OreoShortcuts shortcutsForwarder;
    private final TagsMenu tagsMenu;
    private final Notification notificationForwarder;

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetsForwarder = new Widgets(mainActivity);
        this.interfaceTweaks = new InterfaceTweaks(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.experienceTweaks = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
        this.shortcutsForwarder = new OreoShortcuts(mainActivity);
        this.notificationForwarder = new Notification(mainActivity);
        this.tagsMenu = new TagsMenu(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        widgetsForwarder.onCreate();
        interfaceTweaks.onCreate();
        experienceTweaks.onCreate();
        shortcutsForwarder.onCreate();
        tagsMenu.onCreate();
    }

    public void onStart() {
        widgetsForwarder.onStart();
    }

    public void onResume() {
        interfaceTweaks.onResume();
        experienceTweaks.onResume();
        notificationForwarder.onResume();
        tagsMenu.onResume();
    }

    public void onPause() {
        experienceTweaks.onPause();
        notificationForwarder.onPause();
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        widgetsForwarder.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetsForwarder.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        widgetsForwarder.onCreateContextMenu(menu);
    }

    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaks.onTouch(event);
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        experienceTweaks.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetsForwarder.onDataSetChanged();
        favoritesForwarder.onDataSetChanged();
    }

    public void updateSearchRecords(String query) {
        favoritesForwarder.updateSearchRecords(query);
        experienceTweaks.updateSearchRecords(query);
    }

    public void onFavoriteChange() {
        favoritesForwarder.onFavoriteChange();
    }

    public void onDisplayKissBar(boolean display) {
        experienceTweaks.onDisplayKissBar(display);
    }

    public boolean onMenuButtonClicked(View menuButton) {
        return tagsMenu.onMenuButtonClicked(menuButton);
    }

    public void onDestroy() {
        widgetsForwarder.onDestroy();
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        interfaceTweaks.onConfigurationChanged(newConfig);
        favoritesForwarder.onConfigurationChanged(newConfig);
    }
}
