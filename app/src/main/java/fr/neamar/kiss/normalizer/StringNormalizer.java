package fr.neamar.kiss.normalizer;

import java.util.regex.Pattern;

/**
 * String utils to handle accented characters for search and highlighting
 */
public class StringNormalizer {
    public static Pattern nonAscii = Pattern.compile("(?i)([^a-z0-9 -])");
    /**
     * Return the input string, lower-cased and with standard Ascii characters for common european accents
     *
     * @param input string input, with accents
     * @return normalized string
     */
    public static String normalize(String input) {
        return input.toLowerCase().replaceAll("[èéêë]", "e")
                .replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
                .replaceAll("[àâ]", "a").replaceAll("ô", "o").replaceAll("-", " ");
    }

    /**
     * Return a regexp matching common characters
     * For safe use, all non alpha characters are removed from input
     * Assume the input was previously sent to normalize()
     * <p/>
     * "aze" => /[àâa]z[èéêë]/
     *
     * @param input string to "un-normalize"
     * @return a regexp
     */
    public static String unNormalize(String input) {
        input = nonAscii.matcher(input.toLowerCase()).replaceAll("");

        return input.replaceAll("e", "[eèéêë]")
                .replaceAll("u", "[uûù]").replaceAll("i", "[iïî]")
                .replaceAll("a", "[aàâ]").replaceAll("o", "[oô]").replaceAll(" ", "[ -]");
    }
}
