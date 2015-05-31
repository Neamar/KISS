package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class LoadAliasPojos extends LoadPojos<Pair<String, String>> {

    public LoadAliasPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<Pair<String, String>> doInBackground(Void... params) {
        final PackageManager pm = context.getPackageManager();
        ArrayList<Pair<String, String>> alias = new ArrayList<>();
        String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
        alias.add(new Pair<>("contacts", contactApp));

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        alias.add(new Pair<>("dial", phoneApp));
        alias.add(new Pair<>("compose", phoneApp));

        String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
        alias.add(new Pair<>("internet", browserApp));
        alias.add(new Pair<>("web", browserApp));

        String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
        alias.add(new Pair<>("email", mailApp));
        alias.add(new Pair<>("mail", mailApp));

        String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
        alias.add(new Pair<>("market", marketApp));

        String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
        alias.add(new Pair<>("text", messagingApp));
        alias.add(new Pair<>("sms", messagingApp));
        return alias;

    }

    private String getApp(PackageManager pm, String action) {
        Intent lookingFor = new Intent(action, null);
        return getApp(pm, lookingFor);
    }

    private String getAppByCategory(PackageManager pm, String category) {
        Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
        lookingFor.addCategory(category);
        return getApp(pm, lookingFor);
    }

    private String getApp(PackageManager pm, Intent lookingFor) {
        List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
        if (list.size() == 0)
            return "(none)";
        else
            return "app://" + list.get(0).activityInfo.applicationInfo.packageName + "/"
                    + list.get(0).activityInfo.name;

    }
}
