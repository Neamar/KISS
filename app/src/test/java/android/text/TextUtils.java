package android.text;

/**
 * see <a href="https://medium.com/@okmanideep/dont-create-that-stringutils-to-unit-test-your-android-class-8ab32af34e84">https://medium.com/@okmanideep/dont-create-that-stringutils-to-unit-test-your-android-class-8ab32af34e84</a>
 */
public class TextUtils {
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
