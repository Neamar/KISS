package fr.neamar.kiss.normalizer;

/**
 * Created by neamar on 5/30/15.
 */
public class StringNormalizer {
    /**
     * Return the input string, lowercased and with standard Ascii characters for common european accents
     * @param input
     * @return normalized string
     */
    public static String normalize(String input) {
        return input.toLowerCase().replaceAll("[èéêë]", "e")
                .replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
                .replaceAll("[àâ]", "a").replaceAll("ô", "o").replaceAll("[ÈÉÊË]", "E");
    }
}
