package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final Widget widgetForwarder;
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaks;
    private final ExperienceTweaks experienceTweaks;
    private final Favorites favoritesForwarder;
    private final Permission permissionForwarder;
    private final OreoShortcuts shortcutsForwarder;
    private final TagsMenu tagsMenu;
    private final Notification notificationForwarder;


    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetForwarder = new Widget(mainActivity);
        this.interfaceTweaks = new InterfaceTweaks(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.experienceTweaks = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
        this.permissionForwarder = new Permission(mainActivity);
        this.shortcutsForwarder = new OreoShortcuts(mainActivity);
        this.notificationForwarder = new Notification(mainActivity);
        this.tagsMenu = new TagsMenu(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        widgetForwarder.onCreate();
        interfaceTweaks.onCreate();
        experienceTweaks.onCreate();
        shortcutsForwarder.onCreate();
        tagsMenu.onCreate();

    }

    public void onStart() {
        widgetForwarder.onStart();
    }

    public void onResume() {
        interfaceTweaks.onResume();
        experienceTweaks.onResume();
        notificationForwarder.onResume();
        tagsMenu.onResume();
    }

    public void onPause() {
        notificationForwarder.onPause();
    }

    public void onStop() {
        widgetForwarder.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        widgetForwarder.onActivityResult(requestCode, resultCode, data);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        permissionForwarder.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetForwarder.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        widgetForwarder.onCreateContextMenu(menu, v, menuInfo);
    }

    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaks.onTouch(view, event); // always return false anyway
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        experienceTweaks.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
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

    public boolean onMenuButtonClicked(View menuButton) {
        if (tagsMenu.isTagMenuEnabled()) {
            mainActivity.registerPopup(tagsMenu.showMenu(menuButton));
            return true;
        }
        return false;
    }

    public void onWallpaperScroll(float fCurrent) {
        widgetForwarder.onWallpaperScroll(fCurrent);
    }
}
