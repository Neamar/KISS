package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final WidgetForwarder widgetForwarder;
    private final WallpaperForwarder wallpaperForwarder;
    private final UITweaksForwarder uiTweaksForwarder;
    private final UXTweaksForwarder uxTweaksForwarder;
    private final FavoriteForwarder favoriteForwarder;

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetForwarder = new WidgetForwarder(mainActivity);
        this.wallpaperForwarder = new WallpaperForwarder(mainActivity);
        this.uiTweaksForwarder = new UITweaksForwarder(mainActivity);
        this.uxTweaksForwarder = new UXTweaksForwarder(mainActivity);
        this.favoriteForwarder = new FavoriteForwarder(mainActivity);
    }

    public void onCreate() {
        favoriteForwarder.onCreate();
        widgetForwarder.onCreate();
        uiTweaksForwarder.onCreate();
        uxTweaksForwarder.onCreate();
    }

    public void onResume() {
        uiTweaksForwarder.onResume();
        uxTweaksForwarder.onResume();
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
        uxTweaksForwarder.onTouch(view, event); // always return false anyway
        return wallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        uxTweaksForwarder.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
    }

    public void updateRecords(String query) {
        favoriteForwarder.updateRecords(query);
        uxTweaksForwarder.updateRecords(query);
    }

    public void onAllProvidersLoaded() {
        favoriteForwarder.onAllProvidersLoaded();
    }

    public void onFavoriteChange() {
        favoriteForwarder.onFavoriteChange();
    }

    public void onDisplayKissBar(Boolean display) {
        uxTweaksForwarder.onDisplayKissBar(display);
    }
}
