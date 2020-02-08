package fr.neamar.kiss.normalizer;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;
import java.text.Normalizer;
import java.util.Arrays;

/**
 * String utils to handle accented characters for search and highlighting
 */
public class StringNormalizer {
    private StringNormalizer() {
    }

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
     * @param input         string input, with accents and anything else you can think of
     * @param makeLowercase make all characters lowercase
     * @return normalized string and list that maps each result string position to its source
     * string position
     */
    public static Result normalizeWithResult(CharSequence input, boolean makeLowercase) {
        int numCodePoints = Character.codePointCount(input, 0, input.length());
        IntSequenceBuilder codePoints = new IntSequenceBuilder(numCodePoints);
        IntSequenceBuilder resultMap = new IntSequenceBuilder(numCodePoints);
        CharBuffer buffer = CharBuffer.allocate(2);
        int i = 0;
        for (int iterCodePoint = 0; iterCodePoint < numCodePoints; iterCodePoint += 1) {
            int codepoint = Character.codePointAt(input, i);
            String decomposedCharString;
            // Is it within the basic latin range?
            // If so, we can skip the expensive call to Normalizer.normalize
            if(codepoint < 'z') {
                // Ascii range, no need to normalize!
                // Add directly if it's not a dash
                // (HYPHEN-MINUS is the only character before 'z' in one of the
                //  NON_SPACING_MARK / COMBINING_SPACING_MARK / DASH_PUNCTUATION
                //  category, so we can skip the Character.getType() and explicitly check for it)
                if(codepoint != '-') {
                    codePoints.add(makeLowercase ? Character.toLowerCase(codepoint) : codepoint);
                    resultMap.add(i);
                }
            }
            else {
                // Otherwise, we'll need to normalize the code point to a letter and potential accentuation
                buffer.put(Character.toChars(codepoint));
                buffer.flip();
                decomposedCharString = Normalizer.normalize(buffer, Normalizer.Form.NFKD);
                buffer.clear();

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
                            // See http://www.fileformat.info/info/unicode/category/Mn/list.htm
                            // And http://www.fileformat.info/info/unicode/category/Mc/list.htm
                            break;

                        case Character.DASH_PUNCTUATION:
                            // We skip dashes too
                            // (standard HYPHEN-MINUS was skipped above, but dashes are a large family!)
                            // see http://www.fileformat.info/info/unicode/category/Pd/list.htm
                            break;

                        default:
                            codePoints.add(makeLowercase ? Character.toLowerCase(resultChar) : resultChar);
                            resultMap.add(i);
                    }

                    decomposedCharOffset += Character.charCount(resultChar);
                }
            }

            i += Character.charCount(codepoint);
        }

        return new Result(input.length(), codePoints.toArray(), resultMap.toArray());
    }

    public static class Result implements Comparable<Result> {
        private final int originalInputLastCharPosition;
        public final int[] codePoints;
        private final int[] mapPositions;

        Result(final int originalInputLastCharPosition,
               final int[] codePoints, final int[] mapPositions) {
            if (codePoints.length != mapPositions.length)
                throw new IllegalStateException("Each codepoint needs a mapped position");
            this.originalInputLastCharPosition = originalInputLastCharPosition;
            this.codePoints = codePoints;
            this.mapPositions = mapPositions;
        }

        public int length() {
            return this.codePoints.length;
        }

        /**
         * Map a position in the normalized string to a position in the original string
         *
         * @param position Position in normalized string
         * @return Position in non-normalized string
         */
        public int mapPosition(int position) {
            if (position < mapPositions.length)
                return mapPositions[position];
            // We are behind the last character, return the position of the end of the original input
            return originalInputLastCharPosition;
        }

        @Override
        public int compareTo(@NonNull Result that) {
            // this optimization is usually worthwhile, and can always be added
            if (this == that)
                return 0;

            int result;
            int minLength = Math.min(this.codePoints.length, that.codePoints.length);
            for (int i = 0; i < minLength; i += 1) {
                if ((result = Character.toLowerCase(this.codePoints[i]) - Character.toLowerCase(that.codePoints[i])) != 0)
                    return result;
            }

            if (this.codePoints.length != that.codePoints.length)
                return this.codePoints.length - that.codePoints.length;
            
            // equal
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Result))
                return false;

            Result result = (Result) o;

            return Arrays.equals(codePoints, result.codePoints);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(codePoints);
        }

        @Override
        public String toString() {
            // Since we stripped all combining Unicode characters in the
            // normalization function there should be no combining character
            // remaining in the string and the composed and decomposed
            // versions of the string should be equivalent. This also means
            // we do not need to convert the string back to composed Unicode
            // before returning it.
            StringBuilder sb = new StringBuilder(codePoints.length);
            for (int codePoint : codePoints)
                sb.appendCodePoint(codePoint);
            return sb.toString();
        }
    }
}
