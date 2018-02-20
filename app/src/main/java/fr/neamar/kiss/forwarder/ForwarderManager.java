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

    public ForwarderManager(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);

        this.widgetForwarder = new WidgetForwarder(mainActivity, prefs);
        this.wallpaperForwarder = new WallpaperForwarder(mainActivity, prefs);
        this.uiTweaksForwarder = new UITweaksForwarder(mainActivity, prefs);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        widgetForwarder.onCreate();
        wallpaperForwarder.onCreate();
        uiTweaksForwarder.onCreate();
    }

    @Override
    public void onResume() {
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
        return wallpaperForwarder.onTouch(view, event);
    }

    @Override
    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
    }
}
