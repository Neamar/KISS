
package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherUserInfo;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected List<ShortcutPojo> doInBackground(Void... params) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        List<ShortcutRecord> records = DBHelper.getShortcuts(context);
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        TagsHandler tagsHandler = dataHandler.getTagsHandler();
        Set<String> excludedApps = dataHandler.getExcluded();
        Set<String> excludedShortcutApps = dataHandler.getExcludedShortcutApps();

        List<ShortcutPojo> pojos = new ArrayList<>();

        for (ShortcutRecord shortcutRecord : records) {
            ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, null, true, false, false);
            if (!pojo.isOreoShortcut()) {
                // add older shortcuts from DB
                pojos.add(pojo);
            }
        }

        // get all oreo shortcuts from system directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getAllShortcuts(context);
            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                LauncherUserInfo info = null;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    info = launcherApps.getLauncherUserInfo(shortcutInfo.getUserHandle());
                }
                if (isCancelled()) {
                    break;
                }
                if (ShortcutUtil.isShortcutVisible(context, shortcutInfo, excludedApps, excludedShortcutApps)) {
                    ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(context, shortcutInfo, !shortcutInfo.isPinned());
                    if (shortcutRecord != null) {
                        boolean disabled = PackageManagerUtils.isAppSuspended(context, shortcutInfo.getPackage(), new UserHandle(context, shortcutInfo.getUserHandle())) || userManager.isQuietModeEnabled(shortcutInfo.getUserHandle());
                        ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, ShortcutUtil.getComponentName(context, shortcutInfo), shortcutInfo.isPinned(), shortcutInfo.isDynamic(), disabled);
                        if ((android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                            && (info != null)) {
                            boolean privateSpaceShortcutsDisabled = prefs.getBoolean("disable-private-space-shortcuts", true);
                            if (shouldAddShortcut(userManager, info, shortcutInfo.getUserHandle(),
                                    privateSpaceShortcutsDisabled)) {
                                pojos.add(pojo);
                            }
                        } else {
                            pojos.add(pojo);
                        }
                    }
                }
            }
        }

        return pojos;
    }

    @RequiresApi(35)
    private boolean shouldAddShortcut(UserManager manager, LauncherUserInfo info,
                                      android.os.UserHandle profile, boolean privateSpaceShortcutsDisabled) {
        if (!info.getUserType().equalsIgnoreCase(UserManager.USER_TYPE_PROFILE_PRIVATE)) {
            return true;
        } else {
            if (privateSpaceShortcutsDisabled) {
                return false;
            }
            else return !isQuietModeEnabled(manager, profile);
        }
    }

    private ShortcutPojo createPojo(ShortcutRecord shortcutRecord, TagsHandler tagsHandler, String componentName, boolean pinned, boolean dynamic, boolean disabled) {
        ShortcutPojo pojo = new ShortcutPojo(shortcutRecord, componentName, pinned, dynamic, disabled);
        pojo.setName(shortcutRecord.name);
        pojo.setTags(tagsHandler.getTags(pojo.id));
        return pojo;
    }

    private boolean isQuietModeEnabled(UserManager manager, android.os.UserHandle profile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return manager.isQuietModeEnabled(profile);
        }
        return false;
    }
}
