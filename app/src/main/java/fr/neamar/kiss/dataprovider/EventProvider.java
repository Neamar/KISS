package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.loader.LoadEventPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AliasPojo;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.EventPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SettingPojo;

public class EventProvider extends Provider<EventPojo> {
    private final String eventsName;
    private SharedPreferences prefs;

    public EventProvider(Context context) {
        super(new LoadEventPojos(context));
        eventsName="Events: ".toLowerCase();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public ArrayList<Pojo> getResults(String query) {
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String eventNameLowerCased;
        if (query.length()<2)
            return results;
        if (query.equals("!day"))
        {
            return getTodaysEvents();
        } else if (query.equals("!week")) {
            return getWeeksEvents();
        }
        else if (query.equals("!month")) {
            return getMonthsEvents();
        }

        if (prefs.getBoolean("enable-events-special-search-only", false)) {
            return results;
        }
        for (EventPojo event : pojos) {
            relevance = 0;
            eventNameLowerCased = event.nameNormalized;
            if (eventNameLowerCased.startsWith(query)) {
                relevance = 10;
            }
            else if (eventNameLowerCased.contains(query)) {
                relevance = 5;
            }
            else if (eventsName.startsWith(query)) {
                // Also display for a search on "events" for instance
                relevance = 4;
            }

            if (relevance>0)
            {
                //event.startDate;

                event.displayName = event.name.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}");

                event.relevance = relevance;
                results.add(event);
            }
        }

        return results;
    }

    private ArrayList<Pojo> getEvents(int hours)
    {
        ArrayList<Pojo> results = new ArrayList<>();
        for (EventPojo event : pojos) {
            if (diffDatesInHours(new Date(), event.startDate) < hours) {
                event.displayName = event.name;
                event.relevance = 10;
                results.add(event);
            }
        }
        return results;
    }

    private ArrayList<Pojo> getTodaysEvents()
    {
        return getEvents(24);
    }


    private ArrayList<Pojo> getWeeksEvents() {
        return getEvents(24 * 7);
    }


    private ArrayList<Pojo> getMonthsEvents() {
        return getEvents(24 * 31);
    }

    /**
     * Return a Pojo
     *
     * @param id              we're looking for
     * @param allowSideEffect do we allow this function to have potential side effect? Set to false to ensure none.
     * @return an apppojo, or null
     */
    public Pojo findById(String id, Boolean allowSideEffect) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                // Reset displayName to default value
                if (allowSideEffect) {
                    pojo.displayName = pojo.name;
                }
                return pojo;
            }

        }

        return null;
    }

    public Pojo findById(String id) {
        return findById(id, true);
    }

    public ArrayList<Pojo> getAllEvents() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());
        records.trimToSize();

        for (Pojo pojo : pojos) {
            pojo.displayName = pojo.name;
            records.add(pojo);
        }
        return records;
    }

    public int diffDatesInHours(Date d1, Date d2)
    {
        return Math.round((d2.getTime()-d1.getTime())/1000/60/60);
    }
}
