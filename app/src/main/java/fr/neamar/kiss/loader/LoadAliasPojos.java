package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.pojo.AliasPojo;

public class LoadAliasPojos extends LoadPojos<AliasPojo> {

    public LoadAliasPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<AliasPojo> doInBackground(Void... params) {
        final PackageManager pm = context.getPackageManager();
        ArrayList<AliasPojo> alias = new ArrayList<>();
        String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
        alias.add(makeAliasPojo("contacts", contactApp));

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        alias.add(makeAliasPojo("dial", phoneApp));
        alias.add(makeAliasPojo("compose", phoneApp));

        String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
        alias.add(makeAliasPojo("internet", browserApp));
        alias.add(makeAliasPojo("web", browserApp));

        String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
        alias.add(makeAliasPojo("email", mailApp));
        alias.add(makeAliasPojo("mail", mailApp));

        String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
        alias.add(makeAliasPojo("market", marketApp));

        String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
        alias.add(makeAliasPojo("text", messagingApp));
        alias.add(makeAliasPojo("sms", messagingApp));
        return alias;

    }

    public AliasPojo makeAliasPojo(String alias, String app) {
        AliasPojo aliasPojo = new AliasPojo();
        aliasPojo.alias = alias;
        aliasPojo.app = app;

        return aliasPojo;
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
