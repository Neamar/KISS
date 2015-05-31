package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.pojo.AliasPojo;
import fr.neamar.kiss.pojo.AppPojo;

public class LoadAliasPojos extends LoadPojos<AliasPojo> {

    public LoadAliasPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<AliasPojo> doInBackground(Void... params) {
        final PackageManager pm = context.getPackageManager();
        ArrayList<AliasPojo> alias = new ArrayList<>();

        AppPojo contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
        if(contactApp != null) {
            alias.add(makeAliasPojo("contacts", contactApp));
            alias.add(makeAliasPojo("people", contactApp));
        }

        AppPojo phoneApp = getApp(pm, Intent.ACTION_DIAL);
        if(phoneApp != null) {
            alias.add(makeAliasPojo("dial", phoneApp));
            alias.add(makeAliasPojo("compose", phoneApp));
            alias.add(makeAliasPojo("phone", phoneApp));
        }

        AppPojo browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
        if(browserApp != null) {
            alias.add(makeAliasPojo("internet", browserApp));
            alias.add(makeAliasPojo("web", browserApp));
            alias.add(makeAliasPojo("browser", browserApp));
        }

        AppPojo mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
        if(mailApp != null) {
            alias.add(makeAliasPojo("email", mailApp));
            alias.add(makeAliasPojo("mail", mailApp));
        }

        AppPojo marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
        if(marketApp != null) {
            alias.add(makeAliasPojo("market", marketApp));
            alias.add(makeAliasPojo("store", marketApp));
        }

        AppPojo messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
        if(messagingApp != null) {
            alias.add(makeAliasPojo("text", messagingApp));
            alias.add(makeAliasPojo("sms", messagingApp));
            alias.add(makeAliasPojo("messaging", messagingApp));
        }

        return alias;

    }

    public AliasPojo makeAliasPojo(String alias, AppPojo app) {
        AliasPojo aliasPojo = new AliasPojo();
        aliasPojo.alias = alias;
        aliasPojo.packageName = app.packageName;
        aliasPojo.activityName = app.activityName;

        return aliasPojo;
    }

    private AppPojo getApp(PackageManager pm, String action) {
        Intent lookingFor = new Intent(action, null);
        return getApp(pm, lookingFor);
    }

    private AppPojo getAppByCategory(PackageManager pm, String category) {
        Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
        lookingFor.addCategory(category);
        return getApp(pm, lookingFor);
    }

    private AppPojo getApp(PackageManager pm, Intent lookingFor) {
        List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
        if (list.size() == 0) {
            return null;
        } else {
            AppPojo appPojo = new AppPojo();
            appPojo.name = list.get(0).activityInfo.name;
            appPojo.packageName = list.get(0).activityInfo.applicationInfo.packageName;

            return appPojo;
        }

    }
}
