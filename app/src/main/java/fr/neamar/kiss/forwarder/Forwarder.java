package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import fr.neamar.kiss.MainActivity;

class Forwarder {
    MainActivity mainActivity;
    protected SharedPreferences prefs;

    Forwarder(MainActivity mainActivity, SharedPreferences prefs) {
        this.mainActivity = mainActivity;
        this.prefs = prefs;
    }

    public void onCreate() {
    }

    public void onResume() {
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

    public void onDataSetChanged() {
    }
}