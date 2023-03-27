package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
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
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SearchResult extends Result {
    private final SearchPojo searchPojo;

    SearchResult(SearchPojo searchPojo) {
        super(searchPojo);
        this.searchPojo = searchPojo;
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

        if (searchPojo.type == SearchPojo.URL_QUERY) {
            text = String.format(context.getString(R.string.ui_item_visit), this.pojo.getName());
            pos = text.indexOf(this.pojo.getName());
            len = this.pojo.getName().length();
            image.setImageResource(R.drawable.ic_public);
        } else if (searchPojo.type == SearchPojo.SEARCH_QUERY) {
            text = String.format(context.getString(R.string.ui_item_search), this.pojo.getName(), searchPojo.query);
            pos = text.indexOf(searchPojo.query);
            len = searchPojo.query.length();
            image.setImageResource(R.drawable.search);

            boolean hideIcons = getHideIcons(context);
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
        } else if (searchPojo.type == SearchPojo.CALCULATOR_QUERY) {
            text = searchPojo.query;
            pos = text.indexOf("=");
            len = text.length() - pos;
            image.setImageResource(R.drawable.ic_functions);
        } else if (searchPojo.type == SearchPojo.URI_QUERY) {
            text = String.format(context.getString(R.string.ui_item_open), this.searchPojo.query);
            pos = text.indexOf(searchPojo.query);
            len = searchPojo.query.length();
            image.setImageResource(R.drawable.ic_public);

            if (!getHideIcons(context)) {
                Intent intent = createUriIntent();
                Drawable icon = getIconByIntent(context, intent);
                if (icon != null) {
                    image.setImageDrawable(icon);
                    hasCustomIcon = true;
                }
            }
        } else {
            throw new IllegalArgumentException("Wrong type!");
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
     * Creates intent to start activity with given uri.
     *
     * @return intent
     */
    private Intent createUriIntent() {
        Uri uri = Uri.parse(searchPojo.query);
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
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName), new UserHandle());
        }
        return null;
    }

    /**
     * @param context
     * @param packageName
     * @return icon, of best matching app for given package name
     */
    private Drawable getIconByPackageName(Context context, String packageName) {
        ComponentName componentName = PackageManagerUtils.getLaunchingComponent(context, packageName);
        if (componentName != null) {
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName), new UserHandle());
        }
        return null;
    }

    private boolean getHideIcons(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("icons-hide", false);
    }

    @Override
    public void doLaunch(Context context, View v) {
        switch (searchPojo.type) {
            case SearchPojo.URL_QUERY:
            case SearchPojo.SEARCH_QUERY:
                if (isGoogleSearch()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(SearchManager.QUERY, searchPojo.query); // query contains search string
                        context.startActivity(intent);
                        break;
                    } catch (ActivityNotFoundException e) {
                        // Google app not found, fall back to default method
                    }
                }
                String query;
                try {
                    query = URLEncoder.encode(searchPojo.query, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    query = URLEncoder.encode(searchPojo.query);
                }
                String urlWithQuery = searchPojo.url.replaceAll("%s|\\{q\\}", query);
                Uri uri = Uri.parse(urlWithQuery);
                Intent search = new Intent(Intent.ACTION_VIEW, uri);
                search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(search);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.w("SearchResult", "Unable to run search for url: " + searchPojo.url);
                }
                break;
            case SearchPojo.CALCULATOR_QUERY:
                ClipboardUtils.setClipboard(context, searchPojo.query.substring(searchPojo.query.indexOf("=") + 2));
                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
                break;
            case SearchPojo.URI_QUERY:
                Intent intent = createUriIntent();
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.w("SearchResult", "Unable to run search for uri: " + searchPojo.url);
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
        switch (stringId) {
            case R.string.share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, searchPojo.query);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareIntent);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private boolean isGoogleSearch() {
        return searchPojo.url.startsWith("https://encrypted.google.com");
    }

    private boolean isDuckDuckGo() {
        return searchPojo.url.startsWith("https://start.duckduckgo.com");
    }
}
