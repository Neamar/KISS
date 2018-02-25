package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;

abstract class Forwarder {
    final MainActivity mainActivity;
    final SharedPreferences prefs;

    Forwarder(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.prefs = mainActivity.prefs;
    }

    void onCreate() {
    }

    void onResume() {
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    }

    public boolean onTouch(View view, MotionEvent event) {
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
    }

    public void onDataSetChanged() {
    }

    public void updateRecords(String query) {
    }

    public void onAllProvidersLoaded() {
    }

    public void onFavoriteChange() {
    }

    public void onDisplayKissBar(Boolean display) {
    }
}