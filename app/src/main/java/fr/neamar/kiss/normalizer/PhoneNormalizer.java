package fr.neamar.kiss.normalizer;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

public class PhoneNormalizer {
    public static String convertKeypadLettersToDigits(String phoneNumber) {
        return PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber);
    }

    public static StringNormalizer.Result simplifyPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return StringNormalizer.Result.EMPTY;
        }

        phoneNumber = convertKeypadLettersToDigits(phoneNumber);

        // This is done manually for performance reason,
        // But the algorithm is just a regexp replacement of "[-.():/ ]" with ""

        int numCodePoints = Character.codePointCount(phoneNumber, 0, phoneNumber.length());
        IntSequenceBuilder codePoints = new IntSequenceBuilder(numCodePoints);
        IntSequenceBuilder resultMap = new IntSequenceBuilder(numCodePoints);

        int i = 0;
        for (int iterCodePoint = 0; iterCodePoint < numCodePoints; iterCodePoint += 1) {
            int c = Character.codePointAt(phoneNumber, i);

            if (c != ' ' && c != '-' && c != '.' && c != '(' && c != ')' && c != ':' && c != '/') {
                codePoints.add(c);
                resultMap.add(i);
            }
            i += Character.charCount(c);
        }

        return new StringNormalizer.Result(phoneNumber.length(), codePoints.toArray(), resultMap.toArray());
    }
}
