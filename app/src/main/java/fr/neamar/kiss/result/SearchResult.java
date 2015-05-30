package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchResult extends Result {
	public SearchPojo searchHolder;

	public SearchResult(SearchPojo searchHolder) {
		super();
		this.pojo = this.searchHolder = searchHolder;
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_search);

		TextView appName = (TextView) v.findViewById(R.id.item_search_text);
		appName.setText(enrichText(context.getString(R.string.ui_item_search) + " \"{"
				+ searchHolder.query + "}\""));

		return v;
	}

	@Override
	public void doLaunch(Context context, View v) {
		Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
		search.putExtra(SearchManager.QUERY, searchHolder.query);
        // In the latest Google Now version, ACTION_WEB_SEARCH is broken when used with FLAG_ACTIVITY_NEW_TASK.
        // Adding FLAG_ACTIVITY_CLEAR_TASK seems to fix the problem.
        search.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		try {
			context.startActivity(search);
		} catch (ActivityNotFoundException e) {
			// This exception gets thrown if Google Search has been deactivated:
			Uri uri = Uri.parse("http://www.google.com/#q=" + searchHolder.query);
			search = new Intent(Intent.ACTION_VIEW, uri);
			search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(search);
		}
	}

}
