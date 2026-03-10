package fr.neamar.kiss.dataprovider.simpleprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SearchPojoType;
import fr.neamar.kiss.searcher.Searcher;

public class TimerProvider extends SimpleProvider<SearchPojo> {
    private static final String TIMER_SCHEME = "timer://";
    private static final Pattern TIMER_REGEXP = Pattern.compile("^(?:\\s*([0-9]*\\.?[0-9]+)\\s*h)?(?:\\s*([0-9]*\\.?[0-9]+)\\s*m)?(?:\\s*([0-9]*\\.?[0-9]+)\\s*s)?\\s*$", Pattern.CASE_INSENSITIVE);

    private final SharedPreferences prefs;

    public TimerProvider(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    static OptionalLong getTimerDurationSeconds(String query) {
        if (query == null || query.trim().isEmpty()) {
            return OptionalLong.empty();
        }

        Matcher matcher = TIMER_REGEXP.matcher(query);
        if (!matcher.matches()) {
            return OptionalLong.empty();
        }

        String hoursStr = matcher.group(1);
        String minutesStr = matcher.group(2);
        String secondsStr = matcher.group(3);
        if (hoursStr == null && minutesStr == null && secondsStr == null) {
            return OptionalLong.empty();
        }

        try {
            double hours = hoursStr != null ? Double.parseDouble(hoursStr) : 0;
            double minutes = minutesStr != null ? Double.parseDouble(minutesStr) : 0;
            double seconds = secondsStr != null ? Double.parseDouble(secondsStr) : 0;

            long totalSeconds = (long) Math.ceil(hours * 3600 + minutes * 60 + seconds);
            if (totalSeconds <= 0 || totalSeconds > Integer.MAX_VALUE) {
                return OptionalLong.empty();
            }

            return OptionalLong.of(totalSeconds);
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        if (!prefs.getBoolean("enable-timer", true)) {
            return;
        }

        OptionalLong totalSeconds = getTimerDurationSeconds(query);
        if (!totalSeconds.isPresent()) {
            return;
        }

        SearchPojo pojo = new SearchPojo(TIMER_SCHEME + totalSeconds.getAsLong(), query, String.valueOf(totalSeconds.getAsLong()), SearchPojoType.TIMER_QUERY);
        pojo.relevance = 25;
        searcher.addResult(pojo);
    }
}
