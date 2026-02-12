package fr.neamar.kiss.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import fr.neamar.kiss.R;

public class URIUtils {

    public enum URIValidity {
        VALID(true, 0),
        NOT_AN_URI(false, R.string.search_provider_error_url),
        NO_APP_CAN_HANDLE_URI(false, R.string.search_provider_error_uri_cannot_be_handle),
        NO_PLACEHOLDER(false, R.string.search_provider_error_placeholder),
        INVALID_PIPE_CHAR(false, R.string.search_provider_error_char),
        INVALID_NAME_EXISTS(false, R.string.search_provider_error_exists);

        private final boolean isValid;
        private final int errorMessageResId;

        URIValidity(final boolean isValid, @StringRes int errorMessageResId) {
            this.isValid = isValid;
            this.errorMessageResId = errorMessageResId;
        }

        public boolean isValid() {
            return isValid;
        }

        @Nullable
        public String getErrorMessage(@NonNull Context context) {
            return isValid ? null : context.getString(errorMessageResId);
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
            if (uri.isAbsolute() && uri.getSchemeSpecificPart().length() >= 2) {
                Intent intent = PackageManagerUtils.createUriIntent(uri);
                final PackageManager packageManager = context.getPackageManager();
                final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return !receiverList.isEmpty() ? URIValidity.VALID : URIValidity.NO_APP_CAN_HANDLE_URI;
            }
        }
        return URIValidity.NOT_AN_URI;
    }
}
