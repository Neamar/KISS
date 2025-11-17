package fr.neamar.kiss.normalizer;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import fr.neamar.kiss.utils.PhoneUtils;

public class PhoneNormalizer {

    /**
     * Normalize phone number by removing all characters other than digits.
     * If the given phone number contains keypad letters, these will be converted to digits first.
     * <p>
     * Works similar to {@link PhoneNumberUtils#normalizeNumber(String)}
     *
     * @param phoneNumber the number to be normalized.
     * @return the normalized number.
     */
    public static StringNormalizer.Result normalizeWithResult(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return StringNormalizer.Result.EMPTY;
        }
        phoneNumber = PhoneUtils.convertKeypadLettersToDigits(phoneNumber);

        // This is done manually for performance reason,
        // But the algorithm is just a regexp replacement of "[-.():/ ]" with ""

        int numCodePoints = Character.codePointCount(phoneNumber, 0, phoneNumber.length());
        IntSequenceBuilder codePoints = new IntSequenceBuilder(numCodePoints);
        IntSequenceBuilder resultMap = new IntSequenceBuilder(numCodePoints);

        int i = 0;
        for (int iterCodePoint = 0; iterCodePoint < numCodePoints; iterCodePoint += 1) {
            int c = Character.codePointAt(phoneNumber, i);
            int digit = Character.digit(c, 10);
            if (digit != -1 || (codePoints.size() == 0 && c == '+')) {
                codePoints.add(c);
                resultMap.add(i);
            }
            i += Character.charCount(c);
        }

        return new StringNormalizer.Result(phoneNumber.length(), codePoints.toArray(), resultMap.toArray());
    }
}
