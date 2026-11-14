
package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.UserManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    private static final String TAG = LoadShortcutsPojos.class.getSimpleName();

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected List<ShortcutPojo> doInBackground(Void... params) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }

        List<ShortcutPojo> nonOreoPojos = fetchNonOreoPojos(context);
        List<ShortcutPojo> oreoPojos = fetchOreoPojos(context);

        List<ShortcutPojo> allPojos = new ArrayList<>(nonOreoPojos);
        allPojos.addAll(oreoPojos);

        return allPojos;
    }

    // get all oreo shortcuts from system directly
    private List<ShortcutPojo> fetchOreoPojos(Context context) {
        List<ShortcutPojo> oreoPojos = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
            Set<String> excludedApps = dataHandler.getExcluded();
            Set<String> excludedShortcutApps = dataHandler.getExcludedShortcutApps();
            UserManager userManager = ContextCompat.getSystemService(context, UserManager.class);
            List<ShortcutInfo> shortcutInfos;
            try {
                shortcutInfos = ShortcutUtil.getAllShortcuts(context);
            } catch (SecurityException e) {
                // shortcut host permission lost (e.g. KISS no longer default launcher)
                Log.w(TAG, "Unable to get all shortcuts", e);
                return oreoPojos;
            }

            // Many shortcuts share the same profile and the same app: only ask the system once for each
            Map<android.os.UserHandle, Boolean> hiddenProfiles = new HashMap<>();
            Map<android.os.UserHandle, Boolean> quietProfiles = new HashMap<>();
            Map<String, ApplicationInfo> appInfos = new HashMap<>();
            Map<String, String> appNames = new HashMap<>();

            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                if (isCancelled()) {
                    break;
                }

                android.os.UserHandle profile = shortcutInfo.getUserHandle();
                if (ShortcutUtil.isShortcutVisibleInProfile(context, shortcutInfo, excludedApps, excludedShortcutApps)
                        && !hiddenProfiles.computeIfAbsent(profile, p -> ShortcutUtil.isProfileHidden(context, p))) {
                    UserHandle user = new UserHandle(context, profile);
                    String appKey = user.addUserSuffixToString(shortcutInfo.getPackage(), '#');
                    ApplicationInfo appInfo = appInfos.computeIfAbsent(appKey, k -> PackageManagerUtils.getApplicationInfo(context, shortcutInfo.getPackage(), user));
                    // Reading a label loads the app's resources, only do it when the name is displayed
                    String appName = null;
                    if (!shortcutInfo.isPinned() && appInfo != null) {
                        appName = appNames.computeIfAbsent(appKey, k -> context.getPackageManager().getApplicationLabel(appInfo).toString());
                    }
                    ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(context, shortcutInfo, appName);

                    if (shortcutRecord != null) {
                        boolean isSuspended = appInfo != null && PackageManagerUtils.isAppSuspended(appInfo);
                        boolean isQuietModeEnabled = quietProfiles.computeIfAbsent(profile, userManager::isQuietModeEnabled);
                        boolean disabled = isSuspended || isQuietModeEnabled;

                        ShortcutPojo pojo = createPojo(
                                user,
                                shortcutRecord,
                                dataHandler.getTagsHandler(),
                                ShortcutUtil.getComponentName(context, shortcutInfo),
                                shortcutInfo.isPinned(),
                                shortcutInfo.isDynamic(),
                                disabled
                        );

                        oreoPojos.add(pojo);
                    }
                }
            }
        }

        return oreoPojos;
    }

    private List<ShortcutPojo> fetchNonOreoPojos(Context context) {
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        TagsHandler tagsHandler = dataHandler.getTagsHandler();
        List<ShortcutPojo> pojos = new ArrayList<>();
        List<ShortcutRecord> records = DBHelper.getShortcuts(context);

        for (ShortcutRecord shortcutRecord : records) {
            if (isCancelled()) {
                break;
            }
            ShortcutPojo pojo = createPojo(null, shortcutRecord, tagsHandler, null, true, false, false);
            if (!pojo.isOreoShortcut()) {
                // add older shortcuts from DB
                pojos.add(pojo);
            }
        }

        return pojos;
    }

    private ShortcutPojo createPojo(UserHandle userHandle, ShortcutRecord shortcutRecord, TagsHandler tagsHandler, String componentName, boolean pinned, boolean dynamic, boolean disabled) {
        ShortcutPojo pojo = new ShortcutPojo(userHandle, shortcutRecord, componentName, pinned, dynamic, disabled);
        pojo.setName(shortcutRecord.name);
        pojo.setTags(tagsHandler.getTags(pojo.id));
        return pojo;
    }
}
