package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SearchPojoType;
import fr.neamar.kiss.searcher.Searcher;

public class TimerProvider extends SimpleProvider<SearchPojo> {
    private final Pattern timerRegexp;

    public TimerProvider() {
        // Matches strings like "1h 30m 10s" or "5m" or "30s"
        // Case insensitive.
        // Expects order: hours, minutes, seconds. All optional, but at least one must be present for the logic check.
        timerRegexp = Pattern.compile("^(?:\\s*(\\d+)\\s*h)?(?:\\s*(\\d+)\\s*m)?(?:\\s*(\\d+)\\s*s)?\\s*$", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        if (query.trim().isEmpty()) {
            return;
        }

        Matcher m = timerRegexp.matcher(query);
        if (m.matches()) {
            String hoursStr = m.group(1);
            String minutesStr = m.group(2);
            String secondsStr = m.group(3);

            // If no time is specified, it's not a timer request
            if (hoursStr == null && minutesStr == null && secondsStr == null) {
                return;
            }

            int hours = hoursStr != null ? Integer.parseInt(hoursStr) : 0;
            int minutes = minutesStr != null ? Integer.parseInt(minutesStr) : 0;
            int seconds = secondsStr != null ? Integer.parseInt(secondsStr) : 0;

            long totalSeconds = hours * 3600L + minutes * 60L + seconds;
            
            if (totalSeconds == 0 || totalSeconds > Integer.MAX_VALUE) {
                return;
            }
            
            // Build the display string
            StringBuilder display = new StringBuilder("Start timer: ");
            if (hours > 0) display.append(hours).append("h ");
            if (minutes > 0) display.append(minutes).append("m ");
            if (seconds > 0) display.append(seconds).append("s");

            // Create the Pojo
            // We store the total seconds in the URL field for easy retrieval
            SearchPojo pojo = new SearchPojo("timer://" + totalSeconds, display.toString().trim(), String.valueOf(totalSeconds), SearchPojoType.TIMER_QUERY);
            pojo.relevance = 25;
            searcher.addResult(pojo);
        }
    }
}
