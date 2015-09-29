package fr.neamar.kiss.normalizer;

import android.util.Pair;

import java.text.Normalizer;

/**
 * String utils to handle accented characters for search and highlighting
 */
public class StringNormalizer {
    /**
     * Make the given string easier to compare by performing a number of simplifications on it
     * <p/>
     * 1. Decompose combination characters into their respective parts (see below)
     * 2. Strip all combining character marks (see below)
     * 3. Strip some other common-but-not-very-useful characters (such as dashes)
     * 4. Lower-case the string
     * <p/>
     * Combination characters are characters that (essentially) have the same meaning as one or
     * more other, more common, characters. Examples for these include:
     * Roman numerals (`Ⅱ` → `II`) and half-width katakana (`ﾐ` → `ミ`)
     * <p/>
     * Combining character marks are diacritics and other extra strokes that are often found as
     * part of many characters in non-English roman scripts. Examples for these include:
     * Diaereses (`ë` → `e`), acutes (`á` → `a`) and macrons (`ō` → `o`)
     *
     * @param input string input, with accents and anything else you can think of
     * @return normalized string and list that maps each result string position to its source
     * string position
     */
    public static Pair<String, int[]> normalizeWithMap(String input) {
        StringBuilder resultString = new StringBuilder();
        IntSequenceBuilder resultMap = new IntSequenceBuilder(input.length() * 3 / 2);

        StringBuilder charBuffer = new StringBuilder(2);

        int inputOffset = 0, inputLength = input.length();
        while (inputOffset < inputLength) {
            int inputChar = input.codePointAt(inputOffset);

            // Decompose codepoint at given position
            charBuffer.append(Character.toChars(inputChar));
            String decomposedCharString = Normalizer.normalize(charBuffer, Normalizer.Form.NFKD);
            charBuffer.delete(0, charBuffer.length());

            // `inputChar` codepoint may be decomposed to four (or maybe even more) new code points
            int decomposedCharOffset = 0;
            while (decomposedCharOffset < decomposedCharString.length()) {
                int resultChar = decomposedCharString.codePointAt(decomposedCharOffset);

                // Skip characters for some unicode character classes, including:
                //  * combining characters produced by the NFKD normalizer above
                //  * dashes
                // See the method's description for more information
                switch (Character.getType(resultChar)) {
                    case Character.NON_SPACING_MARK:
                    case Character.COMBINING_SPACING_MARK:
                        // Some combining character found
                        break;

                    case Character.DASH_PUNCTUATION:
                        // Some other unwanted character found
                        break;

                    default:
                        resultString.appendCodePoint(Character.toLowerCase(resultChar));
                        resultMap.add(inputOffset);
                }

                decomposedCharOffset += Character.charCount(resultChar);
            }

            inputOffset += Character.charCount(inputChar);
        }

        // Since we stripped all combining Unicode characters in the
        // previous while-loop there should be no combining character
        // remaining in the string and the composed and decomposed
        // versions of the string should be equivalent. This also means
        // we do not need to convert the string back to composed Unicode
        // before returning it.
        return new Pair<>(resultString.toString(), resultMap.toArray());
    }


    /**
     * Make the given string easier to compare by performing a number of simplifications on it
     *
     * @param input string input, with accents and anything else you can think of
     * @return normalized string
     * @see StringNormalizer#normalizeWithMap(String)
     */
    public static String normalize(String input) {
        return StringNormalizer.normalizeWithMap(input).first;
    }
}
