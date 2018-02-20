package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import fr.neamar.kiss.MainActivity;

public class ForwarderManager extends Forwarder {
    private final WidgetForwarder widgetForwarder;

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetForwarder = new WidgetForwarder(mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        widgetForwarder.onCreate();
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

    @Override
    public void onDataSetChanged() {
        widgetForwarder.onDataSetChanged();
    }
}
