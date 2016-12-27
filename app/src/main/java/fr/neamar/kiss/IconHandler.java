package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import fr.neamar.kiss.drawablecache.DiskDrawableCache;
import fr.neamar.kiss.drawablecache.MemoryDrawableCache;


public class IconHandler {

    private static final String TAG = "IconHandler";
    private final MemoryDrawableCache memoryCache = new MemoryDrawableCache();
    private final DiskDrawableCache diskCache;
    private IconPack iconPack;

    public IconHandler(Context ctx) {
        super();
        iconPack = KissApplication.getIconPack();
        diskCache = new DiskDrawableCache(ctx);
    }

    public void reset() {
        iconPack = KissApplication.getIconPack();
        memoryCache.clear();
        diskCache.clear();
    }

    private String getCacheKey(ComponentName componentName) {
        if (iconPack != null) {
            return iconPack.packageName + "_" + componentName.toString();
        }
        return "default_" + componentName.toString();
    }

    public boolean setIconToView(Context context, ImageView view, ComponentName componentName) {
        Drawable icon = memoryCache.get(getCacheKey(componentName));
        if (icon != null) {
            view.setImageDrawable(icon);
            view.setTag(R.id.tag_icon_worker, getCacheKey(componentName));
            return true;
        }

        if (view.getTag(R.id.tag_icon_worker) == null || !view.getTag(R.id.tag_icon_worker).equals(getCacheKey(componentName))) {
            view.setImageResource(0);
            GetIconWorkerTask task = new GetIconWorkerTask(context, KissApplication.getIconPack(), view, componentName);
            if (Build.VERSION.SDK_INT > 10) {
                // Force multithreading
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
            return false;
        }
        return true;
    }

    private class GetIconWorkerTask extends AsyncTask<Void, Void, Drawable> {

        private static final String TAG = "GetIconWorkerTask";
        private final IconPack iconPack;
        private final PackageManager packageManager;
        private final Context context;
        private final ImageView target;
        private final ComponentName componentName;

        GetIconWorkerTask(Context context, IconPack iconPack,
                          ImageView target, ComponentName componentName) {
            this.target = target;
            this.target.setTag(R.id.tag_icon_worker, getCacheKey(componentName));
            this.iconPack = iconPack;
            this.context = context;
            this.packageManager = context.getPackageManager();
            this.componentName = componentName;
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            try {
                Drawable icon = null;

                // Search first in the disk cache
                icon = diskCache.get(getCacheKey(componentName));
                if (icon != null) {
                    memoryCache.put(getCacheKey(componentName), (BitmapDrawable) icon);
                    return icon;
                }

                // Do we use a custom theme?
                if (iconPack != null) {
                    icon = iconPack.getIcon(this.context, componentName);
                } else {
                    icon = packageManager.getActivityIcon(componentName);
                }

                if (icon instanceof BitmapDrawable) {
                    // If the icon is a BitmapDrawable, then we can diskCache it!
                    diskCache.put(getCacheKey(componentName), (BitmapDrawable) icon);
                    memoryCache.put(getCacheKey(componentName), (BitmapDrawable) icon);
                }

                return icon;

            } catch (PackageManager.NameNotFoundException e) {
                Log.e("tmp", "Unable to find component " + componentName.toString() + e);
                return null;
            }
        }

        protected void onPostExecute(Drawable result) {
            if (target != null) {
                if (target.getTag(R.id.tag_icon_worker) == null || target.getTag(R.id.tag_icon_worker).equals(getCacheKey(componentName))) {
                    target.setImageDrawable(result);
                    Animation myFadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                    myFadeInAnimation.setDuration(150);
                    target.startAnimation(myFadeInAnimation);
                }
            }
        }
    }

}


