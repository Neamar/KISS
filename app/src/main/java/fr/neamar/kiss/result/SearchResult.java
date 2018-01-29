package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.ui.ListPopup;

import static fr.neamar.kiss.R.drawable.search;

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
        ImageView image = (ImageView) v.findViewById(R.id.item_search_icon);
        if (searchPojo.direct) {
            String text = context.getString(R.string.ui_item_visit);
            appName.setText(enrichText(String.format(text, "{" + this.pojo.getName() + "}"), context));
            image.setImageResource(R.drawable.ic_public);
        } else {
            String text = context.getString(R.string.ui_item_search);
            appName.setText(enrichText(String.format(text, this.pojo.getName(), "{" + searchPojo.query + "}"), context));
            image.setImageResource(search);
        }
        image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        String urlWithQuery = searchPojo.url.replace("{q}", searchPojo.query);
            Uri uri = Uri.parse(urlWithQuery);
            Intent search = new Intent(Intent.ACTION_VIEW, uri);
            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(search);
            }
            catch (android.content.ActivityNotFoundException e) {
                Log.w("SearchResult", "Unable to run search for url: " + searchPojo.url);
            }
    }

    @Override
    protected ListPopup buildPopupMenu( Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView ) {
        adapter.add( new ListPopup.Item( context, R.string.share ) );

        return inflatePopupMenu(adapter, context );
    }

    @Override
    protected Boolean popupMenuClickHandler( Context context, RecordAdapter parent, int stringId ) {
        switch ( stringId ) {
            case R.string.share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, searchPojo.query);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareIntent);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId );
    }
}
