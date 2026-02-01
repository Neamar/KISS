package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SearchPojoType;
import fr.neamar.kiss.searcher.Searcher;

public class TimerProvider extends SimpleProvider<SearchPojo> {
    private final Pattern timerRegexp;

    public TimerProvider() {
        // Matches strings like "1.5h 30m 10s" or "5m" or "30s" or ".25m"
        // Case insensitive.
        // Expects order: hours, minutes, seconds. All optional, but at least one must be present for the logic check.
        // Supports decimal numbers.
        timerRegexp = Pattern.compile("^(?:\\s*([0-9]*\\.?[0-9]+)\\s*h)?(?:\\s*([0-9]*\\.?[0-9]+)\\s*m)?(?:\\s*([0-9]*\\.?[0-9]+)\\s*s)?\\s*$", Pattern.CASE_INSENSITIVE);
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

            double hours = hoursStr != null ? Double.parseDouble(hoursStr) : 0;
            double minutes = minutesStr != null ? Double.parseDouble(minutesStr) : 0;
            double seconds = secondsStr != null ? Double.parseDouble(secondsStr) : 0;

            long totalSeconds = (long) Math.ceil(hours * 3600 + minutes * 60 + seconds);
            
            if (totalSeconds == 0 || totalSeconds > Integer.MAX_VALUE) {
                return;
            }

            long displayHours = totalSeconds / 3600;
            long displayMinutes = (totalSeconds % 3600) / 60;
            long displaySeconds = totalSeconds % 60;
            
            // Build the display string
            StringBuilder display = new StringBuilder("Start timer: ");
            if (displayHours > 0) display.append(displayHours).append("h ");
            if (displayMinutes > 0) display.append(displayMinutes).append("m ");
            if (displaySeconds > 0) display.append(displaySeconds).append("s");

            // Create the Pojo
            // We store the total seconds in the URL field for easy retrieval
            SearchPojo pojo = new SearchPojo("timer://" + totalSeconds, display.toString().trim(), String.valueOf(totalSeconds), SearchPojoType.TIMER_QUERY);
            pojo.relevance = 25;
            searcher.addResult(pojo);
        }
    }
}
