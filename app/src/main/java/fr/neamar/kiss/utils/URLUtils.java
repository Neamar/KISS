package fr.neamar.kiss.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {
    // host is either an IPv4 address or dotted labels whose last label contains a letter (so 0.1+0.2 isn't a url),
    // optionally followed by a port, path, query or fragment
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[0-9]{1,3}(?:[.][0-9]{1,3}){3}|(?:(?:[a-z0-9-]|[^\\x00-\\x7F])+[.])+[0-9-]*(?:[a-z]|[^\\x00-\\x7F])(?:[a-z0-9-]|[^\\x00-\\x7F])*)(?:[:/?#].*)?$";
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
