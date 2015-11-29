package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.EventPojo;

/**
 * Created by nmitsou on 09.11.15.
 */
public class EventResult extends Result {
    private final EventPojo eventPojo;

    public EventResult(EventPojo eventPojo) {
        super();
        this.pojo = this.eventPojo = eventPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_event);

        TextView eventTitle = (TextView) v.findViewById(R.id.item_event_text);
        eventTitle.setText(enrichText(eventPojo.displayName));

        TextView eventDate = (TextView) v.findViewById(R.id.item_event_date);
        eventDate.setText(enrichText(eventPojo.displayDate));

        return v;
    }



    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void doLaunch(Context context, View v) {
        //sanity check, this should never be the case
        // since calendar events are not enabled on older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            return;
        }
        recordLaunch(context);
        Intent calendar = new Intent(Intent.ACTION_VIEW);
        Uri.Builder uri = CalendarContract.Events.CONTENT_URI.buildUpon();
        uri.appendPath(eventPojo.id);
        calendar.setData(uri.build());
        context.startActivity(calendar);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        return super.popupMenuClickHandler(context, parent, item);
    }
}
