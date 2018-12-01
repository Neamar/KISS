package fr.neamar.kiss.forwarder;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.AppPopupAdapter;
import fr.neamar.kiss.result.AppPopupResult;
import fr.neamar.kiss.result.ShortcutPopupResult;

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
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mainActivity);
        final AlertDialog dialog;
        dialogBuilder.setTitle(mainActivity.getString(R.string.menu_shortcut_add));
        View customView = LayoutInflater.from(mainActivity).inflate(
                R.layout.popup_app_list, null, false);
        ListView listView = customView.findViewById(R.id.listView);

        final ArrayList<AppPopupResult> availableShortcuts = new ArrayList<>();
        PackageManager packageManager = mainActivity.getPackageManager();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = packageManager.queryIntentActivities(shortcutsIntent, 0);
        for (ResolveInfo shortcut : shortcuts) {
            ShortcutPopupResult r = new ShortcutPopupResult(shortcut.activityInfo.packageName, shortcut.loadLabel(packageManager).toString(), shortcut);
            availableShortcuts.add(r);
        }

        AppPopupAdapter mAdapter = new AppPopupAdapter(availableShortcuts);
        listView.setAdapter(mAdapter);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dialogBuilder.setView(customView);
        dialog = dialogBuilder.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                ResolveInfo r = ((ShortcutPopupResult) availableShortcuts.get(position)).shortcut;
                ActivityInfo activity = r.activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                        activity.name);
                Intent i = new Intent(Intent.ACTION_MAIN);

                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                i.setComponent(name);

                mainActivity.startActivity(i);
            }
        });
    }
}
