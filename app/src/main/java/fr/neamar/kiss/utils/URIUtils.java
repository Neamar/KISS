package fr.neamar.kiss.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.URLUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class URIUtils {

    public enum URIValidity {
        VALID(true),
        NOT_AN_URI(false),
        NO_APP_CAN_HANDLE_URI(false)
        ;

        public final boolean isValid;

        URIValidity(final boolean isValid) {
            this.isValid = isValid;
        }
    }

    /**
     * Check for valid uri by searching for any app that can handle it.
     *
     * @param query uri submitted
     * @return URIValidity
     */
    @NotNull
    public static URIValidity isValidUri(final @NotNull String query, final @NotNull Context context) {
        if (!URLUtil.isValidUrl(query)) {
            Uri uri = Uri.parse(query);
            if (uri.isAbsolute() && uri.getSchemeSpecificPart().length() > 2) {
                Intent intent = PackageManagerUtils.createUriIntent(uri);
                final PackageManager packageManager = context.getPackageManager();
                final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return receiverList.size() > 0 ? URIValidity.VALID : URIValidity.NO_APP_CAN_HANDLE_URI;
            }
        }
        return URIValidity.NOT_AN_URI;
    }
}
