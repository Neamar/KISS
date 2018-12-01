package fr.neamar.kiss.forwarder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.AppPopupAdapter;
import fr.neamar.kiss.result.AppPopupResult;

class LegacyShortcut extends Forwarder {
    LegacyShortcut(MainActivity mainActivity) {
        super(mainActivity);
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.shortcut) {
            displayShortcutPopup();

            return true;
        }

        return false;
    }

    private void displayShortcutPopup() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mainActivity);
        // dialog.setContentView(R.layout.alert_list_radio);
        dialog.setTitle(mainActivity.getString(R.string.menu_shortcut_add));
        View customView = LayoutInflater.from(mainActivity).inflate(
                R.layout.popup_app_list, null, false);
        ListView listView = customView.findViewById(R.id.listView);

        // ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
        // R.layout.single_item_layout , R.id.singleItem, dummies);
        ArrayList<AppPopupResult> availableShortcuts = new ArrayList<>();
        PackageManager packageManager = mainActivity.getPackageManager();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = packageManager.queryIntentActivities(shortcutsIntent, 0);
        for (ResolveInfo shortcut : shortcuts) {
            AppPopupResult r = new AppPopupResult(shortcut.activityInfo.packageName, shortcut.loadLabel(packageManager).toString());
            availableShortcuts.add(r);
        }

        AppPopupAdapter mAdapter = new AppPopupAdapter(availableShortcuts);
        listView.setAdapter(mAdapter);
        // listView.setOnItemClickListener(mOnItemClick);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dialog.setView(customView);
        dialog.setPositiveButton("Add shortcut", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        dialog.show();
    }
}
