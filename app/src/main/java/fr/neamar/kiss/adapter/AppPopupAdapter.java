package fr.neamar.kiss.adapter;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import fr.neamar.kiss.R;
import fr.neamar.kiss.result.AppPopupResult;

public class AppPopupAdapter extends BaseAdapter {
    ArrayList<AppPopupResult> items;

    public AppPopupAdapter(ArrayList<AppPopupResult> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate the layout for each list row
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.popup_app, parent, false);
        }

        AppPopupResult currentItem = items.get(position);
        TextView appName = convertView.findViewById(R.id.item_app_name);
        //sets the text for item name and item description from the current item object
        appName.setText(currentItem.name);

        try
        {
            Drawable icon = parent.getContext().getPackageManager().getApplicationIcon(currentItem.packageName);
            ImageView appIcon = convertView.findViewById(R.id.item_app_icon);
            appIcon.setImageDrawable(icon);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        // returns the view for the current row
        return convertView;
    }
}
