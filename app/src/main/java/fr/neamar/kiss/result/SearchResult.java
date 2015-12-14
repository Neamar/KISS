package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchResult extends Result {
    private final SearchPojo searchPojo;

    public SearchResult(SearchPojo searchPojo) {
        super();
        this.pojo = this.searchPojo = searchPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_search);

        TextView appName = (TextView) v.findViewById(R.id.item_search_text);
        String text = context.getString(R.string.ui_item_search);
        appName.setText(enrichText(String.format(text, "{" + searchPojo.query + "}")));

        ((ImageView) v.findViewById(R.id.item_search_icon)).setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void doLaunch(Context context, View v) {
        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
        search.putExtra(SearchManager.QUERY, searchPojo.query);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // In the latest Google Now version, ACTION_WEB_SEARCH is broken when used with FLAG_ACTIVITY_NEW_TASK.
            // Adding FLAG_ACTIVITY_CLEAR_TASK seems to fix the problem.
            search.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(search);
        } catch (ActivityNotFoundException e) {
            // This exception gets thrown if Google Search has been deactivated:
            Uri uri = Uri.parse("https://encrypted.google.com/search?q=" + searchPojo.query);
            search = new Intent(Intent.ACTION_VIEW, uri);
            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(search);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {

        //empty menu so that you don't add on favorites
        PopupMenu menu = new PopupMenu(context, parentView);
        return menu;
    }
}
