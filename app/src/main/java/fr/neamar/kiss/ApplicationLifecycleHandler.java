package fr.neamar.kiss;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.os.Bundle;

import fr.neamar.kiss.cache.MemoryCacheHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    //private static final String TAG = ApplicationLifecycleHandler.class.getSimpleName();
    private static boolean isInBackground = false;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {

        if (isInBackground) {
            //Log.d(TAG, "app went to foreground");
            isInBackground = false;

            for (Pojo pojo : KissApplication.getApplication(activity).getDataHandler().getApplications()) {
                if (!(pojo instanceof AppPojo))
                    continue;
                AppPojo appPojo = (AppPojo) pojo;
                ComponentName componentName = new ComponentName(appPojo.packageName, appPojo.activityName);
                MemoryCacheHelper.cacheAppIconDrawable(activity, componentName, appPojo.userHandle);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            //Log.d(TAG, "app went to background");
            isInBackground = true;
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            MemoryCacheHelper.trimMemory();
        }
    }
}