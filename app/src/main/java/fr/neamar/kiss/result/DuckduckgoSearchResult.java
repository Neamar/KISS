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
import fr.neamar.kiss.pojo.DuckduckgoSearchPojo;

public class DuckduckgoSearchResult extends Result {
    private final DuckduckgoSearchPojo searchPojo;

    public DuckduckgoSearchResult(DuckduckgoSearchPojo searchPojo) {
        super();
        this.pojo = this.searchPojo = searchPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_search);

        TextView appName = (TextView) v.findViewById(R.id.item_search_text);
        appName.setText(enrichText("DuckDuckGo \"{" + searchPojo.query + "}\""));

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void doLaunch(Context context, View v) {
        // TODO: use DDG app if available
        // TODO: programatically grab !bang commands and parse them for names?
        // TODO: add option (in the settings) that uses apps for some bangs if installed?
        Uri uri = Uri.parse("https://duckduckgo.com/?q=" + searchPojo.query);
        Intent search = new Intent(Intent.ACTION_VIEW, uri);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(search);
    }

}
