package fr.neamar.kiss.normalizer;

/**
 * Created by neamar on 5/30/15.
 */
public class StringNormalizer {
    /**
     * Return the input string, lowercased and with standard Ascii characters for common european accents
     *
     * @param input
     * @return normalized string
     */
    public static String normalize(String input) {
        return input.toLowerCase().replaceAll("[èéêë]", "e")
                .replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
                .replaceAll("[àâ]", "a").replaceAll("ô", "o");
    }

    /**
     * Return a regexp matching common characters
     * For safe use, all non alpha characters are removed from input
     * "aze" => /[àâa]z[èéêë]/
     *
     * @param input string to "unnormalize"
     * @return a regexp
     */
    public static String unnormalize(String input) {
        input = input.toLowerCase().replaceAll("(?i)([^a-z -])", "");

        return input.replaceAll("e", "[eèéêë]").replaceAll("u", "[uûù]").replaceAll("i", "[iïî]").replace("a", "[aàâ]").replaceAll("o", "[oô]");
    }
}
