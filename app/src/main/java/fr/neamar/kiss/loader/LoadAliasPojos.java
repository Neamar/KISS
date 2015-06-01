package fr.neamar.kiss.loader;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.pojo.AliasPojo;

public class LoadAliasPojos extends LoadPojos<AliasPojo> {

    public LoadAliasPojos(Context context) {
        super(context, "none://");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    protected ArrayList<AliasPojo> doInBackground(Void... params) {
        final PackageManager pm = context.getPackageManager();
        ArrayList<AliasPojo> alias = new ArrayList<>();

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        if (phoneApp != null) {
            alias.add(makeAliasPojo("dial", phoneApp));
            alias.add(makeAliasPojo("compose", phoneApp));
            alias.add(makeAliasPojo("phone", phoneApp));
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
            if (contactApp != null) {
                alias.add(makeAliasPojo("contacts", contactApp));
                alias.add(makeAliasPojo("people", contactApp));
            }

            String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
            if (browserApp != null) {
                alias.add(makeAliasPojo("internet", browserApp));
                alias.add(makeAliasPojo("web", browserApp));
                alias.add(makeAliasPojo("browser", browserApp));
            }

            String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
            if (mailApp != null) {
                alias.add(makeAliasPojo("email", mailApp));
                alias.add(makeAliasPojo("mail", mailApp));
            }

            String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
            if (marketApp != null) {
                alias.add(makeAliasPojo("market", marketApp));
                alias.add(makeAliasPojo("store", marketApp));
            }

            String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
            if (messagingApp != null) {
                alias.add(makeAliasPojo("text", messagingApp));
                alias.add(makeAliasPojo("sms", messagingApp));
                alias.add(makeAliasPojo("messaging", messagingApp));
            }
        }

        return alias;

    }

    private AliasPojo makeAliasPojo(String alias, String appInfos) {
        AliasPojo aliasPojo = new AliasPojo();
        aliasPojo.alias = alias;
        aliasPojo.app = appInfos;

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
        if (list.size() == 0) {
            return null;
        } else {
            return "app://" + list.get(0).activityInfo.applicationInfo.packageName + "/"
                    + list.get(0).activityInfo.name;
        }

    }
}
