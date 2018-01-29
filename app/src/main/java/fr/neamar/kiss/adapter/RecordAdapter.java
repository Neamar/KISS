package fr.neamar.kiss.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import fr.neamar.kiss.BadgeHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.result.ContactsResult;
import fr.neamar.kiss.result.PhoneResult;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.result.SearchResult;
import fr.neamar.kiss.result.SettingsResult;
import fr.neamar.kiss.result.ShortcutsResult;
import fr.neamar.kiss.result.TogglesResult;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ListPopup;

public class RecordAdapter extends ArrayAdapter<Result> {

    private final QueryInterface parent;
    /**
     * Array list containing all the results currently displayed
     */
    private final ArrayList<Result> results;

    public RecordAdapter(Context context, QueryInterface parent, int textViewResourceId,
                         ArrayList<Result> results) {
        super(context, textViewResourceId, results);

        this.parent = parent;
        this.results = results;
    }

    @Override
    public int getViewTypeCount() {
        return 7;
    }

    @Override
    public int getItemViewType(int position) {
        if (results.get(position) instanceof AppResult)
            return 0;
        else if (results.get(position) instanceof SearchResult)
            return 1;
        else if (results.get(position) instanceof ContactsResult)
            return 2;
        else if (results.get(position) instanceof TogglesResult)
            return 3;
        else if (results.get(position) instanceof SettingsResult)
            return 4;
        else if (results.get(position) instanceof PhoneResult)
            return 5;
        else if (results.get(position) instanceof ShortcutsResult)
            return 6;
        else
            return -1;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public long getItemId( int position )
    {
        return results.get(position).getUniqueId();
    }

    @Override
     public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if ( convertView != null )
        {
            if ( !(convertView.getTag() instanceof Integer) )
                convertView = null;
            else if ( (Integer)convertView.getTag() != getItemViewType( position ) )
            {
                // This is happening on HTC Desire X (Android 4.1.1, API 16)
                //throw new IllegalStateException( "can't convert view from different type" );
                convertView = null;
            }
        }
        View view = results.get(position).display(getContext(), results.size() - position, convertView);
        //Log.d( "TBog", "getView pos " + position + " convertView " + ((convertView == null) ? "null" : convertView.toString()) + " will return " + view.toString() );
        view.setTag( getItemViewType( position ) );
        return view;
    }

    public void onLongClick(final int pos, View v) {
        ListPopup menu = results.get(pos).getPopupMenu(getContext(), this, v);

        //check if menu contains elements and if yes show it
        if (menu.getAdapter().getCount() > 0) {
            parent.registerPopup( menu );
            menu.show( v );
        }
    }

    public void onClick(final int position, View v) {
        final Result result;

        try {
            result = results.get(position);
            result.launch(getContext(), v);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return;
        }

        // Record the launch after some period,
        // * to ensure the animation runs smoothly
        // * to avoid a flickering -- launchOccurred will refresh the list
        // Thus TOUCH_DELAY * 3
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                parent.launchOccurred(results.size() - position, result);
            }
        }, KissApplication.TOUCH_DELAY * 3);

    }

    public void removeResult(Result result) {
        results.remove(result);
        result.deleteRecord(getContext());
        notifyDataSetChanged();
    }

    public void reloadBadges() {
        for (Result result : results) {
            if (result instanceof AppResult) {
                AppResult appResult = (AppResult) result;
                appResult.reloadBadgeCount(getContext());
            }
        }

        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecordAdapter.this.notifyDataSetChanged();
            }
        });
    }

    public void reloadBadge(String packageName) {
        boolean found = false;
        for (Result result : results) {
            if (result instanceof AppResult) {
                AppResult appResult = (AppResult) result;
                if (appResult.getPackageName().equals(packageName)) {
                    appResult.reloadBadgeCount(getContext());
                    found = true;
                    break;
                }
            }
        }

        if (found) {
            ((MainActivity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RecordAdapter.this.notifyDataSetChanged();
                }
            });
        }

    }
}
