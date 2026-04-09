-dontobfuscate
-keep class fr.neamar.kiss.ExcludeAppSettingsFragment
-optimizations !code/allocation/variable
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-dontwarn javax.lang.model.element.Modifier # for errorprone
