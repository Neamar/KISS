package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.FuzzyScore;

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
            if(isGoogleSearch()) {
                try {
                    Drawable icon = context.getPackageManager().getApplicationIcon("com.google.android.googlequicksearchbox");
                    image.setImageDrawable(icon);
                    hasCustomIcon = true;
                } catch (PackageManager.NameNotFoundException e) {
                    // Keep default
                }
            }
            if(isDuckDuckGo()) {
                try {
                    Drawable icon = context.getPackageManager().getApplicationIcon("com.duckduckgo.mobile.android");
                    image.setImageDrawable(icon);
                    hasCustomIcon = true;
                } catch (PackageManager.NameNotFoundException e) {
                    // Keep default
                }

            }
        } else if (searchPojo.type == SearchPojo.CALCULATOR_QUERY) {
            text = searchPojo.query;
            pos = text.indexOf("=");
            len = text.length() - pos;
            image.setImageResource(R.drawable.ic_functions);
        } else {
            throw new IllegalArgumentException();
        }

        displayHighlighted(text, Collections.singletonList(new Pair<>(pos, pos + len)), searchText, context);

        if(!hasCustomIcon) {
            image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        }
        return view;
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
