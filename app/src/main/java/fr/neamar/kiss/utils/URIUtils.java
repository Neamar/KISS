package fr.neamar.kiss.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.URLUtil;

import java.util.List;

public class URIUtils {
    /**
     * Check for valid uri by searching for any app that can handle it.
     *
     * @param query uri submitted
     * @return true, if there is any app that can handle the uri
     */
    public static boolean isValidUri(final String query, final Context context) {
        if (!URLUtil.isValidUrl(query)) {
            Uri uri = Uri.parse(query);
            Intent intent = PackageManagerUtils.createUriIntent(uri);
            if (intent != null) {
                final PackageManager packageManager = context.getPackageManager();
                final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return receiverList.size() > 0;
            }
        }
        return false;
    }
}
