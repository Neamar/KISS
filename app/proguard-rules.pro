-dontobfuscate
# Settings screens are only referenced from preferences XML
-keep class fr.neamar.kiss.settings.** extends androidx.preference.PreferenceFragmentCompat
-optimizations !code/allocation/variable
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
