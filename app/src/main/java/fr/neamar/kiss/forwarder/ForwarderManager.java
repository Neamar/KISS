package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.SharedPreferences;
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

    public ForwarderManager(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);

        this.widgetForwarder = new WidgetForwarder(mainActivity, prefs);
        this.wallpaperForwarder = new WallpaperForwarder(mainActivity, prefs);
        this.uiTweaksForwarder = new UITweaksForwarder(mainActivity, prefs);
        this.uxTweaksForwarder = new UXTweaksForwarder(mainActivity, prefs);
        this.favoriteForwarder = new FavoriteForwarder(mainActivity, prefs);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        favoriteForwarder.onCreate();
        widgetForwarder.onCreate();
        uiTweaksForwarder.onCreate();
        uxTweaksForwarder.onCreate();
    }

    @Override
    public void onResume() {
        favoriteForwarder.onResume();
        uiTweaksForwarder.onResume();
    }

    @Override
    public void onStart() {
        widgetForwarder.onStart();
    }

    @Override
    public void onStop() {
        widgetForwarder.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        widgetForwarder.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetForwarder.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        widgetForwarder.onCreateContextMenu(menu, v, menuInfo);
    }

    public boolean onTouch(View view, MotionEvent event) {
        uxTweaksForwarder.onTouch(view, event); // always return false anyway
        return wallpaperForwarder.onTouch(view, event);
    }

    @Override
    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
    }

    @Override
    public void updateRecords(String query) {
        favoriteForwarder.updateRecords(query);
        uxTweaksForwarder.updateRecords(query);
    }

    @Override
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
