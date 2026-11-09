package android.util;

import java.util.regex.Pattern;

/**
 * Test shim: the android.jar used for unit tests has no real implementation.
 * PHONE is copied verbatim from AOSP's android.util.Patterns.
 */
public class Patterns {
    public static final Pattern PHONE
            = Pattern.compile(                      // sdd = space, dot, or dash
            "(\\+[0-9]+[\\- \\.]*)?"        // +<digits><sdd>*
                    + "(\\([0-9]+\\)[\\- \\.]*)?"   // (<digits>)<sdd>*
                    + "([0-9][0-9\\- \\.]+[0-9])"); // <digit><digit|sdd>+<digit>
}
