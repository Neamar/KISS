package fr.neamar.kiss.normalizer;

import android.util.Pair;

import java.text.Normalizer;

/**
 * String utils to handle accented characters for search and highlighting
 */
public class StringNormalizer {
    /**
     * Return the input string, lower-cased, with all combining characters parts stripped
     * (i.e. `ë` → `e`) and combination characters, consisting of one or more basic characters
     * (i.e. `Ⅱ` → `II`), decomposed into their respective parts
     *
     * @param input string input, with accents and anything else you can think of
     * @return normalized string and list that maps each result string position to its source
     *         string position
     */
    public static Pair<String, int[]> normalizeWithMap(String input) {
        StringBuilder    resultString = new StringBuilder();
        IntSequenceBuilder resultMap    = new IntSequenceBuilder(input.length() * 3 / 2);

        StringBuilder charBuffer = new StringBuilder(2);

        int inputOffset = 0, inputLength = input.length();
        while(inputOffset < inputLength) {
            int inputChar = input.codePointAt(inputOffset);

            // Decompose codepoint at given position
            charBuffer.append(Character.toChars(inputChar));
            String decomposedCharString = Normalizer.normalize(charBuffer, Normalizer.Form.NFKD);
            charBuffer.delete(0, charBuffer.length());

            // `inputChar` codepoint may be decomposed to four (or maybe even more) new code points
            int decomposedCharOffset = 0;
            while(decomposedCharOffset < decomposedCharString.length()) {
                int resultChar = decomposedCharString.codePointAt(decomposedCharOffset);

                // Only process characters that are not combining Unicode
                // characters. This way all the decomposed diacritical marks
                // (and some other not-that-important modifiers), that were
                // part of the original string or produced by the NFKD
                // normalizer above, disappear.
                switch(Character.getType(resultChar)) {
                    case Character.NON_SPACING_MARK:
                    case Character.COMBINING_SPACING_MARK:
                        // Some combining character found
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
     * @see StringNormalizer.normalizeWithMap(String)
     *
     * @param input string input, with accents and anything else you can think of
     * @return normalized string
     */
    public static String normalize(String input) {
        return StringNormalizer.normalizeWithMap(input).first;
    }
}
