package fr.neamar.kiss.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.EventPojo;

/**
 * Created by nmitsou on 09.11.15.
 */
public class LoadEventPojos extends LoadPojos<EventPojo> {
    public LoadEventPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<EventPojo> doInBackground(Void... params) {

        ArrayList<EventPojo> events = new ArrayList<>();

        String[] proj =
                new String[]{
                        CalendarContract.Events._ID,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DESCRIPTION};


        String selectionClause = "(dtstart >= ? and dtend <=?)";
        String[] selectionsArgs = new String[]{"" + new Date().getTime(), ""+ getDateInFuture(new Date()).getTime()};

        Cursor cursor = context.getContentResolver()
                .query(
                        Uri.parse("content://com.android.calendar/events"),
                        proj, selectionClause,
                        selectionsArgs, "dtstart");


        cursor.moveToFirst();
        // fetching calendars name
        int eventsCount = cursor.getCount();

        for (int i = 0; i < eventsCount; i++) {

            EventPojo event = new EventPojo();
            event.description = cursor.getString(4);
            event.name = cursor.getString(3);
            event.id = cursor.getString(0);
            event.startDate = getDate(Long.parseLong(cursor.getString(1)));
            event.stopDate = getDate(Long.parseLong(cursor.getString(2)));
            event.nameNormalized = StringNormalizer.normalize(event.name);
            events.add(event);
            cursor.moveToNext();

        }
        return events;

    }
    public static String getDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yy hh:mm a");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    //method created for demonstration purposes
    public Date getDateInFuture(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 30); //1 month
        return calendar.getTime();
    }

}
