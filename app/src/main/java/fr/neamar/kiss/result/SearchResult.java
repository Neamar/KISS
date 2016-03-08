package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchResult extends AppResult {
    private final SearchPojo searchPojo;

    private final ComponentName className;

    public SearchResult(SearchPojo searchPojo) {
        super(searchPojo);

        this.pojo = this.searchPojo = searchPojo;

        className = new ComponentName(searchPojo.packageName, searchPojo.activityName);
    }

    @Override
    public View display(final Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_app);

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);

        final ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
        if (position < 15) {
            appIcon.setImageDrawable(this.getDrawable(context));
        } else {
            // Do actions on a message queue to avoid performance issues on main thread
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    appIcon.setImageDrawable(getDrawable(context));
                }
            });
        }

        appName.setText(enrichText(String.format(context.getString(R.string.ui_item_search),
                searchPojo.name, "{" + searchPojo.query + "}")));

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void doLaunch(Context context, View v) {
        Intent search = new Intent(Intent.ACTION_SEARCH);
        search.setComponent(className);
        search.putExtra(SearchManager.QUERY, searchPojo.query);
        search.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(search);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {

        //empty menu so that you don't add on favorites
        PopupMenu menu = new PopupMenu(context, parentView);
        return menu;
    }
}
