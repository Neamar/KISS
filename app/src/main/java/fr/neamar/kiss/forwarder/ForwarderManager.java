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

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetForwarder = new WidgetForwarder(mainActivity);
        this.wallpaperForwarder = new WallpaperForwarder(mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        widgetForwarder.onCreate();
        wallpaperForwarder.onCreate();
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
