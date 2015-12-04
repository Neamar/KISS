package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.EventPojo;

/**
 * Created by nmitsou on 09.11.15.
 */
public class EventResult extends Result {
    private final EventPojo eventPojo;
    private Drawable icon = null;

    public EventResult(EventPojo eventPojo) {
        super();
        this.pojo = this.eventPojo = eventPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_event);

        TextView eventTitle = (TextView) v.findViewById(R.id.item_event_text);
        TextView eventDate = (TextView) v.findViewById(R.id.item_event_date);
        eventTitle.setText(enrichText(eventPojo.displayName));
        eventDate.setText(enrichText(eventPojo.displayDate));
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("icons-hide", false)) {

            if (position < 15) {

                final ImageView appIcon = (ImageView) v.findViewById(R.id.item_event_icon);
                appIcon.setImageDrawable(getDrawable(eventTitle.getCurrentTextColor()));
            }
        }
        return v;
    }

    private Drawable getDrawable(int color) {
        if (icon == null)
        {
            icon = new EventDrawable(eventPojo.startDate, color);
        }
        return icon;
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

class EventDrawable extends Drawable {

    private final Paint paint;
    private String day;
    private String month;
    private int color;
    private static SimpleDateFormat df = new SimpleDateFormat("MMM");

    public EventDrawable(Date date, int color)
    {
        paint = new Paint();
        this.color = color;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        month = df.format(date).toUpperCase();
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(color);
        paint.setTextSize(32);
        canvas.drawText(day, 14, 33, paint);
        paint.setTextSize(18);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(month, 15, 58, paint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}