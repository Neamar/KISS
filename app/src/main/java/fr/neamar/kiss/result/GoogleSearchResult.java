package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.GoogleSearchPojo;

public class GoogleSearchResult extends Result {
    private final GoogleSearchPojo searchPojo;

    public GoogleSearchResult(GoogleSearchPojo searchPojo) {
        super();
        this.pojo = this.searchPojo = searchPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_search);

        TextView appName = (TextView) v.findViewById(R.id.item_search_text);
        appName.setText(enrichText("Google \"{" + searchPojo.query + "}\""));

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
            // This exception gets thrown if the Google Search app has been deactivated (so we'll just open the browser instead)
            Uri uri = Uri.parse("https://encrypted.google.com/search?q=" + searchPojo.query);
            search = new Intent(Intent.ACTION_VIEW, uri);
            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(search);
        }
    }

}
