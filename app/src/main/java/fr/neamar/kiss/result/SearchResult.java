package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SearchResult extends Result<SearchPojo> {

    private static final String TAG = SearchResult.class.getSimpleName();

    SearchResult(@NonNull SearchPojo pojo) {
        super(pojo);
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_search, parent);

        TextView searchText = view.findViewById(R.id.item_search_text);
        ImageView image = view.findViewById(R.id.item_search_icon);
        boolean hasCustomIcon = false;
        String text;
        int pos;
        int len;

        boolean hideIcons = isHideIcons(context);
        if (hideIcons) {
            image.setImageDrawable(null);
        }

        switch (pojo.type) {
            case URL_QUERY:
                text = String.format(context.getString(R.string.ui_item_visit), this.pojo.getName());
                pos = text.indexOf(this.pojo.getName());
                len = this.pojo.getName().length();
                if (!hideIcons) {
                    image.setImageResource(R.drawable.ic_public);
                }
                break;
            case SEARCH_QUERY:
                text = String.format(context.getString(R.string.ui_item_search), this.pojo.getName(), pojo.query);
                pos = text.indexOf(pojo.query);
                len = pojo.query.length();
                if (!hideIcons) {
                    image.setImageResource(R.drawable.search);
                }

                if (isGoogleSearch() && !hideIcons) {
                    Drawable icon = getIconByPackageName(context, "com.google.android.googlequicksearchbox");
                    if (icon != null) {
                        image.setImageDrawable(icon);
                        hasCustomIcon = true;
                    }
                }
                if (isDuckDuckGo() && !hideIcons) {
                    Drawable icon = getIconByPackageName(context, "com.duckduckgo.mobile.android");
                    if (icon != null) {
                        image.setImageDrawable(icon);
                        hasCustomIcon = true;
                    }
                }

                if (!hasCustomIcon && !hideIcons) {
                    Intent intent = createSearchQueryIntent();
                    Drawable icon = getIconByIntent(context, intent);
                    if (icon != null) {
                        image.setImageDrawable(icon);
                        hasCustomIcon = true;
                    }
                }
                break;
            case CALCULATOR_QUERY:
                text = pojo.query;
                pos = text.indexOf("=");
                len = text.length() - pos;
                if (!hideIcons) {
                    image.setImageResource(R.drawable.ic_functions);
                }
                break;
            case URI_QUERY:
                text = String.format(context.getString(R.string.ui_item_open), this.pojo.query);
                pos = text.indexOf(pojo.query);
                len = pojo.query.length();
                if (!hideIcons) {
                    image.setImageResource(R.drawable.ic_public);
                    Intent intent = createSearchQueryIntent();
                    Drawable icon = getIconByIntent(context, intent);
                    if (icon != null) {
                        image.setImageDrawable(icon);
                        hasCustomIcon = true;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Following type isn't supported: " + pojo.type);
        }

        displayHighlighted(text, Collections.singletonList(new Pair<>(pos, pos + len)), searchText, context);

        if (!hasCustomIcon) {
            image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        } else {
            image.setColorFilter(null);
        }
        return view;
    }

    /**
     * Creates intent to start activity for given uri query.
     *
     * @return intent
     */
    private Intent createUriQueryIntent() {
        Uri uri = Uri.parse(pojo.query);
        return PackageManagerUtils.createUriIntent(uri);
    }

    /**
     * Creates intent to start activity for given search query.
     *
     * @return intent
     */
    private Intent createSearchQueryIntent() {
        String query;
        try {
            query = URLEncoder.encode(pojo.query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            query = URLEncoder.encode(pojo.query);
        }
        String urlWithQuery = pojo.url.replaceAll("%s|\\{q\\}", query);
        Uri uri = Uri.parse(urlWithQuery);
        return PackageManagerUtils.createUriIntent(uri);
    }

    /**
     * @param context
     * @param intent
     * @return icon, of best matching app for given intent
     */
    private Drawable getIconByIntent(Context context, Intent intent) {
        ComponentName componentName = PackageManagerUtils.getComponentName(context, intent);
        if (componentName != null) {
            UserHandle userHandle = new UserHandle();
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName, userHandle), userHandle);
        }
        return null;
    }

    /**
     * @param context
     * @param packageName
     * @return icon, of best matching app for given package name
     */
    private Drawable getIconByPackageName(Context context, String packageName) {
        UserHandle userHandle = new UserHandle();
        ComponentName componentName = PackageManagerUtils.getLaunchingComponent(context, packageName, userHandle);
        if (componentName != null) {
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName, userHandle), userHandle);
        }
        return null;
    }

    @Override
    public void doLaunch(Context context, View v) {
        switch (pojo.type) {
            case URL_QUERY:
            case SEARCH_QUERY:
                if (isGoogleSearch()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(SearchManager.QUERY, pojo.query); // query contains search string
                        setSourceBounds(intent, v);
                        context.startActivity(intent);
                        break;
                    } catch (ActivityNotFoundException e) {
                        // Google app not found, fall back to default method
                    }
                }
                Intent search = createSearchQueryIntent();
                setSourceBounds(search, v);
                try {
                    context.startActivity(search);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to run search for url: " + pojo.url);
                }
                break;
            case CALCULATOR_QUERY:
                ClipboardUtils.setClipboard(context, pojo.query.substring(pojo.query.indexOf("=") + 2));
                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
                break;
            case URI_QUERY:
                Intent intent = createUriQueryIntent();
                setSourceBounds(intent, v);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to run search for uri: " + pojo.url);
                }
                break;
        }
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.share));

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, pojo.query);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private boolean isGoogleSearch() {
        return pojo.url.startsWith("https://encrypted.google.com");
    }

    private boolean isDuckDuckGo() {
        return pojo.url.startsWith("https://start.duckduckgo.com");
    }
}
