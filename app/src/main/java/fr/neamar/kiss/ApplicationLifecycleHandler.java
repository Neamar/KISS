package fr.neamar.kiss;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.List;

import fr.neamar.kiss.cache.MemoryCacheHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {

        List<Pojo> appPojoList = KissApplication.getApplication(activity).getDataHandler().getApplications();
        if (appPojoList != null) {
            for (Pojo pojo : appPojoList) {
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
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            MemoryCacheHelper.trimMemory();
        }
    }
}