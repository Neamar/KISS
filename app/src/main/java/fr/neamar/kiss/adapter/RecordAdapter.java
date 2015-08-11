package fr.neamar.kiss.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.result.ContactResult;
import fr.neamar.kiss.result.PhoneResult;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.result.SearchResult;
import fr.neamar.kiss.result.SettingResult;
import fr.neamar.kiss.result.ToggleResult;
import fr.neamar.kiss.searcher.QueryInterface;

public class RecordAdapter extends ArrayAdapter<Result> {

    private final QueryInterface parent;
    /**
     * Array list containing all the results currently displayed
     */
    private ArrayList<Result> results = new ArrayList<>();

    public RecordAdapter(Context context, QueryInterface parent, int textViewResourceId,
                         ArrayList<Result> results) {
        super(context, textViewResourceId, results);

        this.parent = parent;
        this.results = results;
    }

    public int getViewTypeCount() {
        return 6;
    }

    public int getItemViewType(int position) {
        if (results.get(position) instanceof AppResult)
            return 0;
        else if (results.get(position) instanceof SearchResult)
            return 1;
        else if (results.get(position) instanceof ContactResult)
            return 2;
        else if (results.get(position) instanceof ToggleResult)
            return 3;
        else if (results.get(position) instanceof SettingResult)
            return 4;
        else if (results.get(position) instanceof PhoneResult)
            return 5;
        else
            return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return results.get(position).display(getContext(), results.size() - position, convertView);
    }

    public void onLongClick(final int pos, View v) {
        // Check that the pojo is an app
        if (getItemViewType(pos) == 0)
        {
            final AppPojo appPojo = ((AppResult)results.get(pos)).appPojo;
            PopupMenu popup = new PopupMenu(getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_app, popup.getMenu());

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    popupMenuClickHandler(appPojo, pos, item);
                    return true;
                }
            });

            popup.show();
        }
        else {
            removeItem(pos);
        }
    }

    private void popupMenuClickHandler(AppPojo appPojo, int pos, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_remove:
                removeItem(pos);
                break;
            case R.id.item_app_details:
                launchAppDetails(appPojo);
                break;
            case R.id.item_app_uninstall:
                launchUninstall(appPojo);
                break;
        }
    }

    private void removeItem(int pos) {
        results.get(pos).deleteRecord(getContext());
        results.remove(pos);
        Toast.makeText(getContext(), R.string.removed_item, Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    private void launchAppDetails(AppPojo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                   Uri.fromParts("package", app.packageName, null));
        getContext().startActivity(intent);
    }

    private void launchUninstall(AppPojo app) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                                   Uri.fromParts("package", app.packageName, null));
        getContext().startActivity(intent);
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
}
