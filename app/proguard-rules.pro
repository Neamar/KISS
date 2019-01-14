-dontobfuscate

-optimizationpasses 5
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-overloadaggressively
-optimizations !code/allocation/variable

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

#For Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-assumenosideeffects class fr.neamar.kiss.glide.GlideApp {
    public static fr.neamar.kiss.glide.GlideRequests with(androidx.fragment.app.Fragment);
    public static fr.neamar.kiss.glide.GlideRequests with(android.app.Fragment);
    public static fr.neamar.kiss.glide.GlideRequests with(android.view.View); # Has a switch with a reference to Fragment
}

-assumenosideeffects class com.bumptech.glide.manager.RequestManagerRetriever {
    public final com.bumptech.glide.RequestManager get(android.content.Context); # Has a switch with a reference to Fragment
    public static void findAllSupportFragmentsWithViews(java.util.Collection, java.util.Map);
    public final boolean handleMessage(android.os.Message); # Has a switch with a reference to Fragment
    private com.bumptech.glide.RequestManager get(androidx.fragment.app.FragmentActivity);
}

-assumenosideeffects class com.bumptech.glide.manager.SupportRequestManagerFragment {
    public <init>(com.bumptech.glide.manager.ActivityFragmentLifecycle);
    void setParentFragmentHint(androidx.fragment.app.Fragment);
    public void onAttach(android.content.Context);
    public void onDetach();
    public void onStart();
    public void onStop();
    public void onDestroy();
    public java.lang.String toString();
}

-assumenosideeffects class androidx.fragment.app.Fragment {*;}

-verbose