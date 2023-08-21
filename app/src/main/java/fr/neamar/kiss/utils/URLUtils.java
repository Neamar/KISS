package fr.neamar.kiss.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);

    /**
     * @param query url submitted
     * @return true, if query matches pattern for url
     */
    public static boolean matchesUrlPattern(final String query) {
        Matcher m = urlPattern.matcher(query);
        return m.find();
    }
}
