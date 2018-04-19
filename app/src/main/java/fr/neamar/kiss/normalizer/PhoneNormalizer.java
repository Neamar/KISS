package fr.neamar.kiss.normalizer;

public class PhoneNormalizer {
    public static String simplifyPhoneNumber(String phoneNumber) {
        // This is done manually for performance reason,
        // But the algorithm is just a regexp replacement of "[-.():/ ]" with ""
        StringBuilder sb = new StringBuilder();
        int phoneLength = phoneNumber.length();
        for(int i = 0; i < phoneLength; i++)
        {
            char c = phoneNumber.charAt(i);
            if(c == ' ' || c == '-' || c == '.' || c == '(' || c == ')' || c == ':' || c == '/') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
